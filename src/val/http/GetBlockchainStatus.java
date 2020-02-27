package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Block;
import val.Blockchain;
import val.BlockchainProcessor;
import val.BlockchainProcessor.BlockOutOfOrderException;
import val.ValNetwork;
import val.ValException.ValidationException;
import val.peer.Peer;
import val.services.BlockService;
import val.services.TimeService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.TIME_RESPONSE;

final class GetBlockchainStatus extends APIServlet.APIRequestHandler {

    private final BlockchainProcessor blockchainProcessor;
    private final Blockchain blockchain;
    private final TimeService timeService;
    private final BlockService blockService;

    GetBlockchainStatus(BlockchainProcessor blockchainProcessor, Blockchain blockchain, TimeService timeService, BlockService blockService) {
        super(new APITag[]{APITag.BLOCKS, APITag.INFO});
        this.blockchainProcessor = blockchainProcessor;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.blockService = blockService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        JsonObject response = new JsonObject();
        response.addProperty("application", ValNetwork.APPLICATION);
        response.addProperty("version", ValNetwork.VERSION.toString());
        response.addProperty(TIME_RESPONSE, timeService.getEpochTime());
        Block lastBlock = blockchain.getLastBlock();
        response.addProperty("lastBlock", lastBlock.getStringId());
        response.addProperty("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        String nextCumulative = lastBlock.getCumulativeDifficulty().toString();
        try {
            nextCumulative = blockService.getNextCumulativeDifficulty(lastBlock);
        } catch (BlockOutOfOrderException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        response.addProperty("nextCumulativeDifficulty", nextCumulative);
        response.addProperty("transactionRate", blockchain.getTransactionRate());
        response.addProperty("middlePayload", blockchain.getMiddlePayload());
        response.addProperty("numberOfBlocks", lastBlock.getHeight() + 1);
        Peer lastBlockchainFeeder = blockchainProcessor.getLastBlockchainFeeder();
        response.addProperty("lastBlockchainFeeder", lastBlockchainFeeder == null ? null : lastBlockchainFeeder.getAnnouncedAddress());
        response.addProperty("lastBlockchainFeederHeight", blockchainProcessor.getLastBlockchainFeederHeight());
        response.addProperty("isScanning", blockchainProcessor.isScanning());
        return response;
    }

}
