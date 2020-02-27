package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Escrow;
import val.services.EscrowService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ESCROW_PARAMETER;
import static val.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static val.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

final class GetEscrowTransaction extends APIServlet.APIRequestHandler {

    private final EscrowService escrowService;

    GetEscrowTransaction(EscrowService escrowService) {
        super(new APITag[]{APITag.ACCOUNTS}, ESCROW_PARAMETER);
        this.escrowService = escrowService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        long escrowId;
        try {
            escrowId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(ESCROW_PARAMETER)));
        } catch (Exception e) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 3);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified escrow");
            return response;
        }

        Escrow escrow = escrowService.getEscrowTransaction(escrowId);
        if (escrow == null) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 5);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Escrow transaction not found");
            return response;
        }

        return JSONData.escrowTransaction(escrow);
    }
}
