package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
import static val.http.JSONResponses.INCORRECT_ACCOUNT_NAME_LENGTH;
import static val.http.common.Parameters.DESCRIPTION_PARAMETER;
import static val.http.common.Parameters.NAME_PARAMETER;

final class SetAccountInfo extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;

    public SetAccountInfo(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, apiTransactionManager, NAME_PARAMETER, DESCRIPTION_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String name = Convert.nullToEmpty(req.getParameter(NAME_PARAMETER)).trim();
        String description = Convert.nullToEmpty(req.getParameter(DESCRIPTION_PARAMETER)).trim();

        if (name.length() > Constants.MAX_ACCOUNT_NAME_LENGTH) {
            return INCORRECT_ACCOUNT_NAME_LENGTH;
        }

        if (description.length() > Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH) {
            return INCORRECT_ACCOUNT_DESCRIPTION_LENGTH;
        }

        Account account = parameterService.getSenderAccount(req);
        Attachment attachment = new Attachment.MessagingAccountInfo(name, description, blockchain.getHeight());
        return createTransaction(req, account, attachment);

    }

}
