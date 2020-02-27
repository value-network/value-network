package val;

import val.db.DbKey;

public class AssetTransfer {

    public final DbKey dbKey;
    private final long id;
    private final long assetId;
    private final int height;
    private final long senderId;
    private final long recipientId;
    private final long quantityQNT;
    private final int timestamp;
    public AssetTransfer(DbKey dbKey, Transaction transaction, Attachment.ColoredCoinsAssetTransfer attachment) {
        this.dbKey = dbKey;
        this.id = transaction.getId();
        this.height = transaction.getHeight();
        this.assetId = attachment.getAssetId();
        this.senderId = transaction.getSenderId();
        this.recipientId = transaction.getRecipientId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.timestamp = transaction.getBlockTimestamp();
    }

    protected AssetTransfer(long id, DbKey dbKey, long assetId, int height, long senderId, long recipientId, long quantityQNT, int timestamp) {
        this.id = id;
        this.dbKey = dbKey;
        this.assetId = assetId;
        this.height = height;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.quantityQNT = quantityQNT;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getSenderId() {
        return senderId;
    }

    public long getRecipientId() {
        return recipientId;
    }

    public long getQuantityQNT() {
        return quantityQNT;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getHeight() {
        return height;
    }

    public enum Event {
        ASSET_TRANSFER
    }

}
