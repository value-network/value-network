package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Transaction;
import val.TransactionProcessor;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static val.http.JSONResponses.INCORRECT_ACCOUNT;
import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.ResultFields.UNCONFIRMED_TRANSACTIONS_RESPONSE;

final class GetUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    private final TransactionProcessor transactionProcessor;

    GetUnconfirmedTransactions(TransactionProcessor transactionProcessor) {
        super(new APITag[]{APITag.TRANSACTIONS, APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
        this.transactionProcessor = transactionProcessor;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        final String accountIdString = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));

        long accountId = 0;

        if (accountIdString != null) {
            try {
                accountId = Convert.parseAccountId(accountIdString);
            } catch (RuntimeException e) {
                return INCORRECT_ACCOUNT;
            }
        }

        final List<Transaction> unconfirmedTransactions = transactionProcessor.getAllUnconfirmedTransactions();

        final JsonArray transactions = new JsonArray();

        for (Transaction transaction : unconfirmedTransactions) {
            if (accountId != 0 && !(accountId == transaction.getSenderId() || accountId == transaction.getRecipientId())) {
                continue;
            }
            transactions.add(JSONData.unconfirmedTransaction(transaction));
        }

        final JsonObject response = new JsonObject();

        response.add(UNCONFIRMED_TRANSACTIONS_RESPONSE, transactions);

        return response;
    }

}
