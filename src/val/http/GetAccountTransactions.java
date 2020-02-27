package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Blockchain;
import val.Transaction;
import val.ValException;
import val.db.DbIterator;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.TRANSACTIONS_RESPONSE;

final class GetAccountTransactions extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final Blockchain blockchain;

    GetAccountTransactions(ParameterService parameterService, Blockchain blockchain) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER, TIMESTAMP_PARAMETER, TYPE_PARAMETER, SUBTYPE_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, NUMBER_OF_CONFIRMATIONS_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        Account account = parameterService.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = parameterService.getNumberOfConfirmations(req);

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter(TYPE_PARAMETER));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter(SUBTYPE_PARAMETER));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        if (lastIndex < firstIndex) {
            throw new IllegalArgumentException("lastIndex must be greater or equal to firstIndex");
        }

        JsonArray transactions = new JsonArray();
        try (DbIterator<? extends Transaction> iterator = blockchain.getTransactions(account, numberOfConfirmations, type, subtype, timestamp,
                firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                transactions.add(JSONData.transaction(transaction, blockchain.getHeight()));
            }
        }

        JsonObject response = new JsonObject();
        response.add(TRANSACTIONS_RESPONSE, transactions);
        return response;

    }

}
