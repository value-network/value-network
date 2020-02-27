package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Attachment;
import val.Blockchain;
import val.ValException;
import val.services.AccountService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.RECIPIENT_PARAMETER;
import static val.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static val.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class SetRewardRecipient extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;
    private final AccountService accountService;

    public SetRewardRecipient(ParameterService parameterService, Blockchain blockchain, AccountService accountService, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        final Account account = parameterService.getSenderAccount(req);
        Long recipient = ParameterParser.getRecipientId(req);
        Account recipientAccount = accountService.getAccount(recipient);
        if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 8);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "recipient account does not have public key");
            return response;
        }
        Attachment attachment = new Attachment.ChainMiningRewardRecipientAssignment(blockchain.getHeight());
        return createTransaction(req, account, recipient, 0, attachment);
    }

}
