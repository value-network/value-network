package val.services;

import val.Appendix;
import val.Attachment;
import val.DigitalGoodsStore;
import val.Transaction;
import val.db.DbIterator;
import val.util.Listener;

public interface DGSGoodsStoreService {

    boolean addGoodsListener(Listener<DigitalGoodsStore.Goods> listener, DigitalGoodsStore.Event eventType);

    boolean removeGoodsListener(Listener<DigitalGoodsStore.Goods> listener, DigitalGoodsStore.Event eventType);

    boolean addPurchaseListener(Listener<DigitalGoodsStore.Purchase> listener, DigitalGoodsStore.Event eventType);

    boolean removePurchaseListener(Listener<DigitalGoodsStore.Purchase> listener, DigitalGoodsStore.Event eventType);

    DigitalGoodsStore.Goods getGoods(long goodsId);

    DbIterator<DigitalGoodsStore.Goods> getAllGoods(int from, int to);

    DbIterator<DigitalGoodsStore.Goods> getGoodsInStock(int from, int to);

    DbIterator<DigitalGoodsStore.Goods> getSellerGoods(long sellerId, boolean inStockOnly, int from, int to);

    DbIterator<DigitalGoodsStore.Purchase> getAllPurchases(int from, int to);

    DbIterator<DigitalGoodsStore.Purchase> getSellerPurchases(long sellerId, int from, int to);

    DbIterator<DigitalGoodsStore.Purchase> getBuyerPurchases(long buyerId, int from, int to);

    DbIterator<DigitalGoodsStore.Purchase> getSellerBuyerPurchases(long sellerId, long buyerId, int from, int to);

    DbIterator<DigitalGoodsStore.Purchase> getPendingSellerPurchases(long sellerId, int from, int to);

    DigitalGoodsStore.Purchase getPurchase(long purchaseId);

    void changeQuantity(long goodsId, int deltaQuantity, boolean allowDelisted);

    void purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment);

    void addPurchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId);

    void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment);

    void delistGoods(long goodsId);

    void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message);

    void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage);

    DbIterator<DigitalGoodsStore.Purchase> getExpiredPendingPurchases(int timestamp);

    void changePrice(long goodsId, long priceNQT);

    void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment);

    DigitalGoodsStore.Purchase getPendingPurchase(long purchaseId);

    void setPending(DigitalGoodsStore.Purchase purchase, boolean pendingValue);
}
