package val.db.store;

import org.jooq.Record14;
import org.jooq.Result;
import val.Account;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedBatchEntityTable;
import val.db.VersionedEntityTable;
import val.schema.tables.records.AccountRecord;

import java.util.List;

/**
 * Interface for Database operations related to Accounts
 */
public interface AccountStore {

    VersionedBatchEntityTable<Account> getAccountTable();

    VersionedEntityTable<Account.RewardRecipientAssignment> getRewardRecipientAssignmentTable();

    DbKey.LongKeyFactory<Account.RewardRecipientAssignment> getRewardRecipientAssignmentKeyFactory();

    DbKey.LinkKeyFactory<Account.AccountAsset> getAccountAssetKeyFactory();

    VersionedEntityTable<Account.AccountAsset> getAccountAssetTable();

    int getAssetAccountsCount(long assetId);

    DbKey.LongKeyFactory<Account> getAccountKeyFactory();

    DbIterator<Account.RewardRecipientAssignment> getAccountsWithRewardRecipient(Long recipientId);

    DbIterator<Account.AccountAsset> getAssets(int from, int to, Long id);

    DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to);

    DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to);

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    boolean setOrVerify(Account acc, byte[] key, int height);

    Result<Record14<Long, Long, Long, Long, String, Byte[], String, Long, Long, Long, Long, Integer, Long, Long>> getAccountPledges(long accountId, int page, int limit);

    int getAccountPledgesCount(long accountId);

    Result<AccountRecord> getAccountRoles(int roleCode, int from, int to);

    List<Long> getTotalBalance(); // balance, unconfirmed_balance, forge_balance

    List<Long> getTotalPledge(); // pledge, unpledge
}
