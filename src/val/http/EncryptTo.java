package val.http;

import com.google.gson.JsonElement;
import val.Account;
import val.ValException;
import val.crypto.EncryptedData;
import val.services.AccountService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_RECIPIENT;
import static val.http.common.Parameters.*;

final class EncryptTo extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;

    EncryptTo(ParameterService parameterService, AccountService accountService) {
        super(new APITag[]{APITag.MESSAGES}, RECIPIENT_PARAMETER, MESSAGE_TO_ENCRYPT_PARAMETER, MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER, SECRET_PHRASE_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        long recipientId = ParameterParser.getRecipientId(req);
        Account recipientAccount = accountService.getAccount(recipientId);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            return INCORRECT_RECIPIENT;
        }

        EncryptedData encryptedData = parameterService.getEncryptedMessage(req, recipientAccount, recipientAccount.getPublicKey());
        return JSONData.encryptedData(encryptedData);

    }

}
