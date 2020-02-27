package val.services.impl;

import val.*;
import val.crypto.EncryptedData;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.VersionedValuesTable;
import val.db.store.DigitalGoodsStoreStore;
import val.services.AccountService;
import val.services.DGSGoodsStoreService;
import val.util.Convert;
import val.util.Listener;
import val.util.Listeners;

import java.util.ArrayList;
import java.util.List;

public class DGSGoodsStoreServiceImpl implements DGSGoodsStoreService {

    private final Blockchain blockchain;
    private final DigitalGoodsStoreStore digitalGoodsStoreStore;
    private final AccountService accountService;
    private final VersionedValuesTable<DigitalGoodsStore.Purchase, EncryptedData> feedbackTable;
    private final VersionedValuesTable<DigitalGoodsStore.Purchase, String> publicFeedbackTable;

    private final VersionedEntityTable<DigitalGoodsStore.Goods> goodsTable;
    private final VersionedEntityTable<DigitalGoodsStore.Purchase> purchaseTable;
    private final DbKey.LongKeyFactory<DigitalGoodsStore.Goods> goodsDbKeyFactory;
    private final DbKey.LongKeyFactory<DigitalGoodsStore.Purchase> purchaseDbKeyFactory;

    private final Listeners<DigitalGoodsStore.Goods, DigitalGoodsStore.Event> goodsListeners = new Listeners<>();

    private final Listeners<DigitalGoodsStore.Purchase, DigitalGoodsStore.Event> purchaseListeners = new Listeners<>();

    public DGSGoodsStoreServiceImpl(Blockchain blockchain, DigitalGoodsStoreStore digitalGoodsStoreStore, AccountService accountService) {
        this.blockchain = blockchain;
        this.digitalGoodsStoreStore = digitalGoodsStoreStore;
        this.goodsTable = digitalGoodsStoreStore.getGoodsTable();
        this.purchaseTable = digitalGoodsStoreStore.getPurchaseTable();
        this.goodsDbKeyFactory = digitalGoodsStoreStore.getGoodsDbKeyFactory();
        this.purchaseDbKeyFactory = digitalGoodsStoreStore.getPurchaseDbKeyFactory();
        this.feedbackTable = digitalGoodsStoreStore.getFeedbackTable();
        this.publicFeedbackTable = digitalGoodsStoreStore.getPublicFeedbackTable();

        this.accountService = accountService;
    }

