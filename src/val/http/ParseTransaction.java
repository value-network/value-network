package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Transaction;
import val.ValException;
import val.services.ParameterService;
import val.services.TransactionService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.TRANSACTION_BYTES_PARAMETER;
import static val.http.common.Parameters.TRANSACTION_JSON_PARAMETER;
import static val.http.common.ResultFields.*;

final class ParseTransaction extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ParseTransaction.class);

    private final ParameterService parameterService;
    private final TransactionService transactionService;

    ParseTransaction(ParameterService parameterService, TransactionService transactionService) {
        super(new APITag[]{APITag.TRANSACTIONS}, TRANSACTION_BYTES_PARAMETER, TRANSACTION_JSON_PARAMETER);
        this.parameterService = parameterService;
        this.transactionService = transactionService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String transactionBytes = Convert.emptyToNull(req.getParameter(TRANSACTION_BYTES_PARAMETER));
        String transactionJSON = Convert.emptyToNull(req.getParameter(TRANSACTION_JSON_PARAMETER));
        Transaction transaction = parameterService.parseTransaction(transactionBytes, transactionJSON);
        JsonObject response = JSONData.unconfirmedTransaction(transaction);
        try {
            transactionService.validate(transaction);
        } catch (ValException.ValidationException | RuntimeException e) {
            logger.debug(e.getMessage(), e);
            response.addProperty(VALIDATE_RESPONSE, false);
            response.addProperty(ERROR_CODE_RESPONSE, 4);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid transaction: " + e.toString());
            response.addProperty(ERROR_RESPONSE, e.getMessage());
        }
        response.addProperty(VERIFY_RESPONSE, transaction.verifySignature() && transactionService.verifyPublicKey(transaction));
        return response;
    }

}
