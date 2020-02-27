package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.MinePool;
import val.ValException;
import val.crypto.Crypto;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;
import val.util.JSON;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static val.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class GrantPoolMiner extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final TimeService timeService;

    public GrantPoolMiner(ParameterService parameterService, AccountService accountService, TimeService timeService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, POOL_PARAMETER, PUBLIC_KEY_PARAMETER, SIGNATURE_PARAMETER);
        this.parameterService = parameterService;
        this.timeService = timeService;
        this.accountService = accountService;
    }

    public static void main(String[] args) {
        String minerValue = "[{\"miner\":\"17831228034117176875\"},{\"miner\":\"5181943973808280388\"}]";
//		  JsonArray miners = new JsonArray();
//		  JsonObject miner1 = new JsonObject();
//		  miner1.addProperty("miner", "1234");
//		  miners.add(miner1);
//		  miner1 = new JsonObject();
//		  miner1.addProperty("miner", "2345");
//		  miners.add(miner1);
//		  minerValue = miners.toString();
        System.out.println(minerValue);

        String publicKey = "ca9861231c11e1104cc144d69fbc3b0742103a44976adb175959e15fac287303";
        publicKey = "52a65d2cd95031013e426ecef94d1352687899acf386e4f5174ee061f3581552";
        String secretPhrase = "stress cheat favorite body ahead single shame sunset fragile bottom wound front";
        secretPhrase = "much hunger sunset second approach further surround crawl weary guard shoe arrive";
        System.out.println(minerValue);
        String signature = Convert.toHexString(Crypto.sign(minerValue.getBytes(), secretPhrase));

//		  System.out.printf("minerValue.getBytes(): %s\n", minerValue.getBytes().toString());
//		  for (int i=0; i<minerValue.getBytes().length;i++){
//			  System.out.printf("minerValue.getBytes(i): %s\n", minerValue.getBytes()[i]);
//		  }
        //publicKey = "1a830d582866612fe2492e98b172b849699ffcad16194e4a061f600b213f121c";

        boolean result = Crypto.verify(Convert.parseHexString(signature), minerValue.getBytes(), Convert.parseHexString(publicKey), false);
        System.out.printf("result: %s\n", result);
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
            Account.PoolMiner poolMiner = accountService.getGrantPoolMiner(minerId, 0);
            if (poolMiner != null) {
//				JsonObject response = new JsonObject();
//				response.addProperty(ERROR_CODE_RESPONSE, 9);
//				response.addProperty(ERROR_DESCRIPTION_RESPONSE,
//						"the miner account [" + minerId + "] has been already grant into other mine pool ["+poolMiner.getPoolID()+"]");
                errMsg = "the miner account [" + minerId + "] has been already grant into other mine pool [" + poolMiner.getPoolID() + "]";
                System.out.println(errMsg);
                continue;
            }
            try {
                accountService.addPoolMiner(minerId, poolId, 0, timeService.getEpochTimeMillis());
            } catch (Exception e) {
                e.printStackTrace();
//				JsonObject resp = new JsonObject();
//				resp.addProperty(ERROR_CODE_RESPONSE, 9);
//				resp.addProperty(ERROR_DESCRIPTION_RESPONSE, "save mine pool err: " + e.getMessage());
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
