package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Block;
import val.Blockchain;
import val.ValException;
import val.services.GlobalParameterService;
import val.services.TimeService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.*;

public final class GetGlobalParameter extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final TimeService timeService;
    private final GlobalParameterService globalParameterService;

    public GetGlobalParameter(TimeService timeService, Blockchain blockchain, GlobalParameterService globalParameterService) {
        super(new APITag[]{APITag.MESSAGES});
        this.timeService = timeService;
        this.blockchain = blockchain;
        this.globalParameterService = globalParameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        JsonObject response = new JsonObject();
        response.addProperty(GLOBAL_PLEDGE_RANGE_MIN_RESPONSE, globalParameterService.getPledgeRangeMinCache());
        response.addProperty(GLOBAL_PLEDGE_RANGE_MAX_RESPONSE, globalParameterService.getPledgeRangeMaxCache());
        response.addProperty(GLOBAL_MAX_PLEDGE_REWARD_RESPONSE, globalParameterService.getMaxPledgeRewardCache());
        response.addProperty(GLOBAL_POOL_MAX_CAPICITY_RESPONSE, globalParameterService.getPoolMaxCapicityCache());
        response.addProperty(GLOBAL_GEN_BLOCK_RATIO_RESPONSE, globalParameterService.getGenBlockRatioCache());
        response.addProperty(GLOBAL_POOL_REWARD_PERCENT_RESPONSE, globalParameterService.getPoolRewardPercentCache());
        response.addProperty(GLOBAL_MINER_REWARD_PERCENT_RESPONSE, globalParameterService.getMinerRewardPercentCache());
        response.addProperty(GLOBAL_POOL_COUNT_RESPONSE, globalParameterService.getPoolCountCache());
        response.addProperty(GLOBAL_POOLER_ADDRESS_LIST_RESPONSE, globalParameterService.getPoolerAddressListCache());
        response.addProperty(TIME_RESPONSE, timeService.getEpochTime());
        Block lastBlock = blockchain.getLastBlock();
        response.addProperty("lastBlock", lastBlock.getStringId());
        return response;
    }

}
