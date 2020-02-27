package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Escrow;
import val.ValException;
import val.services.EscrowService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.Parameters.ESCROWS_RESPONSE;

public final class GetAccountEscrowTransactions extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;

    private final EscrowService escrowService;

    GetAccountEscrowTransactions(ParameterService parameterService, EscrowService escrowService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
        this.escrowService = escrowService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        final Account account = parameterService.getAccount(req);

        Collection<Escrow> accountEscrows = escrowService.getEscrowTransactionsByParticipant(account.getId());

        JsonObject response = new JsonObject();

        JsonArray escrows = new JsonArray();

        for (Escrow escrow : accountEscrows) {
            escrows.add(JSONData.escrowTransaction(escrow));
        }

        response.add(ESCROWS_RESPONSE, escrows);
        return response;
    }
}
