package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.MinePool;
import val.ValException;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.*;

final class GetGlobalParameterStoreCache extends APIServlet.APIRequestHandler {

    GetGlobalParameterStoreCache() {
        super(new APITag[]{APITag.DEBUG});
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        JsonObject response = new JsonObject();
        response.addProperty(GLOBAL_PLEDGE_RANGE_MIN_RESPONSE, MinePool.getInstance().getMinPledgeCache());
        response.addProperty(GLOBAL_PLEDGE_RANGE_MAX_RESPONSE, MinePool.getInstance().getMaxPledgeCache());
        response.addProperty(GLOBAL_MAX_PLEDGE_REWARD_RESPONSE, MinePool.getInstance().getMaxPledgeRewardCache());
        response.addProperty(GLOBAL_POOL_MAX_CAPICITY_RESPONSE, MinePool.getInstance().getPoolMaxCapCache());
        // response.addProperty(GLOBAL_GEN_BLOCK_RATIO_RESPONSE, MinePool.getInstance().getGenBlockRetioCache());
        response.addProperty(GLOBAL_POOL_REWARD_PERCENT_RESPONSE, MinePool.getInstance().getPoolRewardPercentCache());
        response.addProperty(GLOBAL_MINER_REWARD_PERCENT_RESPONSE, MinePool.getInstance().getMinerRewardPercentCache());
        response.addProperty(GLOBAL_POOL_COUNT_RESPONSE, MinePool.getInstance().getPoolCountCache());
        response.addProperty(GLOBAL_IS_POOL_RESPONSE, MinePool.getInstance().isPoolNode());

        return response;
    }

}
