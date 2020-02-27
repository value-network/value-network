package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.crypto.Crypto;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;

import static val.http.common.Parameters.*;

public final class CalculateFullHash extends APIServlet.APIRequestHandler {

    public CalculateFullHash() {
        super(new APITag[]{APITag.TRANSACTIONS}, UNSIGNED_TRANSACTION_BYTES_PARAMETER, SIGNATURE_HASH_PARAMETER);
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter(UNSIGNED_TRANSACTION_BYTES_PARAMETER));
        String signatureHashString = Convert.emptyToNull(req.getParameter(SIGNATURE_HASH_PARAMETER));

        if (unsignedBytesString == null) {
            return JSONResponses.MISSING_UNSIGNED_BYTES;
        } else if (signatureHashString == null) {
            return JSONResponses.MISSING_SIGNATURE_HASH;
        }

        MessageDigest digest = Crypto.sha256();
        digest.update(Convert.parseHexString(unsignedBytesString));
        byte[] fullHash = digest.digest(Convert.parseHexString(signatureHashString));
        JsonObject response = new JsonObject();
        response.addProperty(FULL_HASH_RESPONSE, Convert.toHexString(fullHash));

        return response;

    }

}
