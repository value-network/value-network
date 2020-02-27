package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.AliasService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static val.http.common.Parameters.*;

public final class BuyAlias extends CreateTransaction {

    private final ParameterService parameterService;
    private final AliasService aliasService;
    private final Blockchain blockchain;

    public BuyAlias(ParameterService parameterService, Blockchain blockchain, AliasService aliasService, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.ALIASES, APITag.CREATE_TRANSACTION}, apiTransactionManager, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER, AMOUNT_NQT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.aliasService = aliasService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        Account buyer = parameterService.getSenderAccount(req);
        Alias alias = parameterService.getAlias(req);
        long amountNQT = ParameterParser.getAmountNQT(req);

        if (aliasService.getOffer(alias) == null) {
            return INCORRECT_ALIAS_NOTFORSALE;
        }

        long sellerId = alias.getAccountId();
        Attachment attachment = new Attachment.MessagingAliasBuy(alias.getAliasName(), blockchain.getHeight());
        return createTransaction(req, buyer, sellerId, amountNQT, attachment);
    }
}
