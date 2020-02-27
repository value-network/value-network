package val.assetexchange;

import val.*;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.OrderStore;
import val.services.AccountService;
import val.util.Convert;

class OrderServiceImpl {

    private final OrderStore orderStore;
    private final VersionedEntityTable<Order.Ask> askOrderTable;
    private final DbKey.LongKeyFactory<Order.Ask> askOrderDbKeyFactory;
    private final VersionedEntityTable<Order.Bid> bidOrderTable;
    private final DbKey.LongKeyFactory<Order.Bid> bidOrderDbKeyFactory;
    private final AccountService accountService;
    private final TradeServiceImpl tradeService;

    public OrderServiceImpl(OrderStore orderStore, AccountService accountService, TradeServiceImpl tradeService) {
        this.orderStore = orderStore;
        this.askOrderTable = orderStore.getAskOrderTable();
        this.askOrderDbKeyFactory = orderStore.getAskOrderDbKeyFactory();
        this.bidOrderTable = orderStore.getBidOrderTable();
        this.bidOrderDbKeyFactory = orderStore.getBidOrderDbKeyFactory();

        this.accountService = accountService;
        this.tradeService = tradeService;
    }

    public Order.Ask getAskOrder(long orderId) {
        return askOrderTable.get(askOrderDbKeyFactory.newKey(orderId));
    }

    public Order.Bid getBidOrder(long orderId) {
        return bidOrderTable.get(bidOrderDbKeyFactory.newKey(orderId));
    }

    public DbIterator<Order.Ask> getAllAskOrders(int from, int to) {
        return askOrderTable.getAll(from, to);
    }

    public DbIterator<Order.Bid> getAllBidOrders(int from, int to) {
        return bidOrderTable.getAll(from, to);
    }

    public DbIterator<Order.Bid> getSortedBidOrders(long assetId, int from, int to) {
        return orderStore.getSortedBids(assetId, from, to);
    }

    public DbIterator<Order.Ask> getAskOrdersByAccount(long accountId, int from, int to) {
        return orderStore.getAskOrdersByAccount(accountId, from, to);
    }

    public DbIterator<Order.Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return orderStore.getAskOrdersByAccountAsset(accountId, assetId, from, to);
    }

    public DbIterator<Order.Ask> getSortedAskOrders(long assetId, int from, int to) {
        return orderStore.getSortedAsks(assetId, from, to);
    }

    public int getBidCount() {
        return bidOrderTable.getCount();
    }

    public int getAskCount() {
        return askOrderTable.getCount();
    }

    public DbIterator<Order.Bid> getBidOrdersByAccount(long accountId, int from, int to) {
        return orderStore.getBidOrdersByAccount(accountId, from, to);
    }

    public DbIterator<Order.Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
        return orderStore.getBidOrdersByAccountAsset(accountId, assetId, from, to);
    }

    public void removeBidOrder(long orderId) {
        bidOrderTable.delete(getBidOrder(orderId));
    }

    public void removeAskOrder(long orderId) {
        askOrderTable.delete(getAskOrder(orderId));
    }

    public void addAskOrder(Transaction transaction, Attachment.ColoredCoinsAskOrderPlacement attachment) {
        DbKey dbKey = askOrderDbKeyFactory.newKey(transaction.getId());
        Order.Ask order = new Order.Ask(dbKey, transaction, attachment);
        askOrderTable.insert(order);
        matchOrders(attachment.getAssetId());
    }

    public void addBidOrder(Transaction transaction, Attachment.ColoredCoinsBidOrderPlacement attachment) {
        DbKey dbKey = bidOrderDbKeyFactory.newKey(transaction.getId());
        Order.Bid order = new Order.Bid(dbKey, transaction, attachment);
        bidOrderTable.insert(order);
        matchOrders(attachment.getAssetId());
    }

    private Order.Ask getNextAskOrder(long assetId) {
        return ValNetwork.getStores().getOrderStore().getNextOrder(assetId);
    }

    private Order.Bid getNextBidOrder(long assetId) {
        return ValNetwork.getStores().getOrderStore().getNextBid(assetId);
    }

    private void matchOrders(long assetId) {

        Order.Ask askOrder;
        Order.Bid bidOrder;

        while ((askOrder = getNextAskOrder(assetId)) != null
                && (bidOrder = getNextBidOrder(assetId)) != null) {

            if (askOrder.getPriceNQT() > bidOrder.getPriceNQT()) {
                break;
            }


            Trade trade = tradeService.addTrade(assetId, ValNetwork.getBlockchain().getLastBlock(), askOrder, bidOrder);

            askOrderUpdateQuantityQNT(askOrder, Convert.safeSubtract(askOrder.getQuantityQNT(), trade.getQuantityQNT()));
            Account askAccount = accountService.getAccount(askOrder.getAccountId());
            accountService.addToBalanceAndUnconfirmedBalanceNQT(askAccount, Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT()));
            accountService.addToAssetBalanceQNT(askAccount, assetId, -trade.getQuantityQNT());

            bidOrderUpdateQuantityQNT(bidOrder, Convert.safeSubtract(bidOrder.getQuantityQNT(), trade.getQuantityQNT()));
            Account bidAccount = accountService.getAccount(bidOrder.getAccountId());
            accountService.addToAssetAndUnconfirmedAssetBalanceQNT(bidAccount, assetId, trade.getQuantityQNT());
            accountService.addToBalanceNQT(bidAccount, -Convert.safeMultiply(trade.getQuantityQNT(), trade.getPriceNQT()));
            accountService.addToUnconfirmedBalanceNQT(bidAccount, Convert.safeMultiply(trade.getQuantityQNT(), (bidOrder.getPriceNQT() - trade.getPriceNQT())));

        }

    }

    private void askOrderUpdateQuantityQNT(Order.Ask askOrder, long quantityQNT) {
        askOrder.setQuantityQNT(quantityQNT);
        if (quantityQNT > 0) {
            askOrderTable.insert(askOrder);
        } else if (quantityQNT == 0) {
            askOrderTable.delete(askOrder);
        } else {
            throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                    + " for order: " + Convert.toUnsignedLong(askOrder.getId()));
        }
    }

    private void bidOrderUpdateQuantityQNT(Order.Bid bidOrder, long quantityQNT) {
        bidOrder.setQuantityQNT(quantityQNT);
        if (quantityQNT > 0) {
            bidOrderTable.insert(bidOrder);
        } else if (quantityQNT == 0) {
            bidOrderTable.delete(bidOrder);
        } else {
            throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                    + " for order: " + Convert.toUnsignedLong(bidOrder.getId()));
        }
    }


}
