package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Blockchain;
import val.BlockchainProcessor;
import val.ValException;
import val.util.JSON;

public final class ProcessBlock extends PeerServlet.PeerRequestHandler {
    private static final JsonElement ACCEPTED;
    private static final JsonElement NOT_ACCEPTED;

    static {
        JsonObject response = new JsonObject();
        response.addProperty("accepted", true);
        ACCEPTED = response;
    }

    static {
        JsonObject response = new JsonObject();
        response.addProperty("accepted", false);
        NOT_ACCEPTED = response;
    }

    private final Logger logger = LoggerFactory.getLogger(ProcessBlock.class);
    private final Blockchain blockchain;
    private final BlockchainProcessor blockchainProcessor;

    public ProcessBlock(Blockchain blockchain, BlockchainProcessor blockchainProcessor) {
        this.blockchain = blockchain;
        this.blockchainProcessor = blockchainProcessor;
    }

    @Override
    public JsonElement processRequest(JsonObject request, Peer peer) {

        try {

            if (!blockchain.getLastBlock().getStringId().equals(JSON.getAsString(request.get("previousBlock")))) {
                // do this check first to avoid validation failures of future blocks and transactions
                // when loading blockchain from scratch
                logger.info("processBlock: local chain block:[{}->{}], peer:[{}], block:[{}]", blockchain.getLastBlock().getHeight(), blockchain.getLastBlock().getStringId(), peer.getPeerAddress(), request.toString());
                return NOT_ACCEPTED;
            }
            blockchainProcessor.processPeerBlock(request, peer);
            return ACCEPTED;

        } catch (ValException | RuntimeException e) {
            if (peer != null) {
                peer.blacklist(e, "received invalid data via requestType=processBlock");
            }
            e.printStackTrace();
            return NOT_ACCEPTED;
        }

    }

}
