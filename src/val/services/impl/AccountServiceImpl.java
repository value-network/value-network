package val.services.impl;

import org.jooq.*;
import org.jooq.impl.DSL;
import val.*;
import val.Attachment.PledgeAssignment;
import val.Attachment.UnpledgeAssignment;
import val.Constants;
import val.Transaction;
import val.crypto.Crypto;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedBatchEntityTable;
import val.db.VersionedEntityTable;
import val.db.sql.Db;
import val.db.store.AccountStore;
import val.db.store.AssetTransferStore;
import val.db.store.PledgeStore;
import val.db.store.PoolMinerStore;
import val.props.Props;
import val.schema.Tables;
import val.services.AccountService;
import val.util.Convert;
import val.util.Listener;
import val.util.Listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccountServiceImpl implements AccountService {

    static long totalReward;
    static long totalBurning;
    static long totalForged;
    private final AccountStore accountStore;
    private final VersionedBatchEntityTable<Account> accountTable;
    private final DbKey.LongKeyFactory<Account> accountDbKeyFactory;
    private final VersionedEntityTable<Account.AccountAsset> accountAssetTable;
    private final DbKey.LinkKeyFactory<Account.AccountAsset> accountAssetKeyFactory;
    private final VersionedEntityTable<Account.RewardRecipientAssignment> rewardRecipientAssignmentTable;
    private final DbKey.LongKeyFactory<Account.RewardRecipientAssignment> rewardRecipientAssignmentKeyFactory;
    private final PledgeStore pledgeStore;
    private final VersionedEntityTable<Account.Pledges> pledgeTable;
    private final val.db.sql.DbKey.LongKeyFactory<Account.Pledges> pledgeDbKeyFactory;
    private final PoolMinerStore poolMinerStore;
    private final Listeners<Account.Pledges, Account.Event> pledgeListeners = new Listeners<>();
    private final AssetTransferStore assetTransferStore;
    private final Listeners<Account, Account.Event> listeners = new Listeners<>();
    private final Listeners<Account.AccountAsset, Account.Event> assetListeners = new Listeners<>();

    public AccountServiceImpl(AccountStore accountStore, AssetTransferStore assetTransferStore, PledgeStore pledgeStore,
                              PoolMinerStore poolMinerStore) {
        this.accountStore = accountStore;
        this.accountTable = accountStore.getAccountTable();
        this.accountDbKeyFactory = accountStore.getAccountKeyFactory();
        this.assetTransferStore = assetTransferStore;
        this.accountAssetTable = accountStore.getAccountAssetTable();
        this.accountAssetKeyFactory = accountStore.getAccountAssetKeyFactory();
        this.rewardRecipientAssignmentTable = accountStore.getRewardRecipientAssignmentTable();
        this.rewardRecipientAssignmentKeyFactory = accountStore.getRewardRecipientAssignmentKeyFactory();
        this.pledgeStore = pledgeStore;
        this.pledgeDbKeyFactory = pledgeStore.getPledgeKeyFactory();
        this.pledgeTable = pledgeStore.getPledgesTable();
        this.poolMinerStore = poolMinerStore;
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    @Override
    public boolean addListener(Listener<Account> listener, Account.Event eventType) {
        return listeners.addListener(listener, eventType);
    }

    @Override
    public boolean addAssetListener(Listener<Account.AccountAsset> listener, Account.Event eventType) {
        return assetListeners.addListener(listener, eventType);
    }

    @Override
    public Account getAccount(long id) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id));
    }

    @Override
    public List<Long> getAccountByLikeId(String id) {
        List<Long> allId = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {
            // Result<Record1<Long>> results =
            // ctx.select(Tables.ACCOUNT.ID).from(Tables.ACCOUNT)
            // .where(Tables.ACCOUNT.LATEST.eq(true).and(Tables.ACCOUNT.ID.like(id +
            // "%"))).getQuery().fetch();
            // for (Record1<Long> record : results) {
            // Long account = record.getValue(Tables.ACCOUNT.ID);
            // if (account != 0) {
            // allId.add(account);
            // }
            // }
            Result<Record> results = ctx
                    .fetch("select id from account where latest=1 and cast(id as unsigned) like '" + id + "%'");
            for (Record record : results) {
                Long account = (Long) record.getValue("id");
                if (account != 0) {
                    allId.add(account);
                }
            }
            return allId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Long> getPoolAccountByLikeId(String id) {
        List<Long> allId = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {
            // Result<Record1<Long>> results =
            // ctx.select(Tables.ACCOUNT.ID).from(Tables.ACCOUNT)
            // .where(Tables.ACCOUNT.LATEST.eq(true).and(Tables.ACCOUNT.TOTAL_PLEDGED.greaterThan(0L)).and(Tables.ACCOUNT.ID.like(id
            // + "%"))).getQuery().fetch();
            // for (Record1<Long> record : results) {
            // Long account = record.getValue(Tables.ACCOUNT.ID);
            // if (account != 0) {
            // allId.add(account);
            // }
            // }
            Result<Record> results = ctx.fetch(
                    "select id from account where latest=1 and account_role=1 and cast(id as unsigned) like '" + id + "%'");
            for (Record record : results) {
                Long account = (Long) record.getValue("id");
                if (account != 0) {
                    allId.add(account);
                }
            }
            return allId;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public Account getAccount(long id, int height) {
        return id == 0 ? null : accountTable.get(accountDbKeyFactory.newKey(id), height);
    }

    public DbIterator<Account> getMinePoolAccount(ArrayList<Long> minePools) {
        Condition conditions = DSL.trueCondition();
        try (DSLContext ctx = Db.getDSLContext()) {
            Condition condition = DSL.falseCondition();
            for (Long minePool : minePools) {
                // System.out.printf("query mine pool id: %s\n", minePool);
                condition = condition.or(Tables.ACCOUNT.ID.eq(minePool));
            }
            SelectQuery query = ctx.selectFrom(Tables.ACCOUNT)
                    .where(conditions.and(Tables.ACCOUNT.LATEST.eq(true)).and(condition))
                    .orderBy(Tables.ACCOUNT.field("total_pledged", Long.class).desc()).getQuery();
            // System.out.printf("query sql: %s\n", query.getSQL());
            return accountTable.getManyBy(ctx, query, true);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    public int getMinePoolMinerCount(long accountId) {
        List<Condition> conditions = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {
            conditions.add(Tables.PLEDGES.RECIP_ID.eq(accountId));
            conditions.add(Tables.PLEDGES.LATEST.eq(true));
            // conditions.add(Tables.PLEDGES.PLEDGE_TOTAL.gt(0L));
            return ctx.selectCount().from(Tables.PLEDGES).where(conditions).fetchOne(0, int.class);
        } catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }

    }

    @Override
    public Account getAccount(byte[] publicKey) {

        final Account account = accountTable.get(accountDbKeyFactory.newKey(getId(publicKey)));

        if (account == null) {
            return null;
        }

        if (account.getPublicKey() == null || Arrays.equals(account.getPublicKey(), publicKey)) {
            return account;
        }

        throw new RuntimeException("DUPLICATE KEY for account " + Convert.toUnsignedLong(account.getId()) + " existing key "
                + Convert.toHexString(account.getPublicKey()) + " new key " + Convert.toHexString(publicKey));
    }

    @Override
    public DbIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to) {
        return assetTransferStore.getAccountAssetTransfers(accountId, from, to);
    }

    @Override
    public DbIterator<Account.AccountAsset> getAssets(long accountId, int from, int to) {
        return accountStore.getAssets(from, to, accountId);
    }

    @Override
    public long getAccountPledged() {
        return pledgeStore.getBlockchainPledged();
    }

    @Override
    public DbIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId) {
        return accountStore.getAccountsWithRewardRecipient(recipientId);
    }

    @Override
    public DbIterator<Account> getAccountsWithName(String name) {
        return accountTable.getManyBy(Tables.ACCOUNT.NAME.equalIgnoreCase(name), 0, -1);
    }

    @Override
    public DbIterator<Account> getAllAccounts(int from, int to) {
        return accountTable.getAll(from, to);
    }

    @Override
    public Account getOrAddAccount(long id) {
        Account account = accountTable.get(accountDbKeyFactory.newKey(id));
        if (account == null) {
            account = new Account(id);
            accountTable.insert(account);
        }
        return account;
    }

    @Override
    public void flushAccountTable() {
        accountTable.finish();
    }

    @Override
    public int getCount() {
        return accountTable.getCount();
    }

    @Override
    public void addToForgedBalanceNQT(Account account, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        account.setForgedBalanceNQT(Convert.safeAdd(account.getForgedBalanceNQT(), amountNQT));
        accountTable.insert(account);
    }

    @Override
    public void addToPledgeRewardBalance(Account account, long amount) {
        if (0 == amount) {
            return;
        }
        account.setPledgeRewardBalance(Convert.safeAdd(account.getPledgeRewardBalance(), amount));
        accountTable.insert(account);
    }

    @Override
    public void setAccountInfo(Account account, String name, String description) {
        account.setName(Convert.emptyToNull(name.trim()));
        account.setDescription(Convert.emptyToNull(description.trim()));
        accountTable.insert(account);
    }

    @Override
    public void addToAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        Account.AccountAsset accountAsset;

        DbKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
        accountAsset = accountAssetTable.get(newKey);
        long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityQNT();
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new Account.AccountAsset(newKey, account.getId(), assetId, assetBalance, 0);
        } else {
            accountAsset.setQuantityQNT(assetBalance);
        }
        saveAccountAsset(accountAsset);
        listeners.notify(account, Account.Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.ASSET_BALANCE);
    }

    @Override
    public void addToUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        Account.AccountAsset accountAsset;
        DbKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
        accountAsset = accountAssetTable.get(newKey);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new Account.AccountAsset(newKey, account.getId(), assetId, 0, unconfirmedAssetBalance);
        } else {
            accountAsset.setUnconfirmedQuantityQNT(unconfirmedAssetBalance);
        }
        saveAccountAsset(accountAsset);
        listeners.notify(account, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.UNCONFIRMED_ASSET_BALANCE);
    }

    @Override
    public void addToAssetAndUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT) {
        if (quantityQNT == 0) {
            return;
        }
        Account.AccountAsset accountAsset;
        DbKey newKey = accountAssetKeyFactory.newKey(account.getId(), assetId);
        accountAsset = accountAssetTable.get(newKey);
        long assetBalance = accountAsset == null ? 0 : accountAsset.getQuantityQNT();
        assetBalance = Convert.safeAdd(assetBalance, quantityQNT);
        long unconfirmedAssetBalance = accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
        unconfirmedAssetBalance = Convert.safeAdd(unconfirmedAssetBalance, quantityQNT);
        if (accountAsset == null) {
            accountAsset = new Account.AccountAsset(newKey, account.getId(), assetId, assetBalance, unconfirmedAssetBalance);
        } else {
            accountAsset.setQuantityQNT(assetBalance);
            accountAsset.setUnconfirmedQuantityQNT(unconfirmedAssetBalance);
        }
        saveAccountAsset(accountAsset);
        listeners.notify(account, Account.Event.ASSET_BALANCE);
        listeners.notify(account, Account.Event.UNCONFIRMED_ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.ASSET_BALANCE);
        assetListeners.notify(accountAsset, Account.Event.UNCONFIRMED_ASSET_BALANCE);
    }

    @Override
    public void addToBalanceNQT(Account account, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        // System.out.printf("account.getBalanceNQT():
        // %s,account.getUnconfirmedBalanceNQT(): %s, amount:
        // %s\n",account.getBalanceNQT(),account.getUnconfirmedBalanceNQT(),amountNQT);
        account.setBalanceNQT(Convert.safeAdd(account.getBalanceNQT(), amountNQT));
        // System.out.printf("after account.getBalanceNQT():
        // %s\n",account.getBalanceNQT());

        account.checkBalance();
        accountTable.insert(account);
        listeners.notify(account, Account.Event.BALANCE);
    }

    @Override
    public void addToUnconfirmedBalanceNQT(Account account, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        account.setUnconfirmedBalanceNQT(Convert.safeAdd(account.getUnconfirmedBalanceNQT(), amountNQT));
        account.checkBalance();
        accountTable.insert(account);
        listeners.notify(account, Account.Event.UNCONFIRMED_BALANCE);
    }

    @Override
    public void addToBalanceAndUnconfirmedBalanceNQT(Account account, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }

        account.setBalanceNQT(Convert.safeAdd(account.getBalanceNQT(), amountNQT));
        account.setUnconfirmedBalanceNQT(Convert.safeAdd(account.getUnconfirmedBalanceNQT(), amountNQT));
        account.checkBalance();
        accountTable.insert(account);
        listeners.notify(account, Account.Event.BALANCE);
        listeners.notify(account, Account.Event.UNCONFIRMED_BALANCE);
    }

    @Override
    public Account.RewardRecipientAssignment getRewardRecipientAssignment(Account account) {
        return getRewardRecipientAssignment(account.getId());
    }

    private Account.RewardRecipientAssignment getRewardRecipientAssignment(Long id) {
        return rewardRecipientAssignmentTable.get(rewardRecipientAssignmentKeyFactory.newKey(id));
    }

    @Override
    public void setRewardRecipientAssignment(Account account, Long recipient) {
        int currentHeight = ValNetwork.getBlockchain().getLastBlock().getHeight();
        Account.RewardRecipientAssignment assignment = getRewardRecipientAssignment(account.getId());
        if (assignment == null) {
            DbKey dbKey = rewardRecipientAssignmentKeyFactory.newKey(account.getId());
            assignment = new Account.RewardRecipientAssignment(account.getId(), account.getId(), recipient,
                    (int) (currentHeight + Constants.CHAIN_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME), dbKey);
        } else {
            assignment.setRecipient(recipient, (int) (currentHeight + Constants.CHAIN_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME));
        }
        rewardRecipientAssignmentTable.insert(assignment);
    }

    @Override
    public long getUnconfirmedAssetBalanceQNT(Account account, long assetId) {
        DbKey newKey = ValNetwork.getStores().getAccountStore().getAccountAssetKeyFactory().newKey(account.getId(), assetId);
        Account.AccountAsset accountAsset = accountAssetTable.get(newKey);
        return accountAsset == null ? 0 : accountAsset.getUnconfirmedQuantityQNT();
    }

    private void saveAccountAsset(Account.AccountAsset accountAsset) {
        accountAsset.checkBalance();
        if (accountAsset.getQuantityQNT() > 0 || accountAsset.getUnconfirmedQuantityQNT() > 0) {
            accountAssetTable.insert(accountAsset);
        } else {
            accountAssetTable.delete(accountAsset);
        }
    }

    @Override
    public boolean addPledgeListener(Listener<Account.Pledges> listener, Account.Event eventType) {
        return pledgeListeners.addListener(listener, eventType);
    }

    @Override
    public Account.Pledges getAccountPledge(long accountId) {
        return pledgeStore.getAccountPledge(accountId);
    }

    @Override
    public long getMinerTotalReward(long accountId) {
        return pledgeStore.getMinerTotalReward(accountId);
    }

    @Override
    public Account.Pledges getAccountPledgeOne(long accountId, long totalPledge) {
        return pledgeStore.getAccountPledgeOne(accountId, totalPledge);
    }

    @Override
    public Account.Pledges getAccountPledge(long accountId, int height) {
        return pledgeStore.getAccountPledge(accountId, height);
    }

    @Override
    public Account.Pledges getAccountPledge(long accountId, long recipientId) {
        return pledgeStore.getAccountPledge(accountId, recipientId);
    }

    @Override
    public Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> getAccountPledges(
            long accountId, int page, int limit) {
        return accountStore.getAccountPledges(accountId, page, limit);
    }

    @Override
    public int getAccountPledgesCount(long accountId) {
        return accountStore.getAccountPledgesCount(accountId);
    }

    @Override
    public void flushPledgeTable() {
        pledgeTable.finish();
    }

    @Override
    public int getPledgeCount() {
        return pledgeTable.getCount();
    }

    @Override
    public void addToPledgeAmount(Account.Pledges pledge, long amountNQT, int pledgeLatestTime) {
        if (amountNQT == 0) {
            return;
        }

        pledge.setPledgeTotal(Convert.safeAdd(pledge.getPledgeTotal(), amountNQT));
        pledge.setPledgeLatestTime(pledgeLatestTime);
        pledgeTable.insert(pledge);
        pledgeListeners.notify(pledge, Account.Event.PLEDGE);
    }

    @Override
    public void addToUnplegeAmount(Account.Pledges pledge, long amountNQT, int withdrawTime) {
        pledge.setPledgeTotal(Convert.safeAdd(pledge.getPledgeTotal(), -amountNQT));
        pledge.setUnpledgeTotal(Convert.safeAdd(pledge.getUnpledgeTotal(), amountNQT));
        pledge.setWithdrawTime(withdrawTime);
        pledgeTable.insert(pledge);
        pledgeListeners.notify(pledge, Account.Event.UNPLEDGE);
    }

    @Override
    public void addOrUpdatePledge(Transaction transaction, PledgeAssignment attachment) {
        long amountNQT = attachment.getPledgeAmount();
        long pledgeLatestTime = attachment.getPledgeLatestTime();
        if (amountNQT == 0) {
            System.out.println("addOrUpdatePledge - amountNQT == 0");
            return;
        }

        Account.Pledges pledge = getAccountPledge(transaction.getSenderId());
        // System.out.printf("transaction.getSenderId(): %s,
        // transaction.getRecipientId() : %s \n",transaction.getSenderId(),
        // transaction.getRecipientId());
        if (pledge == null) {
            DbKey pledgeDBId = pledgeDbKeyFactory.newKey(transaction.getSenderId());
            pledge = new Account.Pledges(pledgeDBId, transaction.getId(), transaction.getSenderId(),
                    transaction.getRecipientId(), 0L, 0, 0L, 0, ValNetwork.getBlockchain().getLastBlock().getHeight() + 1, 1);
        }
        // System.out.printf("add or update pledge, transaction id: %s",
        // transaction.getId());
        pledge.setId(transaction.getId());
        pledge.setRecipID(transaction.getRecipientId());
        pledge.setPledgeTotal(Convert.safeAdd(pledge.getPledgeTotal(), amountNQT));
        pledge.setPledgeLatestTime(pledgeLatestTime);
        pledge.setHeight(ValNetwork.getBlockchain().getLastBlock().getHeight() + 1);

        // System.out.print("start insert pledgeTable \n");
        pledgeTable.insert(pledge);
        // System.out.print("end insert pledgeTable \n");
        pledgeListeners.notify(pledge, Account.Event.PLEDGE);
    }

    @Override
    public void UpdateUnPledge(Transaction transaction, UnpledgeAssignment attachment) {
        long amountNQT = attachment.getUnpledgeAmount();
        long withdrawTime = attachment.getWithdrawTime();
        if (amountNQT == 0) {
            return;
        }

        Account.Pledges pledge = getAccountPledge(transaction.getSenderId());
        if (pledge == null) {
            return;
        }

        pledge.setHeight(ValNetwork.getBlockchain().getLastBlock().getHeight() + 1);
        pledge.setPledgeTotal(Convert.safeAdd(pledge.getPledgeTotal(), -amountNQT));
       // pledge.setUnpledgeTotal(Convert.safeAdd(pledge.getUnpledgeTotal(), amountNQT));
        pledge.setWithdrawTime(withdrawTime);

        pledgeTable.insert(pledge);
        pledgeListeners.notify(pledge, Account.Event.UNPLEDGE);
    }

    @Override
    public void updateToPledgeTotal(Account account, long amountNQT) {
        if (amountNQT == 0) {
            return;
        }
        account.setTotalPledged(Convert.safeAdd(account.getTotalPledged(), amountNQT));
        accountTable.insert(account);
    }

    @Override
    public DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId) {
        return pledgeStore.getPoolAllMinerPledge(accountId);
    }

    @Override
    public DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId, int page, int limit) {
        return pledgeStore.getPoolAllMinerPledge(accountId, page, limit);
    }

    @Override
    public int getPoolAllMinerPledgeCount(long accountId) {
        return pledgeStore.getPoolAllMinerPledgeCount(accountId);
    }

    @Override
    public DbIterator<Account.Pledges> getPledges(long accountId, int page, int limit) {
        return pledgeStore.getPledges(accountId, page, limit);
    }

    @Override
    public int getPledgesCount(long accountId) {
        return pledgeStore.getPledgesCount(accountId);
    }

    public Account.PoolMiner getGrantPoolMiner(long accountId, long poolId) {
        return poolMinerStore.getGrantPoolMiner(accountId, poolId);
    }

    public Result<Record6<Integer, Long, Long, Integer, Long, Long>> getGrantPoolMiners(long poolId, int page,
                                                                                        int limit) {
        return poolMinerStore.getGrantPoolMiners(poolId, page, limit);
    }

    public int getGrantPoolMinerCount(long poolId) {
        return poolMinerStore.getGrantPoolMinerCount(poolId);
    }

    public void addPoolMiner(long accountId, long poolId, int status, long cTime) throws Exception {
        if (status != 0 && status != 1) {
            System.out.println("addPoolMiner - status not in( 0, 1 )");
            return;
        }

        Account.PoolMiner poolMiner = getGrantPoolMiner(accountId, poolId);
        if (poolMiner == null) {
            Account.PoolMiner newPoolMiner = new Account.PoolMiner(0, accountId, poolId, status, cTime, 0L);
            System.out.print("start insert  PoolMiner  \n");
            poolMinerStore.savePoolMiner(newPoolMiner);
        } else {
            throw new RuntimeException("add exist poolMiner, pool: " + poolId + ",miner: " + accountId);
        }
        System.out.print("end insert  PoolMiner  \n");
    }

    public void UpdatePoolMiner(long accountId, long poolId, int status, long mTime) {
        if (status != 0 && status != 1) {
            System.out.println("addPoolMiner - status not in( 0, 1 )");
            return;
        }

        Account.PoolMiner poolMiner = getGrantPoolMiner(accountId, poolId);
        if (poolMiner == null) {
            return;
        }
        poolMinerStore.revokePoolMiner(accountId, poolId, status, mTime);
    }

    public void burningRemanentForgeReward(Block block, long rewardAmount) {
        long maxReward = MinePool.getInstance().getMaxPledgeRewardCache();

        totalForged += maxReward;
        totalReward += rewardAmount;
        long burningAmount = Convert.safeSubtract(maxReward, rewardAmount);
        totalBurning += burningAmount;
        // System.out.printf(
        //     "block:[%d->%d], burningAmount:%d, totalReward:%d, totalForged:%d, totalReward:%d, totalBurning:%d\n",
        //     block.getHeight(), block.getId(), burningAmount, rewardAmount, totalForged, totalReward, totalBurning);
        if (burningAmount > 0) {
            Account burningAccount = getOrAddAccount(getId(Genesis.getCreatorPublicKey()));

            addToBalanceAndUnconfirmedBalanceNQT(burningAccount, burningAmount);
            // System.out.printf("burning to accountID:'%d\t%d\n", Genesis.CREATOR_ID, burningAmount);
        }
    }

    public boolean checkBalance(Block block) {
        List<Long> balanceList = accountStore.getTotalBalance();
        List<Long> pledgeList = accountStore.getTotalPledge();

        Long total = new Long(0);
        if (balanceList.size() == 3) {
            total += balanceList.get(0);
        }
        if (pledgeList.size() == 2) {
            total += pledgeList.get(0) + pledgeList.get(1);
        }

        long totalReward = MinePool.getInstance().totalReward(block.getHeight());
        Long all = totalReward + 30000000000000000L;
        System.out.printf("check balance for block:%d, db balance total:%d, calculate total:%d, totalReward:%d\n", block.getHeight(), total.longValue(), all.longValue(), totalReward);
        if (all.longValue() != total.longValue()) {
            System.out.printf("##### balance collapse at block:%d, balance wrong(%d---%d), gap:(%d)\n", block.getHeight(),
                    all.longValue(), total.longValue(), total.longValue() - all.longValue());
            // System.exit(10);
        }
        String maxHeight = ValNetwork.getPropertyService().getString(Props.TEST_MAX_HEIGHT);
        if (!maxHeight.equals("0")) {
            Long maxH = new Long(maxHeight);
            if (maxH.longValue() <= block.getHeight()) {
                return true;
            }
        }
        return false;
    }
}
