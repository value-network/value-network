package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.AccountService;
import val.services.ParameterService;
import val.services.TimeService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_AMOUNT;
import static val.http.JSONResponses.MISSING_AMOUNT;
import static val.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static val.http.common.Parameters.RECIPIENT_PARAMETER;

public final class SetPledges extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;
    private final AccountService accountService;
    private final TimeService timeService;

    public SetPledges(ParameterService parameterService, Blockchain blockchain, AccountService accountService, TimeService timeService,
                      APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, apiTransactionManager, AMOUNT_NQT_PARAMETER, RECIPIENT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.timeService = timeService;
        this.accountService = accountService;
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
        long recipient = ParameterParser.getRecipientId(req);
        //String recipintID = Convert.toUnsignedLong(recipient);
//		System.out.printf("recipient:%s, recipientID:%s", recipient,recipintID);

        // verify the mine pool list include the input recipientid
//        if (!MinePool.getInstance().verifyMinePooler(recipient)) {
//            JsonObject response = new JsonObject();
//            response.addProperty(ERROR_CODE_RESPONSE, 8);
//            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "the recipient account was not included in the mine pool");
//            return response;
//        }

        Attachment attachment = new Attachment.PledgeAssignment(amountNQT, timeService.getEpochTimeMillis(), blockchain.getHeight());

        return createTransaction(req, account, recipient, amountNQT, attachment);
    }

}
