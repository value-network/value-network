package val.db.sql;

import org.bouncycastle.util.Strings;
import org.jooq.*;
import val.Transaction;
import val.*;
import val.schema.tables.records.TransactionRecord;
import val.util.Convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static val.schema.Tables.TRANSACTION;

public class SqlTransactionDb implements TransactionDb {

    @Override
    public List<Transaction> findTransactionsByType(byte type, byte subType) {
        try (
                DSLContext ctx = Db.getDSLContext();
                Cursor<TransactionRecord> transactionRecords = ctx.selectFrom(TRANSACTION).
                        where(TRANSACTION.TYPE.eq(type).and(TRANSACTION.SUBTYPE.eq(subType))).orderBy(TRANSACTION.DB_ID).fetchLazy()) {
            List<Transaction> list = new ArrayList<>();
            for (TransactionRecord transactionRecord : transactionRecords) {
                list.add(loadTransaction(transactionRecord));
            }
            return list;
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Load Transaction with type:" + type + ", subType:" + subType + " failed", e);
        }
    }

    @Override
    public Transaction findTransaction(long transactionId) {
        try (DSLContext ctx = Db.getDSLContext()) {
            TransactionRecord transactionRecord = ctx.selectFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)).fetchOne();
            return loadTransaction(transactionRecord);
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, id = " + transactionId + ", does not pass validation!", e);
        }
    }

    @Override
    public Transaction findTransactionByFullHash(String fullHash) {
        try (DSLContext ctx = Db.getDSLContext()) {
            TransactionRecord transactionRecord = ctx.selectFrom(TRANSACTION).where(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(fullHash))).fetchOne();
            return loadTransaction(transactionRecord);
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Transaction already in database, full_hash = " + fullHash + ", does not pass validation!", e);
        }
    }

    @Override
    public List<Long> getTransactionByLikeId(String transactionId) {
        List<Long> allId = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {
//    	Result<Record1<Long>> results = ctx.select(TRANSACTION.ID).from(TRANSACTION).where(TRANSACTION.ID.like(transactionId+"%")).getQuery().fetch();
//    	for (Record1<Long> record:results){
//    		Long transaction = record.getValue(TRANSACTION.ID);
//    		if (transaction!=0){
//    			allId.add(transaction);
//    		}
//    	}
            Result<Record> results = ctx.fetch("select id from transaction where cast(id as unsigned) like '" + transactionId + "%'");
            for (Record record : results) {
                Long transaction = (Long) record.getValue("id");
                if (transaction != 0) {
                    allId.add(transaction);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allId;
    }

    @Override
    public List<Transaction> findTransactionByLikeFullHash(String fullHash) {
        StringBuffer sql = new StringBuffer("select id from transaction where hex(full_hash) like '").append(Strings.toUpperCase(fullHash)).append("%'");
        // System.out.printf("findTransactionByLikeFullHash: sql:%s\n", sql.toString());
        List<Transaction> alltnx = new ArrayList();
        try (DSLContext ctx = Db.getDSLContext()) {

            Result<Record> transactions = ctx.fetch(sql.toString());
            TransactionRecord tr = null;
            Transaction transaction = null;
            for (Record record : transactions) {
                tr = new TransactionRecord();
                long tnxId = (Long) record.getValue("id");
                //System.out.printf("findTransactionByLikeFullHash: id:%s\n", tnxId);
                transaction = findTransaction(tnxId);
                if (transaction != null) {
                    alltnx.add(transaction);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return alltnx;
    }

    @Override
    public boolean hasTransaction(long transactionId) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.fetchExists(ctx.selectFrom(TRANSACTION).where(TRANSACTION.ID.eq(transactionId)));
    }

    @Override
    public boolean hasTransactionByFullHash(String fullHash) {
        DSLContext ctx = Db.getDSLContext();
        return ctx.fetchExists(ctx.selectFrom(TRANSACTION).where(TRANSACTION.FULL_HASH.eq(Convert.parseHexString(fullHash))));
    }

    @Override
    public Transaction loadTransaction(TransactionRecord tr) throws ValException.ValidationException {
        if (tr == null) {
            return null;
        }

        ByteBuffer buffer = null;
        if (tr.getAttachmentBytes() != null) {
            buffer = ByteBuffer.wrap(tr.getAttachmentBytes());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        TransactionType transactionType = TransactionType.findTransactionType(tr.getType(), tr.getSubtype());
        Transaction.Builder builder = new Transaction.Builder(tr.getVersion(), tr.getSenderPublicKey(),
                tr.getAmount(), tr.getFee(), tr.getTimestamp(), tr.getDeadline(),
                transactionType.parseAttachment(buffer, tr.getVersion()))
                .referencedTransactionFullHash(tr.getReferencedTransactionFullhash())
                .signature(tr.getSignature())
                .blockId(tr.getBlockId())
                .height(tr.getHeight())
                .id(tr.getId())
                .senderId(tr.getSenderId())
                .blockTimestamp(tr.getBlockTimestamp())
                .fullHash(tr.getFullHash());
        if (transactionType.hasRecipient()) {
            builder.recipientId(Optional.ofNullable(tr.getRecipientId()).orElse(0L));
        }
        if (tr.getHasMessage()) {
            builder.message(new Appendix.Message(buffer, tr.getVersion()));
        }
        if (tr.getHasEncryptedMessage()) {
            builder.encryptedMessage(new Appendix.EncryptedMessage(buffer, tr.getVersion()));
        }
        if (tr.getHasPublicKeyAnnouncement()) {
            builder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(buffer, tr.getVersion()));
        }
        if (tr.getHasEncrypttoselfMessage()) {
            builder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(buffer, tr.getVersion()));
        }
        if (tr.getVersion() > 0) {
            builder.ecBlockHeight(tr.getEcBlockHeight());
            builder.ecBlockId(Optional.ofNullable(tr.getEcBlockId()).orElse(0L));
        }

        return builder.build();
    }

    @Override
    public Transaction loadTransaction(DSLContext ctx, ResultSet rs) throws ValException.ValidationException {
        // TODO: remove this method once SqlBlockchainStore no longer requires it
        try {

            byte type = rs.getByte("type");
            byte subtype = rs.getByte("subtype");
            int timestamp = rs.getInt("timestamp");
            short deadline = rs.getShort("deadline");
            byte[] senderPublicKey = rs.getBytes("sender_public_key");
            long amountNQT = rs.getLong("amount");
            long feeNQT = rs.getLong("fee");
            byte[] referencedTransactionFullHash = rs.getBytes("referenced_transaction_fullhash");
            int ecBlockHeight = rs.getInt("ec_block_height");
            long ecBlockId = rs.getLong("ec_block_id");
            byte[] signature = rs.getBytes("signature");
            long blockId = rs.getLong("block_id");
            int height = rs.getInt("height");
            long id = rs.getLong("id");
            long senderId = rs.getLong("sender_id");
            byte[] attachmentBytes = rs.getBytes("attachment_bytes");
            int blockTimestamp = rs.getInt("block_timestamp");
            byte[] fullHash = rs.getBytes("full_hash");
            byte version = rs.getByte("version");

            ByteBuffer buffer = null;
            if (attachmentBytes != null) {
                buffer = ByteBuffer.wrap(attachmentBytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            }

            TransactionType transactionType = TransactionType.findTransactionType(type, subtype);
            Transaction.Builder builder = new Transaction.Builder(version, senderPublicKey,
                    amountNQT, feeNQT, timestamp, deadline,
                    transactionType.parseAttachment(buffer, version))
                    .referencedTransactionFullHash(referencedTransactionFullHash)
                    .signature(signature)
                    .blockId(blockId)
                    .height(height)
                    .id(id)
                    .senderId(senderId)
                    .blockTimestamp(blockTimestamp)
                    .fullHash(fullHash);
            if (transactionType.hasRecipient()) {
                long recipientId = rs.getLong("recipient_id");
                if (!rs.wasNull()) {
                    builder.recipientId(recipientId);
                }
            }
            if (rs.getBoolean("has_message")) {
                builder.message(new Appendix.Message(buffer, version));
            }
            if (rs.getBoolean("has_encrypted_message")) {
                builder.encryptedMessage(new Appendix.EncryptedMessage(buffer, version));
            }
            if (rs.getBoolean("has_public_key_announcement")) {
                builder.publicKeyAnnouncement(new Appendix.PublicKeyAnnouncement(buffer, version));
            }
            if (rs.getBoolean("has_encrypttoself_message")) {
                builder.encryptToSelfMessage(new Appendix.EncryptToSelfMessage(buffer, version));
            }
            if (version > 0) {
                builder.ecBlockHeight(ecBlockHeight);
                builder.ecBlockId(ecBlockId);
            }

            return builder.build();

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public List<Transaction> findBlockTransactions(long blockId) {
        try (DSLContext ctx = Db.getDSLContext();
             Cursor<TransactionRecord> transactionRecords = ctx.selectFrom(TRANSACTION).
                     where(TRANSACTION.BLOCK_ID.eq(blockId).and(TRANSACTION.SIGNATURE.isNotNull())).fetchLazy()) {
            List<Transaction> list = new ArrayList<>();
            for (TransactionRecord transactionRecord : transactionRecords) {
                list.add(loadTransaction(transactionRecord));
            }
            return list;
        } catch (ValException.ValidationException e) {
            throw new RuntimeException("Transaction already in database for block_id = " + Convert.toUnsignedLong(blockId)
                    + " does not pass validation!", e);
        }
    }

    private byte[] getAttachmentBytes(Transaction transaction) {
        int bytesLength = 0;
        for (Appendix appendage : transaction.getAppendages()) {
            bytesLength += appendage.getSize();
        }
        if (bytesLength == 0) {
            return null;
        } else {
            ByteBuffer buffer = ByteBuffer.allocate(bytesLength);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            for (Appendix appendage : transaction.getAppendages()) {
                appendage.putBytes(buffer);
            }
            return buffer.array();
        }
    }

    public void saveTransactions(List<Transaction> transactions) {
        if (transactions.size() > 0) {
            try (DSLContext ctx = Db.getDSLContext()) {
                BatchBindStep insertBatch = ctx.batch(
                        ctx.insertInto(TRANSACTION, TRANSACTION.ID, TRANSACTION.DEADLINE,
                                TRANSACTION.SENDER_PUBLIC_KEY, TRANSACTION.RECIPIENT_ID, TRANSACTION.AMOUNT,
                                TRANSACTION.FEE, TRANSACTION.REFERENCED_TRANSACTION_FULLHASH, TRANSACTION.HEIGHT,
                                TRANSACTION.BLOCK_ID, TRANSACTION.SIGNATURE, TRANSACTION.TIMESTAMP,
                                TRANSACTION.TYPE,
                                TRANSACTION.SUBTYPE, TRANSACTION.SENDER_ID, TRANSACTION.ATTACHMENT_BYTES,
                                TRANSACTION.BLOCK_TIMESTAMP, TRANSACTION.FULL_HASH, TRANSACTION.VERSION,
                                TRANSACTION.HAS_MESSAGE, TRANSACTION.HAS_ENCRYPTED_MESSAGE,
                                TRANSACTION.HAS_PUBLIC_KEY_ANNOUNCEMENT, TRANSACTION.HAS_ENCRYPTTOSELF_MESSAGE,
                                TRANSACTION.EC_BLOCK_HEIGHT, TRANSACTION.EC_BLOCK_ID)
                                .values((Long) null, null, null, null, null, null, null, null, null, null, null,
                                        null, null,
                                        null, null, null, null, null, null, null, null, null, null, null));
                for (Transaction transaction : transactions) {
                    insertBatch = insertBatch.bind(
                            transaction.getId(),
                            transaction.getDeadline(),
                            transaction.getSenderPublicKey(),
                            (transaction.getRecipientId() == 0 ? null : transaction.getRecipientId()),
                            transaction.getAmountNQT(),
                            transaction.getFeeNQT(),
                            Convert.parseHexString(transaction.getReferencedTransactionFullHash()),
                            transaction.getHeight(),
                            transaction.getBlockId(),
                            transaction.getSignature(),
                            transaction.getTimestamp(),
                            transaction.getType().getType(),
                            transaction.getType().getSubtype(),
                            transaction.getSenderId(),
                            getAttachmentBytes(transaction),
                            transaction.getBlockTimestamp(),
                            Convert.parseHexString(transaction.getFullHash()),
                            transaction.getVersion(),
                            transaction.getMessage() != null,
                            transaction.getEncryptedMessage() != null,
                            transaction.getPublicKeyAnnouncement() != null,
                            transaction.getEncryptToSelfMessage() != null,
                            transaction.getECBlockHeight(),
                            (transaction.getECBlockId() != 0 ? transaction.getECBlockId() : null)
                    );
                }
                insertBatch.execute();
            }
        }
    }
}
