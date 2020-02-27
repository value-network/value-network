package val.peer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Block;
import val.Blockchain;

final class GetCumulativeDifficulty extends PeerServlet.PeerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetCumulativeDifficulty.class);

    private final Blockchain blockchain;

    GetCumulativeDifficulty(Blockchain blockchain) {
        this.blockchain = blockchain;
    }


    @Override
    JsonElement processRequest(JsonObject request, Peer peer) {
        JsonObject response = new JsonObject();

        Block lastBlock = blockchain.getLastBlock();
        // logger.info("GetCumulativeDifficulty called by peer:[{}-->{}], current node lastBlock:[{}-->{}]", peer.getAnnouncedAddress(), peer.getPeerAddress(), lastBlock.getHeight(), lastBlock.getCumulativeDifficulty());
        response.addProperty("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
        response.addProperty("blockchainHeight", lastBlock.getHeight());
        return response;
    }

}
