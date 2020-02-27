package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Blockchain;
import val.ValException;
import val.services.AccountService;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.ResultFields.REWARD_RECIPIENT_RESPONSE;

final class GetRewardRecipient extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final Blockchain blockchain;
    private final AccountService accountService;

    GetRewardRecipient(ParameterService parameterService, Blockchain blockchain, AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        JsonObject response = new JsonObject();

        final Account account = parameterService.getAccount(req);
        Account.RewardRecipientAssignment assignment = accountService.getRewardRecipientAssignment(account);
        long height = blockchain.getLastBlock().getHeight();
        if (assignment == null) {
            response.addProperty(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(account.getId()));
        } else if (assignment.getFromHeight() > height + 1) {
            response.addProperty(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(assignment.getPrevRecipientId()));
        } else {
            response.addProperty(REWARD_RECIPIENT_RESPONSE, Convert.toUnsignedLong(assignment.getRecipientId()));
        }

        return response;
    }

}
