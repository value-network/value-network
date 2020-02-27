package val;

import val.db.DbKey;


public class GlobalParameter {
    public final DbKey dbKey;
    public long id;
    public long transactionId;
    public String value;
    private int height;
    private int latest;


    public GlobalParameter(DbKey dbKey, long id, long transactionId, String value, int height, int latest) {
        super();
        this.id = id;
        this.dbKey = dbKey;
        this.transactionId = transactionId;
        this.value = value;
        this.height = height;
        this.latest = latest;
    }


    public long getId() {
        return id;
    }


    public void setId(long id) {
        this.id = id;
    }


    public long getTransactionId() {
        return transactionId;
    }


    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }


    public int getHeight() {
        return height;
    }


    public void setHeight(int height) {
        this.height = height;
    }


    public int getLatest() {
        return latest;
    }


    public void setLatest(int latest) {
        this.latest = latest;
    }


    public DbKey getDbKey() {
        return dbKey;
    }

    @Override
    public String toString() {
        return "GlobalParameter [dbKey=" + dbKey + ", id=" + id + ", transactionId=" + transactionId + ", value=" + value
                + ", height=" + height + ", latest=" + latest + "]";
    }


}


