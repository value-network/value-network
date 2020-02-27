package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Block;
import val.Blockchain;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.BlockService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;

final class GetBlocks extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final BlockService blockService;

    GetBlocks(Blockchain blockchain, BlockService blockService) {
        super(new APITag[]{APITag.BLOCKS}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_TRANSACTIONS_PARAMETER);
        this.blockchain = blockchain;
        this.blockService = blockService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        if (lastIndex < 0 || lastIndex - firstIndex > 99) {
            lastIndex = firstIndex + 99;
        }

        boolean includeTransactions = Parameters.isTrue(req.getParameter(Parameters.INCLUDE_TRANSACTIONS_PARAMETER));

        JsonArray blocks = new JsonArray();
        try (DbIterator<? extends Block> iterator = blockchain.getBlocks(firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Block block = iterator.next();
                blocks.add(JSONData.block(block, includeTransactions, blockchain.getHeight(), block.getForgeReward(), blockService.getScoopNum(block)));
            }
        }

        JsonObject response = new JsonObject();
        response.add("blocks", blocks);

        return response;
    }

}
