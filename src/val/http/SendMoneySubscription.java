package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.*;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static val.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

final class SendMoneySubscription extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;

    public SendMoneySubscription(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, AMOUNT_NQT_PARAMETER, FREQUENCY_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        Account sender = parameterService.getSenderAccount(req);
        Long recipient = ParameterParser.getRecipientId(req);
        long amountNQT = ParameterParser.getAmountNQT(req);

        int frequency;
        try {
            frequency = Integer.parseInt(req.getParameter(FREQUENCY_PARAMETER));
        } catch (Exception e) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 4);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid or missing frequency parameter");
            return response;
        }

        if (frequency < Constants.CHAIN_SUBSCRIPTION_MIN_FREQ ||
                frequency > Constants.CHAIN_SUBSCRIPTION_MAX_FREQ) {
            JsonObject response = new JsonObject();
            response.addProperty(ERROR_CODE_RESPONSE, 4);
            response.addProperty(ERROR_DESCRIPTION_RESPONSE, "Invalid frequency amount");
            return response;
        }

        Attachment.AdvancedPaymentSubscriptionSubscribe attachment = new Attachment.AdvancedPaymentSubscriptionSubscribe(frequency, blockchain.getHeight());

        return createTransaction(req, sender, recipient, amountNQT, attachment);
    }
}
