package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Blockchain;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_HEIGHT;
import static val.http.JSONResponses.MISSING_HEIGHT;
import static val.http.common.Parameters.HEIGHT_PARAMETER;

final class GetBlockId extends APIServlet.APIRequestHandler {

    private final Blockchain blockchain;

    GetBlockId(Blockchain blockchain) {
        super(new APITag[]{APITag.BLOCKS}, HEIGHT_PARAMETER);
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        int height;
        try {
            String heightValue = Convert.emptyToNull(req.getParameter(HEIGHT_PARAMETER));
            if (heightValue == null) {
                return MISSING_HEIGHT;
            }
            height = Integer.parseInt(heightValue);
        } catch (RuntimeException e) {
            return INCORRECT_HEIGHT;
        }

        try {
            JsonObject response = new JsonObject();
            response.addProperty("block", Convert.toUnsignedLong(blockchain.getBlockIdAtHeight(height)));
            return response;
        } catch (RuntimeException e) {
            return INCORRECT_HEIGHT;
        }

    }

}