    @Override
    public boolean addGoodsListener(Listener<DigitalGoodsStore.Goods> listener, DigitalGoodsStore.Event eventType) {
        return goodsListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeGoodsListener(Listener<DigitalGoodsStore.Goods> listener, DigitalGoodsStore.Event eventType) {
        return goodsListeners.removeListener(listener, eventType);
    }

    @Override
    public boolean addPurchaseListener(Listener<DigitalGoodsStore.Purchase> listener, DigitalGoodsStore.Event eventType) {
        return purchaseListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removePurchaseListener(Listener<DigitalGoodsStore.Purchase> listener, DigitalGoodsStore.Event eventType) {
        return purchaseListeners.removeListener(listener, eventType);
    }

    @Override
    public DigitalGoodsStore.Goods getGoods(long goodsId) {
        return goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
    }

    @Override
    public DbIterator<DigitalGoodsStore.Goods> getAllGoods(int from, int to) {
        return goodsTable.getAll(from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Goods> getGoodsInStock(int from, int to) {
        return digitalGoodsStoreStore.getGoodsInStock(from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Goods> getSellerGoods(final long sellerId, final boolean inStockOnly, int from, int to) {
        return digitalGoodsStoreStore.getSellerGoods(sellerId, inStockOnly, from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getAllPurchases(int from, int to) {
        return purchaseTable.getAll(from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getSellerPurchases(long sellerId, int from, int to) {
        return digitalGoodsStoreStore.getSellerPurchases(sellerId, from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getBuyerPurchases(long buyerId, int from, int to) {
        return digitalGoodsStoreStore.getBuyerPurchases(buyerId, from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getSellerBuyerPurchases(final long sellerId, final long buyerId, int from, int to) {
        return digitalGoodsStoreStore.getSellerBuyerPurchases(sellerId, buyerId, from, to);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getPendingSellerPurchases(final long sellerId, int from, int to) {
        return digitalGoodsStoreStore.getPendingSellerPurchases(sellerId, from, to);
    }

    @Override
    public DigitalGoodsStore.Purchase getPurchase(long purchaseId) {
        return purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
    }

    @Override
    public void changeQuantity(long goodsId, int deltaQuantity, boolean allowDelisted) {
        DigitalGoodsStore.Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
        if (allowDelisted || !goods.isDelisted()) {
            goods.changeQuantity(deltaQuantity);
            goodsTable.insert(goods);
            goodsListeners.notify(goods, DigitalGoodsStore.Event.GOODS_QUANTITY_CHANGE);
        } else {
            throw new IllegalStateException("Can't change quantity of delisted goods");
        }
    }

    @Override
    public void purchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment) {
        DigitalGoodsStore.Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(attachment.getGoodsId()));
        if (!goods.isDelisted() && attachment.getQuantity() <= goods.getQuantity() && attachment.getPriceNQT() == goods.getPriceNQT()
                && attachment.getDeliveryDeadlineTimestamp() > blockchain.getLastBlock().getTimestamp()) {
            changeQuantity(goods.getId(), -attachment.getQuantity(), false);
            addPurchase(transaction, attachment, goods.getSellerId());
        } else {
            Account buyer = accountService.getAccount(transaction.getSenderId());
            accountService.addToUnconfirmedBalanceNQT(buyer, Convert.safeMultiply(attachment.getQuantity(), attachment.getPriceNQT()));
            // restoring the unconfirmed balance if purchase not successful, however buyer still lost the transaction fees
        }
    }

    @Override
    public void addPurchase(Transaction transaction, Attachment.DigitalGoodsPurchase attachment, long sellerId) {
        DigitalGoodsStore.Purchase purchase = new DigitalGoodsStore.Purchase(transaction, attachment, sellerId);
        purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, DigitalGoodsStore.Event.PURCHASE);
    }

    @Override
    public void listGoods(Transaction transaction, Attachment.DigitalGoodsListing attachment) {
        DbKey dbKey = goodsDbKeyFactory.newKey(transaction.getId());
        DigitalGoodsStore.Goods goods = new DigitalGoodsStore.Goods(dbKey, transaction, attachment);
        goodsTable.insert(goods);
        goodsListeners.notify(goods, DigitalGoodsStore.Event.GOODS_LISTED);
    }

    @Override
    public void delistGoods(long goodsId) {
        DigitalGoodsStore.Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
        if (!goods.isDelisted()) {
            goods.setDelisted(true);
            goodsTable.insert(goods);
            goodsListeners.notify(goods, DigitalGoodsStore.Event.GOODS_DELISTED);
        } else {
            throw new IllegalStateException("Goods already delisted");
        }
    }

    @Override
    public void feedback(long purchaseId, Appendix.EncryptedMessage encryptedMessage, Appendix.Message message) {
        DigitalGoodsStore.Purchase purchase = purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
        if (encryptedMessage != null) {
            purchase.addFeedbackNote(encryptedMessage.getEncryptedData());
            purchaseTable.insert(purchase);
            feedbackTable.insert(purchase, purchase.getFeedbackNotes());
        }
        if (message != null) {
            addPublicFeedback(purchase, Convert.toString(message.getMessage()));
        }
        purchaseListeners.notify(purchase, DigitalGoodsStore.Event.FEEDBACK);
    }

    private void addPublicFeedback(DigitalGoodsStore.Purchase purchase, String publicFeedback) {
        List<String> publicFeedbacks = purchase.getPublicFeedbacks();
        if (publicFeedbacks == null) {
            publicFeedbacks = new ArrayList<>();
        }
        publicFeedbacks.add(publicFeedback);
        purchase.setHasPublicFeedbacks(true);
        purchaseTable.insert(purchase);
        publicFeedbackTable.insert(purchase, publicFeedbacks);
    }

    @Override
    public void refund(long sellerId, long purchaseId, long refundNQT, Appendix.EncryptedMessage encryptedMessage) {
        DigitalGoodsStore.Purchase purchase = purchaseTable.get(purchaseDbKeyFactory.newKey(purchaseId));
        Account seller = accountService.getAccount(sellerId);
        accountService.addToBalanceNQT(seller, -refundNQT);
        Account buyer = accountService.getAccount(purchase.getBuyerId());
        accountService.addToBalanceAndUnconfirmedBalanceNQT(buyer, refundNQT);
        if (encryptedMessage != null) {
            purchase.setRefundNote(encryptedMessage.getEncryptedData());
            purchaseTable.insert(purchase);
        }
        purchase.setRefundNQT(refundNQT);
        purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, DigitalGoodsStore.Event.REFUND);
    }

    @Override
    public DbIterator<DigitalGoodsStore.Purchase> getExpiredPendingPurchases(final int timestamp) {
        return digitalGoodsStoreStore.getExpiredPendingPurchases(timestamp);
    }

    @Override
    public void changePrice(long goodsId, long priceNQT) {
        DigitalGoodsStore.Goods goods = goodsTable.get(goodsDbKeyFactory.newKey(goodsId));
        if (!goods.isDelisted()) {
            goods.changePrice(priceNQT);
            goodsTable.insert(goods);
            goodsListeners.notify(goods, DigitalGoodsStore.Event.GOODS_PRICE_CHANGE);
        } else {
            throw new IllegalStateException("Can't change price of delisted goods");
        }
    }

    @Override
    public void deliver(Transaction transaction, Attachment.DigitalGoodsDelivery attachment) {
        DigitalGoodsStore.Purchase purchase = getPendingPurchase(attachment.getPurchaseId());
        if (purchase == null) {
            throw new RuntimeException("cant find purchase with id " + attachment.getPurchaseId());
        }
        setPending(purchase, false);
        long totalWithoutDiscount = Convert.safeMultiply(purchase.getQuantity(), purchase.getPriceNQT());
        Account buyer = accountService.getAccount(purchase.getBuyerId());
        accountService.addToBalanceNQT(buyer, Convert.safeSubtract(attachment.getDiscountNQT(), totalWithoutDiscount));
        accountService.addToUnconfirmedBalanceNQT(buyer, attachment.getDiscountNQT());
        Account seller = accountService.getAccount(transaction.getSenderId());
        accountService.addToBalanceAndUnconfirmedBalanceNQT(seller, Convert.safeSubtract(totalWithoutDiscount, attachment.getDiscountNQT()));
        purchase.setEncryptedGoods(attachment.getGoods(), attachment.goodsIsText());
        purchaseTable.insert(purchase);
        purchase.setDiscountNQT(attachment.getDiscountNQT());
        purchaseTable.insert(purchase);
        purchaseListeners.notify(purchase, DigitalGoodsStore.Event.DELIVERY);
    }

    @Override
    public DigitalGoodsStore.Purchase getPendingPurchase(long purchaseId) {
        DigitalGoodsStore.Purchase purchase = getPurchase(purchaseId);
        return purchase == null || !purchase.isPending() ? null : purchase;
    }

    @Override
    public void setPending(DigitalGoodsStore.Purchase purchase, boolean pendingValue) {
        purchase.setPending(pendingValue);
        purchaseTable.insert(purchase);
    }

}
