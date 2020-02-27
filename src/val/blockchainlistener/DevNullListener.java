package val.blockchainlistener;

import val.Block;
import val.services.AccountService;
import val.services.DGSGoodsStoreService;
import val.util.Listener;

public class DevNullListener implements Listener<Block> {

    private final AccountService accountService;
    private final DGSGoodsStoreService goodsService;

    public DevNullListener(AccountService accountService, DGSGoodsStoreService goodsService) {
        this.accountService = accountService;
        this.goodsService = goodsService;
    }

    @Override
    public void notify(Block block) {
        // try (BurstIterator<DigitalGoodsStore.Purchase> purchases = goodsService.getExpiredPendingPurchases(block.getTimestamp())) {
        //   while (purchases.hasNext()) {
        //     DigitalGoodsStore.Purchase purchase = purchases.next();
        //     Account buyer = accountService.getAccount(purchase.getBuyerId());
        //     accountService.addToUnconfirmedBalanceNQT(buyer, Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT()));
        //     goodsService.changeQuantity(purchase.getGoodsId(), purchase.getQuantity(), true);
        //     goodsService.setPending(purchase, false);
        //   }
        // }
    }
}
