package val;

import val.crypto.Crypto;
import val.crypto.EncryptedData;
import val.db.DbKey;
import val.db.VersionedBatchEntityTable;
import val.util.Convert;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Account {

    private static final Logger logger = Logger.getLogger(Account.class.getSimpleName());

    public final long id;
    public final DbKey nxtKey;
    private final int creationHeight;
    protected long balanceNQT;
    protected long unconfirmedBalanceNQT;
    protected long forgedBalanceNQT;
    protected long totalPledged;
    protected long pledgeRewardBalance;
    protected int accountRole;
    protected String name;
    protected String description;
    private byte[] publicKey;
    private int keyHeight;

    public Account(long id) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            logger.log(Level.INFO, "CRITICAL ERROR: Reed-Solomon encoding fails for {0}", id);
        }
        this.id = id;
        this.nxtKey = accountDbKeyFactory().newKey(this.id);
        this.creationHeight = ValNetwork.getBlockchain().getHeight();
    }

    protected Account(long id, DbKey dbKey, int creationHeight) {
        if (id != Crypto.rsDecode(Crypto.rsEncode(id))) {
            logger.log(Level.INFO, "CRITICAL ERROR: Reed-Solomon encoding fails for {0}", id);
        }
        this.id = id;
        this.nxtKey = dbKey;
        this.creationHeight = creationHeight;
    }

    private static DbKey.LongKeyFactory<Account> accountDbKeyFactory() {
        return ValNetwork.getStores().getAccountStore().getAccountKeyFactory();
    }

    private static VersionedBatchEntityTable<Account> accountTable() {
        return ValNetwork.getStores().getAccountStore().getAccountTable();
    }

    public static Account getAccount(long id) {
        return id == 0 ? null : accountTable().get(accountDbKeyFactory().newKey(id));
    }

    public static long getId(byte[] publicKey) {
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        return Convert.fullHashToId(publicKeyHash);
    }

    static Account getOrAddAccount(long id) {
        Account account = getAccount(id);
        if (account == null) {
            account = new Account(id);
            accountTable().insert(account);
        }
        return account;
    }

    public static EncryptedData encryptTo(byte[] data, String senderSecretPhrase, byte[] publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("public key required");
        }
        return EncryptedData.encrypt(data, Crypto.getPrivateKey(senderSecretPhrase), publicKey);
    }

    private static void checkBalance(long accountId, long confirmed, long unconfirmed) {
        if (confirmed < 0) {
            throw new DoubleSpendingException("Negative balance or quantity ("
                    + confirmed
                    + ") for account "
                    + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed < 0) {
            throw new DoubleSpendingException("Negative unconfirmed balance or quantity ("
                    + unconfirmed
                    + ") for account "
                    + Convert.toUnsignedLong(accountId));
        }
        if (unconfirmed > confirmed) {
            throw new DoubleSpendingException("Unconfirmed ("
                    + unconfirmed
                    + ") exceeds confirmed ("
                    + confirmed
                    + ") balance or quantity for account "
                    + Convert.toUnsignedLong(accountId));
        }
    }

    public int getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(int accountRole) {
        this.accountRole = accountRole;
    }

    public long getPledgeRewardBalance() {
        return pledgeRewardBalance;
    }

    public void setPledgeRewardBalance(long pledgeRewardBalance) {
        this.pledgeRewardBalance = pledgeRewardBalance;
    }

    public long getTotalPledged() {
        return totalPledged;
    }

    public void setTotalPledged(long totalPledged) {
        this.totalPledged = totalPledged;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getPublicKey() {
        if (this.keyHeight == -1) {
            return null;
        }
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public int getCreationHeight() {
        return creationHeight;
    }

    public int getKeyHeight() {
        return keyHeight;
    }

    public void setKeyHeight(int keyHeight) {
        this.keyHeight = keyHeight;
    }

    public EncryptedData encryptTo(byte[] data, String senderSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Recipient account doesn't have a public key set");
        }
        return EncryptedData.encrypt(data, Crypto.getPrivateKey(senderSecretPhrase), publicKey);
    }

    public byte[] decryptFrom(EncryptedData encryptedData, String recipientSecretPhrase) {
        if (getPublicKey() == null) {
            throw new IllegalArgumentException("Sender account doesn't have a public key set");
        }
        return encryptedData.decrypt(Crypto.getPrivateKey(recipientSecretPhrase), publicKey);
    }

    public long getBalanceNQT() {
        return balanceNQT;
    }

    public void setBalanceNQT(long balanceNQT) {
        this.balanceNQT = balanceNQT;
    }

    public long getUnconfirmedBalanceNQT() {
        return unconfirmedBalanceNQT;
    }

    public void setUnconfirmedBalanceNQT(long unconfirmedBalanceNQT) {
        this.unconfirmedBalanceNQT = unconfirmedBalanceNQT;
    }

    public long getForgedBalanceNQT() {
        return forgedBalanceNQT;
    }

    public void setForgedBalanceNQT(long forgedBalanceNQT) {
        this.forgedBalanceNQT = forgedBalanceNQT;
    }

    // returns true iff:
    // this.publicKey is set to null (in which case this.publicKey also gets set to key)
    // or
    // this.publicKey is already set to an array equal to key
    public boolean setOrVerify(byte[] key, int height) {
        return ValNetwork.getStores().getAccountStore().setOrVerify(this, key, height);
    }

    public void apply(byte[] key, int height) {
        if (!setOrVerify(key, this.creationHeight)) {
            throw new IllegalStateException("Public key mismatch");
        }
        if (this.publicKey == null) {
            throw new IllegalStateException("Public key has not been set for account " + Convert.toUnsignedLong(id)
                    + " at height " + height + ", key height is " + keyHeight);
        }
        if (this.keyHeight == -1 || this.keyHeight > height) {
            this.keyHeight = height;
            accountTable().insert(this);
        }
    }

    public void checkBalance() {
        checkBalance(this.id, this.balanceNQT, this.unconfirmedBalanceNQT);
    }

    public enum Event {
        BALANCE, UNCONFIRMED_BALANCE, ASSET_BALANCE, UNCONFIRMED_ASSET_BALANCE,
        LEASE_SCHEDULED, LEASE_STARTED, LEASE_ENDED, PLEDGE, UNPLEDGE, WITHDRAW,

    }

    public static class AccountAsset {
        public final long accountId;
        public final long assetId;
        public final DbKey dbKey;
        private long quantityQNT;
        private long unconfirmedQuantityQNT;

        protected AccountAsset(long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT, DbKey dbKey) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
            this.dbKey = dbKey;
        }

        public AccountAsset(DbKey dbKey, long accountId, long assetId, long quantityQNT, long unconfirmedQuantityQNT) {
            this.accountId = accountId;
            this.assetId = assetId;
            this.dbKey = dbKey;
            this.quantityQNT = quantityQNT;
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        public long getAccountId() {
            return accountId;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public void setQuantityQNT(long quantityQNT) {
            this.quantityQNT = quantityQNT;
        }

        public long getUnconfirmedQuantityQNT() {
            return unconfirmedQuantityQNT;
        }

        public void setUnconfirmedQuantityQNT(long unconfirmedQuantityQNT) {
            this.unconfirmedQuantityQNT = unconfirmedQuantityQNT;
        }

        public void checkBalance() {
            Account.checkBalance(this.accountId, this.quantityQNT, this.unconfirmedQuantityQNT);
        }

        @Override
        public String toString() {
            return "AccountAsset account_id: "
                    + Convert.toUnsignedLong(accountId)
                    + " asset_id: "
                    + Convert.toUnsignedLong(assetId)
                    + " quantity: "
                    + quantityQNT
                    + " unconfirmedQuantity: "
                    + unconfirmedQuantityQNT;
        }
    }

    public static class Pledges {
        public final DbKey dbKey;
        public long id;
        public long accountID;
        public long recipID;
        protected long pledgeTotal;
        protected long unpledgeTotal;
        private long pledgeLatestTime;
        private long withdrawTime;
        private int height;
        private int latest;


        public Pledges(DbKey dbKey, long id, long accountID, long recipID, long pledgeTotal,
                       long pledgeLatestTime, long unpledgeTotal, long withdrawTime, int height, int latest) {
            super();

            this.dbKey = dbKey;
            this.id = id;
            this.accountID = accountID;
            this.recipID = recipID;
            this.pledgeTotal = pledgeTotal;
            this.pledgeLatestTime = pledgeLatestTime;
            this.unpledgeTotal = unpledgeTotal;
            this.withdrawTime = withdrawTime;
            this.height = height;
            this.latest = latest;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public long getAccountID() {
            return accountID;
        }

        public void setAccountID(long accountID) {
            this.accountID = accountID;
        }

        public long getRecipID() {
            return recipID;
        }

        public void setRecipID(long recipID) {
            this.recipID = recipID;
        }

        public long getPledgeTotal() {
            return pledgeTotal;
        }

        public void setPledgeTotal(long pledgeTotal) {
            this.pledgeTotal = pledgeTotal;
        }

        public long getPledgeLatestTime() {
            return pledgeLatestTime;
        }

        public void setPledgeLatestTime(long pledgeLatestTime) {
            this.pledgeLatestTime = pledgeLatestTime;
        }

        public DbKey getDbKey() {
            return dbKey;
        }

        public int getLatest() {
            return latest;
        }

        public void setLatest(int latest) {
            this.latest = latest;
        }

        public long getUnpledgeTotal() {
            return unpledgeTotal;
        }

        public void setUnpledgeTotal(long unpledgeTotal) {
            this.unpledgeTotal = unpledgeTotal;
        }

        public long getWithdrawTime() {
            return withdrawTime;
        }

        public void setWithdrawTime(long withdrawTime) {
            this.withdrawTime = withdrawTime;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        @Override
        public String toString() {
            return "Pledges [dbKey=" + dbKey + ", accountID=" + accountID + ", recipID=" + recipID + ", height=" + height
                    + ", pledgeTotal=" + pledgeTotal + ", pledgeLatestTime=" + pledgeLatestTime + ", unpledgeTotal="
                    + unpledgeTotal + ", withdrawTime=" + withdrawTime + ", latest=" + latest + "]";
        }


    }

    public static class PoolMiner {
        public final int dbKey;
        public long accountID;
        public long poolID;
        private int status;
        private long cTime;
        private long mTime;


        public PoolMiner(int dbKey, long accountID, long poolID, int status, long cTime, long mTime) {
            super();
            this.dbKey = dbKey;
            this.accountID = accountID;
            this.poolID = poolID;
            this.status = status;
            this.cTime = cTime;
            this.mTime = mTime;
        }

        public long getAccountID() {
            return accountID;
        }

        public void setAccountID(long accountID) {
            this.accountID = accountID;
        }

        public long getPoolID() {
            return poolID;
        }

        public void setPoolID(long poolID) {
            this.poolID = poolID;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public long getcTime() {
            return cTime;
        }

        public void setcTime(long cTime) {
            this.cTime = cTime;
        }

        public long getmTime() {
            return mTime;
        }

        public void setmTime(long mTime) {
            this.mTime = mTime;
        }

        public int getDbKey() {
            return dbKey;
        }

        @Override
        public String toString() {
            return "PoolMiner [dbKey=" + dbKey + ", accountID=" + accountID + ", poolID=" + poolID + ", status=" + status
                    + ", cTime=" + cTime + ", mTime=" + mTime + "]";
        }

    }

    public static class RewardRecipientAssignment {
        public final Long accountId;
        public final DbKey dbKey;
        private Long prevRecipientId;
        private Long recipientId;
        private int fromHeight;

        public RewardRecipientAssignment(Long accountId, Long prevRecipientId, Long recipientId, int fromHeight, DbKey dbKey) {
            this.accountId = accountId;
            this.prevRecipientId = prevRecipientId;
            this.recipientId = recipientId;
            this.fromHeight = fromHeight;
            this.dbKey = dbKey;
        }

        public long getAccountId() {
            return accountId;
        }

        public long getPrevRecipientId() {
            return prevRecipientId;
        }

        public long getRecipientId() {
            return recipientId;
        }

        public int getFromHeight() {
            return fromHeight;
        }

        public void setRecipient(long newRecipientId, int fromHeight) {
            prevRecipientId = recipientId;
            recipientId = newRecipientId;
            this.fromHeight = fromHeight;
        }
    }

    static class DoubleSpendingException extends RuntimeException {

        DoubleSpendingException(String message) {
            super(message);
        }

    }

}
