package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import val.Block;
import val.Blockchain;
import val.ValException;
import val.db.DbIterator;
import val.http.common.Parameters;
import val.services.BlockService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;

final class GetBlockList extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;
    private final BlockService blockService;

    GetBlockList(Blockchain blockchain, BlockService blockService) {
        super(new APITag[]{APITag.BLOCKS}, PAGE_INDEX_PARAMETER, PAGE_SIZE_PARAMETER, HEIGHT_PARAMETER,
                BLOCK_PARAMETER, ACCOUNT_PARAMETER, POOL_PARAMETER);
        this.blockchain = blockchain;
        this.blockService = blockService;
    }

    public static void main(String[] argv) {
        long blockID = 0;
        // String blockIDString = "-2427812242471857659";
        String blockIDString = "16018931831237693957";
        if (blockIDString != null && !blockIDString.startsWith("-")) {
            blockID = Convert.parseUnsignedLong(blockIDString);
        } else {
            blockID = Long.parseLong(blockIDString);
        }
        System.out.print(blockID);
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        int page = ParameterParser.getPage(req);
        int limit = ParameterParser.getLimit(req);
        long blockID = 0;
        String strHeight = Convert.emptyToNull(req.getParameter(Parameters.HEIGHT_PARAMETER));
        int height = -1;
        if (null != strHeight) {
            height = Convert.parseInt(strHeight);
        }

        String blockIDString = Convert.emptyToNull(req.getParameter(BLOCK_PARAMETER));
        long account = ParameterParser.getAccountIdByDefault(req.getParameter(ACCOUNT_PARAMETER));
        long pool = ParameterParser.getAccountIdByDefault(req.getParameter(POOL_PARAMETER));

        if (blockIDString != null) {
            if (blockIDString.startsWith("-")) {
                try {
                    blockID = Long.parseLong(blockIDString);
                } catch (Exception e) {
                    e.printStackTrace();
                    blockID = 0;
                }
            } else {
                blockID = Convert.parseUnsignedLong(blockIDString);
            }
        }
        // System.out.printf("input block:%s, converd block:%s\n",
        // blockIDString,blockID);
        JsonArray blocks = new JsonArray();
        try (DbIterator<? extends Block> iterator = blockchain.getBlockLists(page, limit, height, blockID, account,
                pool)) {
            while (iterator.hasNext()) {
                Block block = iterator.next();
                blocks.add(JSONData.blockList(block, blockchain.getHeight(), block.getForgeReward(),
                        blockService.getScoopNum(block)));// blockService.getBlockReward(block),
            }
        }
        int total = blockchain.getBlockCount(height, blockID, account, pool);

        return JSONData.listResponse(0, "OK", total, blocks);
    }

}
