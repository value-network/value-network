package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.*;
import static val.http.common.Parameters.AMOUNT_NQT_PARAMETER;

public final class SetUnpledges extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;
    private final AccountService accountService;
    private final TimeService timeService;

    public SetUnpledges(ParameterService parameterService, Blockchain blockchain, AccountService accountService,
                        TimeService timeService, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.CREATE_TRANSACTION}, apiTransactionManager,
                AMOUNT_NQT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.accountService = accountService;
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String amountValueNQT = Convert.emptyToNull(req.getParameter(AMOUNT_NQT_PARAMETER));
        if (amountValueNQT == null) {
            return MISSING_AMOUNT;
        }
        long amountNQT;
        try {
            amountNQT = Long.parseLong(amountValueNQT);
        } catch (RuntimeException e) {
            return INCORRECT_AMOUNT;
        }
        if (amountNQT <= 0 || amountNQT >= Constants.MAX_BALANCE_NQT) {
            //throw new ParameterException(INCORRECT_AMOUNT);
            return INCORRECT_AMOUNT;
        }
        final Account account = parameterService.getSenderAccount(req);
        Account.Pledges pledgeAccount = accountService.getAccountPledge(account.getId());
        if (pledgeAccount == null || amountNQT > pledgeAccount.getPledgeTotal()) {
            return NOT_ENOUGH_PLEDGE;
        }
//		System.out.printf("UNPLEDGE account:%s - recipient - %s\n",account.getId(),recipient.getRecipID());
        long withdrawTime = timeService.getEpochTime() + Constants.WITHDRAW_ALLOW_CYCLE; // withdraw time:anytime
//		System.out.printf("unpledge amount: %s, withdraw time: %s ", amountNQT,withdrawTime);
        Attachment attachment = new Attachment.UnpledgeAssignment(amountNQT, withdrawTime, blockchain.getHeight());
        return createTransaction(req, account, null, amountNQT, attachment);
    }

}
