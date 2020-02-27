package val.services;

import org.jooq.Record14;
import org.jooq.Record6;
import org.jooq.Result;
import val.*;
import val.db.DbIterator;
import val.util.Listener;

import java.util.ArrayList;
import java.util.List;

public interface AccountService {

    boolean addListener(Listener<Account> listener, Account.Event eventType);

    boolean addAssetListener(Listener<Account.AccountAsset> listener, Account.Event eventType);

    Account getAccount(long id);

    List<Long> getAccountByLikeId(String id);

    List<Long> getPoolAccountByLikeId(String id);

    Account getAccount(long id, int height);

    Account getAccount(byte[] publicKey);

    DbIterator<AssetTransfer> getAssetTransfers(long accountId, int from, int to);

    DbIterator<Account.AccountAsset> getAssets(long accountId, int from, int to);

    DbIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

    DbIterator<Account> getAccountsWithName(String name);

    DbIterator<Account> getMinePoolAccount(ArrayList<Long> minePools);

    int getMinePoolMinerCount(long accountId);

    DbIterator<Account> getAllAccounts(int from, int to);

    Account getOrAddAccount(long id);

    void flushAccountTable();

    int getCount();

    long getAccountPledged();

    void addToForgedBalanceNQT(Account account, long amountNQT);

    void addToPledgeRewardBalance(Account account, long amount);

    void setAccountInfo(Account account, String name, String description);

    void addToAssetBalanceQNT(Account account, long assetId, long quantityQNT);

    void addToUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT);

    void addToAssetAndUnconfirmedAssetBalanceQNT(Account account, long assetId, long quantityQNT);

    void addToBalanceNQT(Account account, long amountNQT);

    void addToUnconfirmedBalanceNQT(Account account, long amountNQT);

    void addToBalanceAndUnconfirmedBalanceNQT(Account account, long amountNQT);

    void updateToPledgeTotal(Account account, long pledgeTotal);

    Account.RewardRecipientAssignment getRewardRecipientAssignment(Account account);

    void setRewardRecipientAssignment(Account account, Long recipient);

    long getUnconfirmedAssetBalanceQNT(Account account, long assetId);

    boolean addPledgeListener(Listener<Account.Pledges> listener, Account.Event eventType);


    Account.Pledges getAccountPledge(long accountId);

    long getMinerTotalReward(long accountId);

    Account.Pledges getAccountPledge(long accountId, int height);

    Account.Pledges getAccountPledge(long accountId, long recipientId);

    Account.Pledges getAccountPledgeOne(long accountId, long totalPledge);

    Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> getAccountPledges(long accountId, int page, int limit);

    int getAccountPledgesCount(long accountId);


    void flushPledgeTable();

    int getPledgeCount();

    void addOrUpdatePledge(Transaction transaction, Attachment.PledgeAssignment attachment);

    void UpdateUnPledge(Transaction transaction, Attachment.UnpledgeAssignment attachment);

    void addPoolMiner(long accountId, long poolId, int status, long cTime) throws Exception;

    void UpdatePoolMiner(long accountId, long poolId, int status, long mTime);

    Result<Record6<Integer, Long, Long, Integer, Long, Long>> getGrantPoolMiners(long poolId, int page, int limit);

    int getGrantPoolMinerCount(long poolId);

    Account.PoolMiner getGrantPoolMiner(long accountId, long poolId);

    void addToPledgeAmount(Account.Pledges pledge, long amountNQT, int pledgeLatestTime);

    void addToUnplegeAmount(Account.Pledges pledge, long amountNQT, int withdrawTime);


    DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId);

    DbIterator<Account.Pledges> getPoolAllMinerPledge(long accountId, int page, int limit);

    int getPoolAllMinerPledgeCount(long accountId);

    DbIterator<Account.Pledges> getPledges(long accountId, int page, int limit);

    int getPledgesCount(long accountId);

    void burningRemanentForgeReward(Block block, long totalReward);

    boolean checkBalance(Block block);
}
