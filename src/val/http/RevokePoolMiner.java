package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.MinePool;
import val.ValException;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;
import val.util.JSON;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static val.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class RevokePoolMiner extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final TimeService timeService;

    public RevokePoolMiner(ParameterService parameterService, AccountService accountService, TimeService timeService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, POOL_PARAMETER, PUBLIC_KEY_PARAMETER, SIGNATURE_PARAMETER);
        this.parameterService = parameterService;
        this.timeService = timeService;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        final Account poolAccount = parameterService.getSenderAccount(req);
        if (MinePool.getInstance().getPoolAccountID() != poolAccount.getId()) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 9);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the operation must be in his own volume node!");
            return response;
        }
        long poolId = ParameterParser.getPoolId(req);

        if (poolAccount.getId() != poolId) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 9);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the sender account does not matched the pool account");
            return response;
        }

        if (!MinePool.getInstance().verifyMinePooler(poolId)) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 8);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the pool account was not included in the mine pool");
            return response;
        }

        boolean verify = parameterService.verifyPoolMinerSignature(req);
        if (!verify) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 9);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "verify signature err.");
            return response;
        }

        String minerValue = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
        JsonArray minerArr = JSON.getAsJsonArray(JSON.parse(minerValue));
        JsonArray data = new JsonArray();
        String errMsg = "";
        for (int i = 0; i < minerArr.size(); i++) {
            JsonObject minerObject = JSON.getAsJsonObject(minerArr.get(i));
            String miner = JSON.getAsString(minerObject.get("miner"));
            //System.out.println(miner);
            long minerId = Convert.parseAccountId(miner);
            Account.PoolMiner poolMiner = accountService.getGrantPoolMiner(minerId, poolId);
            if (poolMiner == null) {
//				JsonObject response = new JsonObject();
//				response.addProperty(ERROR_CODE_RESPONSE, 9);
//				response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the miner account [" + minerId +"] has not been grant into the mine pool ["+poolId+"]");
                errMsg = "the miner account [" + minerId + "] has not been grant into the mine pool [" + poolId + "]";
                System.out.println(errMsg);

                continue;
            }
            try {
                accountService.UpdatePoolMiner(minerId, poolId, 1, timeService.getEpochTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
//				JsonObject resp = new JsonObject();
//				resp.addProperty(ERROR_CODE_RESPONSE, 9);
//				resp.addProperty(ERROR_DESCRIPTION_RESPONSE, "revoke mine pool err: "+ e.getMessage());
                continue;
            }
            data.add(JSONData.addMiner(minerId, poolId, timeService.getEpochTimeMillis()));
        }
        if (data.size() == 0) {
            return JSONData.listResponse(1, errMsg, minerArr.size(), data);
        }
        return JSONData.listResponse(0, "OK", minerArr.size(), data);
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
