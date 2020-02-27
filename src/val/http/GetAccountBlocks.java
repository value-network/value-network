package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Block;
import val.Blockchain;
import val.ValException;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.BlockService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.BLOCKS_RESPONSE;

public final class GetAccountBlocks extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final ParameterService parameterService;
    private final BlockService blockService;

    GetAccountBlocks(Blockchain blockchain, ParameterService parameterService, BlockService blockService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, TIMESTAMP_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_TRANSACTIONS_PARAMETER);
        this.blockchain = blockchain;
        this.parameterService = parameterService;
        this.blockService = blockService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        Account account = parameterService.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        boolean includeTransactions = Parameters.isTrue(req.getParameter(INCLUDE_TRANSACTIONS_PARAMETER));

        JsonArray blocks = new JsonArray();
        try (DbIterator<? extends Block> iterator = blockchain.getBlocks(account, timestamp, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Block block = iterator.next();
                blocks.add(JSONData.block(block, includeTransactions, blockchain.getHeight(), block.getForgeReward(), blockService.getScoopNum(block)));
            }
        }

        JsonObject response = new JsonObject();
        response.add(BLOCKS_RESPONSE, blocks);

        return response;
    }

}
