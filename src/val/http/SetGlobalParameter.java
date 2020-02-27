package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import val.*;
import val.services.ParameterService;
import val.util.Convert;
import val.util.JSON;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.*;

public final class SetGlobalParameter extends CreateTransaction {

    private final Blockchain blockchain;
    private final ParameterService parameterService;

    public SetGlobalParameter(ParameterService parameterService, Blockchain blockchain,
                              APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.MESSAGES, APITag.CREATE_TRANSACTION}, apiTransactionManager, GLOBAL_PLEDGE_RANGE_MIN_RESPONSE, GLOBAL_PLEDGE_RANGE_MAX_RESPONSE,
                GLOBAL_MAX_PLEDGE_REWARD_RESPONSE, GLOBAL_POOL_MAX_CAPICITY_RESPONSE, GLOBAL_GEN_BLOCK_RATIO_RESPONSE,
                GLOBAL_POOL_REWARD_PERCENT_RESPONSE, GLOBAL_MINER_REWARD_PERCENT_RESPONSE, GLOBAL_POOL_COUNT_RESPONSE, GLOBAL_POOLER_ADDRESS_LIST_RESPONSE);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        //String amountValueNQT = Convert.emptyToNull(req.getParameter(AMOUNT_NQT_PARAMETER));
        String pledgeRangeMin = Convert.nullToEmpty(req.getParameter(GLOBAL_PLEDGE_RANGE_MIN_RESPONSE));
        String pledgeRangeMax = Convert.nullToEmpty(req.getParameter(GLOBAL_PLEDGE_RANGE_MAX_RESPONSE));
        String maxPledgeReward = Convert.nullToEmpty(req.getParameter(GLOBAL_MAX_PLEDGE_REWARD_RESPONSE));
        String poolMaxCapicity = Convert.nullToEmpty(req.getParameter(GLOBAL_POOL_MAX_CAPICITY_RESPONSE));
        String genBlockRatio = Convert.nullToEmpty(req.getParameter(GLOBAL_GEN_BLOCK_RATIO_RESPONSE));
        String poolRewardPercent = Convert.nullToEmpty(req.getParameter(GLOBAL_POOL_REWARD_PERCENT_RESPONSE));
        String minerRewardPercent = Convert.nullToEmpty(req.getParameter(GLOBAL_MINER_REWARD_PERCENT_RESPONSE));
        String poolCount = Convert.nullToEmpty(req.getParameter(GLOBAL_POOL_COUNT_RESPONSE));
        String poolerAddressList = Convert.nullToEmpty(req.getParameter(GLOBAL_POOLER_ADDRESS_LIST_RESPONSE));

        final Account senderAccount = parameterService.getSenderAccount(req);
        if (senderAccount == null) {
            return JSONResponses.MISSING_ACCOUNT;
        }

        boolean checkPermission = true;
        if (StringUtils.isEmpty(pledgeRangeMin)
                && StringUtils.isEmpty(pledgeRangeMax)
                && StringUtils.isEmpty(maxPledgeReward)
                && StringUtils.isEmpty(poolMaxCapicity)
                && StringUtils.isEmpty(genBlockRatio)
                && StringUtils.isEmpty(poolRewardPercent)
                && StringUtils.isEmpty(minerRewardPercent)
                && !StringUtils.isEmpty(poolCount)
                && !StringUtils.isEmpty(poolerAddressList)) {
            // 除了添加矿池，其他参数都是空的。
            // 说明仅仅是为了添加矿池，就不检查账户权限
            JsonObject poolListData = JSON.getAsJsonObject(JSON.parse(poolerAddressList));
            JsonArray poolListArr = poolListData.getAsJsonArray("addPool");
            JsonArray poolListDelArr = poolListData.getAsJsonArray("delPool");
            if ((poolListArr != null && poolListArr.size() > 0) && (poolListDelArr == null || poolListDelArr.size() == 0)) {
                checkPermission = false;
            }
        }

        if (checkPermission) {
            if (senderAccount.getId() != Account.getId(Convert.parseHexString(Constants.FOUNDATION_PUBLIC_KEY_HEX)) &&
                    senderAccount.getId() != Account.getId(Convert.parseHexString(Constants.MASTER_PUBLIC_KEY_HEX))) {
                JsonObject response = new JsonObject();
                response.addProperty(ERROR_CODE_RESPONSE, 9);
                response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the operation must in the foundation account volume node!");
                return response;
            }
        }

        Attachment attachment = new Attachment.MessagingGlobalPrameter(pledgeRangeMin, pledgeRangeMax, maxPledgeReward,
                poolMaxCapicity, genBlockRatio, poolRewardPercent, minerRewardPercent, poolCount, poolerAddressList, blockchain.getHeight());

        return createTransaction(req, senderAccount, attachment);
    }

}
