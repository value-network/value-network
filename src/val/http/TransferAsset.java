package val.http;

import com.google.gson.JsonElement;
import val.*;
import val.services.AccountService;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.NOT_ENOUGH_ASSETS;
import static val.http.common.Parameters.*;

public final class TransferAsset extends CreateTransaction {

    private final ParameterService parameterService;
    private final Blockchain blockchain;
    private final AccountService accountService;

    public TransferAsset(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager, AccountService accountService) {
        super(new APITag[]{APITag.AE, APITag.CREATE_TRANSACTION}, apiTransactionManager, RECIPIENT_PARAMETER, ASSET_PARAMETER, QUANTITY_QNT_PARAMETER);
        this.parameterService = parameterService;
        this.blockchain = blockchain;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        long recipient = ParameterParser.getRecipientId(req);

        Asset asset = parameterService.getAsset(req);
        long quantityQNT = ParameterParser.getQuantityQNT(req);
        Account account = parameterService.getSenderAccount(req);

        long assetBalance = accountService.getUnconfirmedAssetBalanceQNT(account, asset.getId());
        if (assetBalance < 0 || quantityQNT > assetBalance) {
            return NOT_ENOUGH_ASSETS;
        }

        Attachment attachment = new Attachment.ColoredCoinsAssetTransfer(asset.getId(), quantityQNT, blockchain.getHeight());
        return createTransaction(req, account, recipient, 0, attachment);

    }

}
