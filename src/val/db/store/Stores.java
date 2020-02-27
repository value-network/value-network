package val.db.store;

import val.db.cache.DBCacheManagerImpl;
import val.db.sql.*;
import val.props.PropertyService;
import val.services.TimeService;
import val.unconfirmedtransactions.UnconfirmedTransactionStore;
import val.unconfirmedtransactions.UnconfirmedTransactionStoreImpl;

public class Stores {
    private final AccountStore accountStore;
    private final AliasStore aliasStore;
    private final AssetTransferStore assetTransferStore;
    private final AssetStore assetStore;
    private final ATStore atStore;
    private final BlockchainStore blockchainStore;
    private final DigitalGoodsStoreStore digitalGoodsStoreStore;
    private final EscrowStore escrowStore;
    private final OrderStore orderStore;
    private final TradeStore tradeStore;
    private final SubscriptionStore subscriptionStore;
    private final PledgeStore pledgeStore;
    private final PoolMinerStore poolMinerStore;
    private final GlobalParameterStore globalParameterStore;
    private final UnconfirmedTransactionStore unconfirmedTransactionStore;

    public Stores(DerivedTableManager derivedTableManager, DBCacheManagerImpl dbCacheManager, TimeService timeService, PropertyService propertyService) {
        this.accountStore = new SqlAccountStore(derivedTableManager, dbCacheManager);
        this.aliasStore = null; //new SqlAliasStore(derivedTableManager);
        this.assetStore = null; //new SqlAssetStore(derivedTableManager);
        this.assetTransferStore = null; //new SqlAssetTransferStore(derivedTableManager);
        this.atStore = null; //new SqlATStore(derivedTableManager);
        this.blockchainStore = new SqlBlockchainStore(timeService);
        this.digitalGoodsStoreStore = null; //new SqlDigitalGoodsStoreStore(derivedTableManager);
        this.escrowStore = null; //new SqlEscrowStore(derivedTableManager);
        this.orderStore = null; //new SqlOrderStore(derivedTableManager);
        this.tradeStore = null; //new SqlTradeStore(derivedTableManager);
        this.subscriptionStore = null; //new SqlSubscriptionStore(derivedTableManager);
        this.pledgeStore = new SqlPledgeStore(derivedTableManager);
        this.poolMinerStore = new SqlPoolMinerStore();
        this.globalParameterStore = new SqlGlobalParameterStore(derivedTableManager);
        this.unconfirmedTransactionStore = new UnconfirmedTransactionStoreImpl(timeService, propertyService, accountStore, pledgeStore);
    }

    public PledgeStore getPledgeStore() {
        return pledgeStore;
    }

    public PoolMinerStore getPoolMinerStore() {
        return poolMinerStore;
    }

    public GlobalParameterStore getGlobalParameterStore() {
        return globalParameterStore;
    }

    public AccountStore getAccountStore() {
        return accountStore;
    }

    public AliasStore getAliasStore() {
        return aliasStore;
    }

    public AssetStore getAssetStore() {
        return assetStore;
    }

    public AssetTransferStore getAssetTransferStore() {
        return assetTransferStore;
    }

    public ATStore getAtStore() {
        return atStore;
    }

    public BlockchainStore getBlockchainStore() {
        return blockchainStore;
    }

    public DigitalGoodsStoreStore getDigitalGoodsStoreStore() {
        return digitalGoodsStoreStore;
    }

    public void beginTransaction() {
        Db.beginTransaction();
    }

    public void commitTransaction() {
        Db.commitTransaction();
    }

    public void rollbackTransaction() {
        Db.rollbackTransaction();
    }

    public void endTransaction() {
        Db.endTransaction();
    }

    public EscrowStore getEscrowStore() {
        return escrowStore;
    }

    public OrderStore getOrderStore() {
        return orderStore;
    }

    public TradeStore getTradeStore() {
        return tradeStore;
    }

    public UnconfirmedTransactionStore getUnconfirmedTransactionStore() {
        return unconfirmedTransactionStore;
    }

    public SubscriptionStore getSubscriptionStore() {
        return subscriptionStore;
    }

}
