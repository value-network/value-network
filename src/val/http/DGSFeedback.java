package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.AccountService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.PURCHASE_PARAMETER;

public final class DGSFeedback extends CreateTransaction {

    private final ParameterService parameterService;
    private final AccountService accountService;
    private final Blockchain blockchain;

    DGSFeedback(ParameterService parameterService, Blockchain blockchain, AccountService accountService, APITransactionManager apiTransactionManager) {
        super(new APITag[]{APITag.DGS, APITag.CREATE_TRANSACTION}, apiTransactionManager, PURCHASE_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
        this.blockchain = blockchain;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        DigitalGoodsStore.Purchase purchase = parameterService.getPurchase(req);
        Account buyerAccount = parameterService.getSenderAccount(req);

        if (buyerAccount.getId() != purchase.getBuyerId()) {
            return JSONResponses.INCORRECT_PURCHASE;
        }
        if (purchase.getEncryptedGoods() == null) {
            return JSONResponses.GOODS_NOT_DELIVERED;
        }

        Account sellerAccount = accountService.getAccount(purchase.getSellerId());
        Attachment attachment = new Attachment.DigitalGoodsFeedback(purchase.getId(), blockchain.getHeight());

        return createTransaction(req, buyerAccount, sellerAccount.getId(), 0, attachment);
    }

}
