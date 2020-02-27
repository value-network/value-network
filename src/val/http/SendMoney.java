package val.http;

import com.google.gson.JsonElement;
import val.Account;
import val.ValException;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static val.http.common.Parameters.RECIPIENT_PARAMETER;

final class SendMoney extends CreateTransaction {

    private final ParameterService parameterService;

    SendMoney(ParameterService parameterService, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER);
        this.parameterService = parameterService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        long recipient = ParameterParser.getRecipientId(req);
        long amountNQT = ParameterParser.getAmountNQT(req);
        Account account = parameterService.getSenderAccount(req);
        return createTransaction(req, account, recipient, amountNQT);
    }

}
