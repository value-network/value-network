package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Block;
import val.Blockchain;
import val.ValNetwork;
import val.crypto.hash.Shabal256;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;

final class GetMiningInfo extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;

    GetMiningInfo(Blockchain blockchain) {
        super(new APITag[]{APITag.MINING, APITag.INFO});
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        JsonObject response = new JsonObject();

        response.addProperty("height", Long.toString((long) ValNetwork.getBlockchain().getHeight() + 1));

        Block lastBlock = blockchain.getLastBlock();
        byte[] lastGenSig = lastBlock.getGenerationSignature();
        long lastGenerator = lastBlock.getGeneratorId();

        ByteBuffer buf = ByteBuffer.allocate(32 + 8);
        buf.put(lastGenSig);
        buf.putLong(lastGenerator);

        Shabal256 md = new Shabal256();
        md.update(buf.array());
        byte[] newGenSig = md.digest();

        response.addProperty("generationSignature", Convert.toHexString(newGenSig));
        response.addProperty("baseTarget", Long.toString(lastBlock.getBaseTarget()));

        return response;
    }
}
