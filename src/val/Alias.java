package val;

import val.db.DbKey;

public class Alias {

    public final DbKey dbKey;
    private final long id;
    private final String aliasName;
    private long accountId;
    private String aliasURI;
    private int timestamp;

    private Alias(DbKey dbKey, long id, long accountId, String aliasName, String aliasURI, int timestamp) {
        this.id = id;
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.aliasName = aliasName;
        this.aliasURI = aliasURI;
        this.timestamp = timestamp;
    }

    protected Alias(long id, long accountId, String aliasName, String aliasURI, int timestamp, DbKey dbKey) {
        this.id = id;
        this.dbKey = dbKey;
        this.accountId = accountId;
        this.aliasName = aliasName;
        this.aliasURI = aliasURI;
        this.timestamp = timestamp;
    }

    public Alias(long aliasId, DbKey dbKey, Transaction transaction, Attachment.MessagingAliasAssignment attachment) {
        this(dbKey, aliasId, transaction.getSenderId(), attachment.getAliasName(), attachment.getAliasURI(),
                transaction.getBlockTimestamp());
    }

    public long getId() {
        return id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getAliasURI() {
        return aliasURI;
    }

    public void setAliasURI(String aliasURI) {
        this.aliasURI = aliasURI;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public static class Offer {

        public final DbKey dbKey;
        private final long aliasId;
        private long priceNQT;
        private long buyerId;

        public Offer(DbKey dbKey, long aliasId, long priceNQT, long buyerId) {
            this.dbKey = dbKey;
            this.priceNQT = priceNQT;
            this.buyerId = buyerId;
            this.aliasId = aliasId;
        }

        protected Offer(long aliasId, long priceNQT, long buyerId, DbKey nxtKey) {
            this.priceNQT = priceNQT;
            this.buyerId = buyerId;
            this.aliasId = aliasId;
            this.dbKey = nxtKey;
        }

        public long getId() {
            return aliasId;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public void setPriceNQT(long priceNQT) {
            this.priceNQT = priceNQT;
        }

        public long getBuyerId() {
            return buyerId;
        }

        public void setBuyerId(long buyerId) {
            this.buyerId = buyerId;
        }
    }

}
