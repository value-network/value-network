package val;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import val.TransactionType.Payment;
import val.at.AT_Constants;
import val.crypto.EncryptedData;
import val.grpc.proto.VlmApi;
import val.util.Convert;
import val.util.JSON;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.*;

public interface Attachment extends Appendix {

    EmptyAttachment ORDINARY_PAYMENT = new EmptyAttachment() {

        @Override
        String getAppendixName() {
            return "OrdinaryPayment";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.ORDINARY;
        }

    };
    // the message payload is in the Appendix
    EmptyAttachment ARBITRARY_MESSAGE = new EmptyAttachment() {

        @Override
        String getAppendixName() {
            return "ArbitraryMessage";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ARBITRARY_MESSAGE;
        }

    };
    EmptyAttachment AT_PAYMENT = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AutomatedTransactions.AT_PAYMENT;
        }

        @Override
        String getAppendixName() {
            return "AT Payment";
        }


    };
    EmptyAttachment GENESIS_TRANSACTION = new EmptyAttachment() {

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.PAYMENT_TRANSACTIONS_GENESIS;
        }

        @Override
        String getAppendixName() {
            return "Genesis Transaction";
        }


    };

    TransactionType getTransactionType();

    abstract class AbstractAttachment extends AbstractAppendix implements Attachment {

        private AbstractAttachment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        private AbstractAttachment(JsonObject attachmentData) {
            super(attachmentData);
        }

        private AbstractAttachment(byte version) {
            super(version);
        }

        private AbstractAttachment(int blockchainHeight) {
            super(blockchainHeight);
        }

        @Override
        public final void validate(Transaction transaction) throws ValException.ValidationException {
            getTransactionType().validateAttachment(transaction);
        }

        @Override
        public final void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
            getTransactionType().apply(transaction, senderAccount, recipientAccount);
        }

    }

    abstract class EmptyAttachment extends AbstractAttachment {

        private EmptyAttachment() {
            super((byte) 0);
        }

        @Override
        final int getMySize() {
            return 0;
        }

        @Override
        final void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        final void putMyJSON(JsonObject json) {
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(Empty.newBuilder().build());
        }

        @Override
        final boolean verifyVersion(byte transactionVersion) {
            return true;
        }

    }

    final class PaymentMultiOutCreation extends AbstractAttachment {

        private final ArrayList<ArrayList<Long>> recipients = new ArrayList<>();

        PaymentMultiOutCreation(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);

            int numberOfRecipients = Byte.toUnsignedInt(buffer.get());
            HashMap<Long, Boolean> recipientOf = new HashMap<>(numberOfRecipients);

            for (int i = 0; i < numberOfRecipients; ++i) {
                long recipientId = buffer.getLong();
                long amountNQT = buffer.getLong();

                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi out transaction");

                if (amountNQT <= 0)
                    throw new ValException.NotValidException("Insufficient amountNQT on multi out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(new ArrayList<>(Arrays.asList(recipientId, amountNQT)));
            }
            if (recipients.size() > Constants.MAX_MULTI_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException(
                        "Invalid number of recipients listed on multi out transaction");
            }
        }

        PaymentMultiOutCreation(JsonObject attachmentData) throws ValException.NotValidException {
            super(attachmentData);

            JsonArray recipients = JSON.getAsJsonArray(attachmentData.get(RECIPIENTS_PARAMETER));
            HashMap<Long, Boolean> recipientOf = new HashMap<>();

            for (JsonElement recipientObject : recipients) {
                JsonArray recipient = JSON.getAsJsonArray(recipientObject);

                long recipientId = new BigInteger(JSON.getAsString(recipient.get(0))).longValue();
                long amountNQT = JSON.getAsLong(recipient.get(1));
                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi out transaction");

                if (amountNQT <= 0)
                    throw new ValException.NotValidException("Insufficient amountNQT on multi out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(new ArrayList<>(Arrays.asList(recipientId, amountNQT)));
            }
            if (recipients.size() > Constants.MAX_MULTI_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException("Invalid number of recipients listed on multi out transaction");
            }
        }

        public PaymentMultiOutCreation(Collection<Entry<String, Long>> recipients, int blockchainHeight) throws ValException.NotValidException {
            super(blockchainHeight);

            HashMap<Long, Boolean> recipientOf = new HashMap<>();
            for (Entry<String, Long> recipient : recipients) {
                long recipientId = (new BigInteger(recipient.getKey())).longValue();
                long amountNQT = recipient.getValue();
                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi out transaction");

                if (amountNQT <= 0)
                    throw new ValException.NotValidException("Insufficient amountNQT on multi out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(new ArrayList<>(Arrays.asList(recipientId, amountNQT)));
            }
            if (recipients.size() > Constants.MAX_MULTI_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException("Invalid number of recipients listed on multi out transaction");
            }
        }

        @Override
        String getAppendixName() {
            return "MultiOutCreation";
        }

        @Override
        int getMySize() {
            return 1 + recipients.size() * 16;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put((byte) this.recipients.size());
            this.recipients.forEach((a) -> {
                buffer.putLong(a.get(0));
                buffer.putLong(a.get(1));
            });
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            final JsonArray recipientsJSON = new JsonArray();

            this.recipients.stream()
                    .map(recipient -> {
                        final JsonArray recipientJSON = new JsonArray();
                        recipientJSON.add(Convert.toUnsignedLong(recipient.get(0)));
                        recipientJSON.add(recipient.get(1).toString());
                        return recipientJSON;
                    }).forEach(recipientsJSON::add);

            attachment.add(RECIPIENTS_RESPONSE, recipientsJSON);
        }

        @Override
        public TransactionType getTransactionType() {
            return Payment.MULTI_OUT;
        }

        public Long getAmountNQT() {
            long amountNQT = 0;
            for (ArrayList<Long> recipient : recipients) {
                amountNQT = Convert.safeAdd(amountNQT, recipient.get(1));
            }
            return amountNQT;
        }

        public Collection<ArrayList<Long>> getRecipients() {
            return Collections.unmodifiableCollection(recipients);
        }

        @Override
        public Any getProtobufMessage() {
            VlmApi.MultiOutAttachment.Builder builder = VlmApi.MultiOutAttachment.newBuilder()
                    .setVersion(getVersion());
            for (ArrayList<Long> recipient : recipients) {
                builder.addRecipients(VlmApi.MultiOutAttachment.MultiOutRecipient.newBuilder()
                        .setRecipient(recipient.get(0))
                        .setAmount(recipient.get(1)));
            }
            return Any.pack(builder.build());
        }
    }

    final class PaymentMultiSameOutCreation extends AbstractAttachment {

        private final ArrayList<Long> recipients = new ArrayList<>();

        PaymentMultiSameOutCreation(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);

            int numberOfRecipients = Byte.toUnsignedInt(buffer.get());
            HashMap<Long, Boolean> recipientOf = new HashMap<>(numberOfRecipients);

            for (int i = 0; i < numberOfRecipients; ++i) {
                long recipientId = buffer.getLong();

                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi same out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(recipientId);
            }
            if (recipients.size() > Constants.MAX_MULTI_SAME_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException(
                        "Invalid number of recipients listed on multi same out transaction");
            }
        }

        PaymentMultiSameOutCreation(JsonObject attachmentData) throws ValException.NotValidException {
            super(attachmentData);

            JsonArray recipients = JSON.getAsJsonArray(attachmentData.get(RECIPIENTS_PARAMETER));
            HashMap<Long, Boolean> recipientOf = new HashMap<>();

            for (JsonElement recipient : recipients) {
                long recipientId = new BigInteger(JSON.getAsString(recipient)).longValue();
                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi same out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(recipientId);
            }
            if (recipients.size() > Constants.MAX_MULTI_SAME_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException(
                        "Invalid number of recipients listed on multi same out transaction");
            }
        }

        public PaymentMultiSameOutCreation(Collection<Long> recipients, int blockchainHeight) throws ValException.NotValidException {
            super(blockchainHeight);

            HashMap<Long, Boolean> recipientOf = new HashMap<>();
            for (Long recipientId : recipients) {
                if (recipientOf.containsKey(recipientId))
                    throw new ValException.NotValidException("Duplicate recipient on multi same out transaction");

                recipientOf.put(recipientId, true);
                this.recipients.add(recipientId);
            }
            if (recipients.size() > Constants.MAX_MULTI_SAME_OUT_RECIPIENTS || recipients.size() <= 1) {
                throw new ValException.NotValidException(
                        "Invalid number of recipients listed on multi same out transaction");
            }
        }

        @Override
        String getAppendixName() {
            return "MultiSameOutCreation";
        }

        @Override
        int getMySize() {
            return 1 + recipients.size() * 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.put((byte) this.recipients.size());
            this.recipients.forEach(buffer::putLong);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            JsonArray recipients = new JsonArray();
            this.recipients.forEach(a -> recipients.add(Convert.toUnsignedLong(a)));
            attachment.add(RECIPIENTS_RESPONSE, recipients);
        }

        @Override
        public TransactionType getTransactionType() {
            return Payment.MULTI_SAME_OUT;
        }

        public Collection<Long> getRecipients() {
            return Collections.unmodifiableCollection(recipients);
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.MultiOutSameAttachment.newBuilder()
                    .setVersion(getVersion())
                    .addAllRecipients(recipients)
                    .build());
        }
    }

    class PledgeAssignment extends AbstractAttachment {

        private final Long pledgeAmount;
        private final Long pledgeLatestTime;

        PledgeAssignment(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            pledgeAmount = buffer.getLong();
            pledgeLatestTime = buffer.getLong();
        }

        PledgeAssignment(JsonObject attachmentData) {
            super(attachmentData);
            this.pledgeAmount = JSON.getAsLong(attachmentData.get(PLEDEG_AMOUNT_RESPONSE));
            this.pledgeLatestTime = JSON.getAsLong(attachmentData.get(PLEDEG_TIME_RESPONSE));
        }

        public PledgeAssignment(Long pledgeTotal, Long pledgeLatestTime, int blockchainHeight) {
            super(blockchainHeight);
            this.pledgeAmount = pledgeTotal;
            this.pledgeLatestTime = pledgeLatestTime;
        }

        @Override
        String getAppendixName() {
            return "PledgeAssignment";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(pledgeAmount);
            buffer.putLong(pledgeLatestTime);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(PLEDEG_AMOUNT_RESPONSE, pledgeAmount);
            attachment.addProperty(PLEDEG_TIME_RESPONSE, pledgeLatestTime);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE;
        }

        public Long getPledgeAmount() {
            return pledgeAmount;
        }

        public Long getPledgeLatestTime() {
            return pledgeLatestTime;
        }

        @Override
        public Any getProtobufMessage() {
            return null;
        }
    }

    class UnpledgeAssignment extends AbstractAttachment {

        private final Long unpledgeAmount;
        private final Long withdrawTime;

        UnpledgeAssignment(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            unpledgeAmount = buffer.getLong();
            withdrawTime = buffer.getLong();
        }

        UnpledgeAssignment(JsonObject attachmentData) {
            super(attachmentData);
            this.unpledgeAmount = JSON.getAsLong(attachmentData.get(UNPLEDEG_AMOUNT_RESPONSE));
            this.withdrawTime = JSON.getAsLong(attachmentData.get(WITHDRAW_TIME_RESPONSE));
        }

        public UnpledgeAssignment(Long unpledgeAmount, Long withdrawTime, int blockchainHeight) {
            super(blockchainHeight);
            this.unpledgeAmount = unpledgeAmount;
            this.withdrawTime = withdrawTime;
        }

        @Override
        String getAppendixName() {
            return "UnpledgeAssignment";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(unpledgeAmount);
            buffer.putLong(withdrawTime);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(UNPLEDEG_AMOUNT_RESPONSE, unpledgeAmount);
            attachment.addProperty(WITHDRAW_TIME_RESPONSE, withdrawTime);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Payment.PAYMENT_TRANSACTIONS_UNPLEDGE;
        }

        public Long getUnpledgeAmount() {
            return unpledgeAmount;
        }

        public Long getWithdrawTime() {
            return withdrawTime;
        }

        @Override
        public Any getProtobufMessage() {
            // TODO Auto-generated method stub
            return null;
        }
    }



    class MessagingAliasAssignment extends AbstractAttachment {

        private final String aliasName;
        private final String aliasURI;

        MessagingAliasAssignment(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim();
        }

        MessagingAliasAssignment(JsonObject attachmentData) {
            super(attachmentData);
            aliasName = (Convert.nullToEmpty(JSON.getAsString(attachmentData.get(ALIAS_PARAMETER)))).trim();
            aliasURI = (Convert.nullToEmpty(JSON.getAsString(attachmentData.get(URI_PARAMETER)))).trim();
        }

        public MessagingAliasAssignment(String aliasName, String aliasURI, int blockchainHeight) {
            super(blockchainHeight);
            this.aliasName = aliasName.trim();
            this.aliasURI = aliasURI.trim();
        }

        @Override
        String getAppendixName() {
            return "AliasAssignment";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte) alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ALIAS_RESPONSE, aliasName);
            attachment.addProperty(URI_RESPONSE, aliasURI);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_ASSIGNMENT;
        }

        public String getAliasName() {
            return aliasName;
        }

        public String getAliasURI() {
            return aliasURI;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AliasAssignmentAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(aliasName)
                    .setUri(aliasURI)
                    .build());
        }
    }

    class MessagingAliasSell extends AbstractAttachment {

        private final String aliasName;
        private final long priceNQT;

        MessagingAliasSell(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
            this.priceNQT = buffer.getLong();
        }

        MessagingAliasSell(JsonObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(ALIAS_PARAMETER)));
            this.priceNQT = JSON.getAsLong(attachmentData.get(PRICE_NQT_PARAMETER));
        }

        public MessagingAliasSell(String aliasName, long priceNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.aliasName = aliasName;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "AliasSell";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_SELL;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ALIAS_RESPONSE, aliasName);
            attachment.addProperty(PRICE_NQT_RESPONSE, priceNQT);
        }

        public String getAliasName() {
            return aliasName;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AliasSellAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(aliasName)
                    .setPrice(priceNQT)
                    .build());
        }
    }

    final class MessagingAliasBuy extends AbstractAttachment {

        private final String aliasName;

        MessagingAliasBuy(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH);
        }

        MessagingAliasBuy(JsonObject attachmentData) {
            super(attachmentData);
            this.aliasName = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(ALIAS_PARAMETER)));
        }

        public MessagingAliasBuy(String aliasName, int blockchainHeight) {
            super(blockchainHeight);
            this.aliasName = aliasName;
        }

        @Override
        String getAppendixName() {
            return "AliasBuy";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ALIAS_BUY;
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] aliasBytes = Convert.toBytes(aliasName);
            buffer.put((byte) aliasBytes.length);
            buffer.put(aliasBytes);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ALIAS_RESPONSE, aliasName);
        }

        public String getAliasName() {
            return aliasName;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AliasBuyAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(aliasName)
                    .build());
        }
    }

    final class MessagingGlobalPrameter extends AbstractAttachment {

        private final String pledgeRangeMin;
        private final String pledgeRangeMax;
        private final String maxPledgeReward;
        private final String poolMaxCapicity;
        private final String genBlockRatio;
        private final String poolRewardPercent;
        private final String minerRewardPercent;
        private final String poolCount;
        private final String poolerAddressList;

        MessagingGlobalPrameter(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.pledgeRangeMin = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.pledgeRangeMax = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.maxPledgeReward = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.poolMaxCapicity = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.genBlockRatio = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.poolRewardPercent = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.minerRewardPercent = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.poolCount = Convert.readString(buffer, buffer.get(), Constants.MAX_PARAMETER_CHAIN_LENGTH);
            this.poolerAddressList = Convert.readString(buffer, buffer.getInt(), Constants.MAX_PARAMETER_POOLLIST_LENGTH);
        }

        MessagingGlobalPrameter(JsonObject attachmentData) {
            super(attachmentData);
            this.pledgeRangeMin = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_PLEDGE_RANGE_MIN_RESPONSE)));
            this.pledgeRangeMax = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_PLEDGE_RANGE_MAX_RESPONSE)));
            this.maxPledgeReward = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_MAX_PLEDGE_REWARD_RESPONSE)));
            this.poolMaxCapicity = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_POOL_MAX_CAPICITY_RESPONSE)));
            this.genBlockRatio = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_GEN_BLOCK_RATIO_RESPONSE)));
            this.poolRewardPercent = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_POOL_REWARD_PERCENT_RESPONSE)));
            this.minerRewardPercent = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_MINER_REWARD_PERCENT_RESPONSE)));
            this.poolCount = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_POOL_COUNT_RESPONSE)));
            this.poolerAddressList = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(GLOBAL_POOLER_ADDRESS_LIST_RESPONSE)));

        }

        public MessagingGlobalPrameter(String pledgeRangeMin, String pledgeRangeMax, String maxPledgeReward, String poolMaxCapicity,
                                       String genBlockRatio, String poolRewardPercent, String minerRewardPercent, String poolCount, String poolerAddressList, int blockchainHeight) {
            super(blockchainHeight);
            this.pledgeRangeMin = pledgeRangeMin;
            this.pledgeRangeMax = pledgeRangeMax;
            this.maxPledgeReward = maxPledgeReward;
            this.poolMaxCapicity = poolMaxCapicity;
            this.genBlockRatio = genBlockRatio;
            this.poolRewardPercent = poolRewardPercent;
            this.minerRewardPercent = minerRewardPercent;
            this.poolCount = poolCount;
            this.poolerAddressList = poolerAddressList;
        }

        @Override
        String getAppendixName() {
            return "GlobalParameter";
        }

        @Override
        int getMySize() {
            int total = 0;
            total += Convert.isNullorEmpty(pledgeRangeMin) ? 1 : 1 + Convert.toBytes(pledgeRangeMin).length;
            total += Convert.isNullorEmpty(pledgeRangeMax) ? 1 : 1 + Convert.toBytes(pledgeRangeMax).length;
            total += Convert.isNullorEmpty(maxPledgeReward) ? 1 : 1 + Convert.toBytes(maxPledgeReward).length;
            total += Convert.isNullorEmpty(poolMaxCapicity) ? 1 : 1 + Convert.toBytes(poolMaxCapicity).length;
            total += Convert.isNullorEmpty(genBlockRatio) ? 1 : 1 + Convert.toBytes(genBlockRatio).length;
            total += Convert.isNullorEmpty(poolRewardPercent) ? 1 : 1 + Convert.toBytes(poolRewardPercent).length;
            total += Convert.isNullorEmpty(minerRewardPercent) ? 1 : 1 + Convert.toBytes(minerRewardPercent).length;
            total += Convert.isNullorEmpty(poolCount) ? 1 : 1 + Convert.toBytes(poolCount).length;
            total += Convert.isNullorEmpty(poolerAddressList) ? 4 : 4 + Convert.toBytes(poolerAddressList).length;
            return total;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {

            byte[] pledgeRangeMin = Convert.toBytes(this.pledgeRangeMin);
            buffer.put((byte) pledgeRangeMin.length);
            buffer.put(pledgeRangeMin);

            byte[] pledgeRangeMax = Convert.toBytes(this.pledgeRangeMax);
            buffer.put((byte) pledgeRangeMax.length);
            buffer.put(pledgeRangeMax);

            byte[] maxPledgeReward = Convert.toBytes(this.maxPledgeReward);
            buffer.put((byte) maxPledgeReward.length);
            buffer.put(maxPledgeReward);

            byte[] poolMaxCapicity = Convert.toBytes(this.poolMaxCapicity);
            buffer.put((byte) poolMaxCapicity.length);
            buffer.put(poolMaxCapicity);

            byte[] genBlockRatio = Convert.toBytes(this.genBlockRatio);
            buffer.put((byte) genBlockRatio.length);
            buffer.put(genBlockRatio);

            byte[] poolRewardPercent = Convert.toBytes(this.poolRewardPercent);
            buffer.put((byte) poolRewardPercent.length);
            buffer.put(poolRewardPercent);

            byte[] minerRewardPercent = Convert.toBytes(this.minerRewardPercent);
            buffer.put((byte) minerRewardPercent.length);
            buffer.put(minerRewardPercent);

            byte[] poolCount = Convert.toBytes(this.poolCount);
            buffer.put((byte) poolCount.length);
            buffer.put(poolCount);

            byte[] poolerAddressList = Convert.toBytes(this.poolerAddressList);
            buffer.putInt(poolerAddressList.length);
            buffer.put(poolerAddressList);

        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(GLOBAL_PLEDGE_RANGE_MIN_RESPONSE, pledgeRangeMin);
            attachment.addProperty(GLOBAL_PLEDGE_RANGE_MAX_RESPONSE, pledgeRangeMax);
            attachment.addProperty(GLOBAL_MAX_PLEDGE_REWARD_RESPONSE, maxPledgeReward);
            attachment.addProperty(GLOBAL_POOL_MAX_CAPICITY_RESPONSE, poolMaxCapicity);
            attachment.addProperty(GLOBAL_GEN_BLOCK_RATIO_RESPONSE, genBlockRatio);
            attachment.addProperty(GLOBAL_POOL_REWARD_PERCENT_RESPONSE, poolRewardPercent);
            attachment.addProperty(GLOBAL_MINER_REWARD_PERCENT_RESPONSE, minerRewardPercent);
            attachment.addProperty(GLOBAL_POOL_COUNT_RESPONSE, poolCount);
            attachment.addProperty(GLOBAL_POOLER_ADDRESS_LIST_RESPONSE, poolerAddressList);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.GLOBAL_PARAMETER;
        }

        public String getPledgeRangeMin() {
            return pledgeRangeMin;
        }

        public String getPledgeRangeMax() {
            return pledgeRangeMax;
        }

        public String getMaxPledgeReward() {
            return maxPledgeReward;
        }

        public String getPoolMaxCapicity() {
            return poolMaxCapicity;
        }

        public String getGenBlockRatio() {
            return genBlockRatio;
        }

        public String getPoolRewardPercent() {
            return poolRewardPercent;
        }

        public String getPoolCount() {
            return poolCount;
        }

        public String getMinerRewardPercent() {
            return minerRewardPercent;
        }

        public String getPoolerAddressList() {
            return poolerAddressList;
        }

        @Override
        public Any getProtobufMessage() {
            return null;
        }
    }

    final class MessagingAccountInfo extends AbstractAttachment {

        private final String name;
        private final String description;

        MessagingAccountInfo(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ACCOUNT_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ACCOUNT_DESCRIPTION_LENGTH);
        }

        MessagingAccountInfo(JsonObject attachmentData) {
            super(attachmentData);
            this.name = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(NAME_PARAMETER)));
            this.description = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(DESCRIPTION_PARAMETER)));
        }

        public MessagingAccountInfo(String name, String description, int blockchainHeight) {
            super(blockchainHeight);
            this.name = name;
            this.description = description;
        }

        @Override
        String getAppendixName() {
            return "AccountInfo";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] putName = Convert.toBytes(this.name);
            byte[] putDescription = Convert.toBytes(this.description);
            buffer.put((byte) putName.length);
            buffer.put(putName);
            buffer.putShort((short) putDescription.length);
            buffer.put(putDescription);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(NAME_RESPONSE, name);
            attachment.addProperty(DESCRIPTION_RESPONSE, description);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.Messaging.ACCOUNT_INFO;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AccountInfoAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(name)
                    .setDescription(description)
                    .build());
        }
    }

    class ColoredCoinsAssetIssuance extends AbstractAttachment {

        private final String name;
        private final String description;
        private final long quantityQNT;
        private final byte decimals;

        ColoredCoinsAssetIssuance(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_ASSET_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_DESCRIPTION_LENGTH);
            this.quantityQNT = buffer.getLong();
            this.decimals = buffer.get();
        }

        ColoredCoinsAssetIssuance(JsonObject attachmentData) {
            super(attachmentData);
            this.name = JSON.getAsString(attachmentData.get(NAME_PARAMETER));
            this.description = Convert.nullToEmpty(JSON.getAsString(attachmentData.get(DESCRIPTION_PARAMETER)));
            this.quantityQNT = JSON.getAsLong(attachmentData.get(QUANTITY_QNT_PARAMETER));
            this.decimals = JSON.getAsByte(attachmentData.get(DECIMALS_PARAMETER));
        }

        public ColoredCoinsAssetIssuance(String name, String description, long quantityQNT, byte decimals, int blockchainHeight) {
            super(blockchainHeight);
            this.name = name;
            this.description = Convert.nullToEmpty(description);
            this.quantityQNT = quantityQNT;
            this.decimals = decimals;
        }

        @Override
        String getAppendixName() {
            return "AssetIssuance";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 8 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] name = Convert.toBytes(this.name);
            byte[] description = Convert.toBytes(this.description);
            buffer.put((byte) name.length);
            buffer.put(name);
            buffer.putShort((short) description.length);
            buffer.put(description);
            buffer.putLong(quantityQNT);
            buffer.put(decimals);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(NAME_RESPONSE, name);
            attachment.addProperty(DESCRIPTION_RESPONSE, description);
            attachment.addProperty(QUANTITY_QNT_RESPONSE, quantityQNT);
            attachment.addProperty(DECIMALS_RESPONSE, decimals);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_ISSUANCE;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public byte getDecimals() {
            return decimals;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AssetIssuanceAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(name)
                    .setDescription(description)
                    .setQuantity(quantityQNT)
                    .setDecimals(decimals)
                    .build());
        }
    }

    final class ColoredCoinsAssetTransfer extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final String comment;

        ColoredCoinsAssetTransfer(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.comment = getVersion() == 0 ? Convert.readString(buffer, buffer.getShort(), Constants.MAX_ASSET_TRANSFER_COMMENT_LENGTH) : null;
        }

        ColoredCoinsAssetTransfer(JsonObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(ASSET_PARAMETER)));
            this.quantityQNT = JSON.getAsLong(attachmentData.get(QUANTITY_QNT_PARAMETER));
            this.comment = getVersion() == 0 ? Convert.nullToEmpty(JSON.getAsString(attachmentData.get(COMMENT_PARAMETER))) : null;
        }

        public ColoredCoinsAssetTransfer(long assetId, long quantityQNT, int blockchainHeight) {
            super(blockchainHeight);
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.comment = null;
        }

        @Override
        String getAppendixName() {
            return "AssetTransfer";
        }

        @Override
        int getMySize() {
            return 8 + 8 + (getVersion() == 0 ? (2 + Convert.toBytes(comment).length) : 0);
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            if (getVersion() == 0 && comment != null) {
                byte[] commentBytes = Convert.toBytes(this.comment);
                buffer.putShort((short) commentBytes.length);
                buffer.put(commentBytes);
            }
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(assetId));
            attachment.addProperty(QUANTITY_QNT_RESPONSE, quantityQNT);
            if (getVersion() == 0) {
                attachment.addProperty(COMMENT_RESPONSE, comment);
            }
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASSET_TRANSFER;
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public String getComment() {
            return comment;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AssetTransferAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setId(assetId)
                    .setQuantity(quantityQNT)
                    .setComment(comment)
                    .build());
        }
    }

    abstract class ColoredCoinsOrderPlacement extends AbstractAttachment {

        private final long assetId;
        private final long quantityQNT;
        private final long priceNQT;

        private ColoredCoinsOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.assetId = buffer.getLong();
            this.quantityQNT = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        private ColoredCoinsOrderPlacement(JsonObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(ASSET_PARAMETER)));
            this.quantityQNT = JSON.getAsLong(attachmentData.get(QUANTITY_QNT_PARAMETER));
            this.priceNQT = JSON.getAsLong(attachmentData.get(PRICE_NQT_PARAMETER));
        }

        private ColoredCoinsOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.assetId = assetId;
            this.quantityQNT = quantityQNT;
            this.priceNQT = priceNQT;
        }

        @Override
        int getMySize() {
            return 8 + 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
            buffer.putLong(quantityQNT);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(assetId));
            attachment.addProperty(QUANTITY_QNT_RESPONSE, quantityQNT);
            attachment.addProperty(PRICE_NQT_RESPONSE, priceNQT);
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AssetOrderPlacementAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setId(assetId)
                    .setQuantity(quantityQNT)
                    .setPrice(priceNQT)
                    .setType(getType())
                    .build());
        }

        public long getAssetId() {
            return assetId;
        }

        public long getQuantityQNT() {
            return quantityQNT;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        protected abstract VlmApi.AssetOrderPlacementAttachment.Type getType();
    }

    final class ColoredCoinsAskOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsAskOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsAskOrderPlacement(JsonObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
            super(assetId, quantityQNT, priceNQT, blockchainHeight);
        }

        @Override
        protected VlmApi.AssetOrderPlacementAttachment.Type getType() {
            return VlmApi.AssetOrderPlacementAttachment.Type.ASK;
        }

        @Override
        String getAppendixName() {
            return "AskOrderPlacement";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_PLACEMENT;
        }

    }

    final class ColoredCoinsBidOrderPlacement extends ColoredCoinsOrderPlacement {

        ColoredCoinsBidOrderPlacement(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsBidOrderPlacement(JsonObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderPlacement(long assetId, long quantityQNT, long priceNQT, int blockchainHeight) {
            super(assetId, quantityQNT, priceNQT, blockchainHeight);
        }

        @Override
        protected VlmApi.AssetOrderPlacementAttachment.Type getType() {
            return VlmApi.AssetOrderPlacementAttachment.Type.BID;
        }

        @Override
        String getAppendixName() {
            return "BidOrderPlacement";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_PLACEMENT;
        }

    }

    abstract class ColoredCoinsOrderCancellation extends AbstractAttachment {

        private final long orderId;

        private ColoredCoinsOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.orderId = buffer.getLong();
        }

        private ColoredCoinsOrderCancellation(JsonObject attachmentData) {
            super(attachmentData);
            this.orderId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(ORDER_PARAMETER)));
        }

        private ColoredCoinsOrderCancellation(long orderId, int blockchainHeight) {
            super(blockchainHeight);
            this.orderId = orderId;
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(orderId);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ORDER_RESPONSE, Convert.toUnsignedLong(orderId));
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.AssetOrderCancellationAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setOrderId(orderId)
                    .setType(getType())
                    .build());
        }

        public long getOrderId() {
            return orderId;
        }

        protected abstract VlmApi.AssetOrderCancellationAttachment.Type getType();
    }

    final class ColoredCoinsAskOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsAskOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsAskOrderCancellation(JsonObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsAskOrderCancellation(long orderId, int blockchainHeight) {
            super(orderId, blockchainHeight);
        }

        @Override
        protected VlmApi.AssetOrderCancellationAttachment.Type getType() {
            return VlmApi.AssetOrderCancellationAttachment.Type.ASK;
        }

        @Override
        String getAppendixName() {
            return "AskOrderCancellation";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.ASK_ORDER_CANCELLATION;
        }

    }

    final class ColoredCoinsBidOrderCancellation extends ColoredCoinsOrderCancellation {

        ColoredCoinsBidOrderCancellation(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ColoredCoinsBidOrderCancellation(JsonObject attachmentData) {
            super(attachmentData);
        }

        public ColoredCoinsBidOrderCancellation(long orderId, int blockchainHeight) {
            super(orderId, blockchainHeight);
        }

        @Override
        protected VlmApi.AssetOrderCancellationAttachment.Type getType() {
            return VlmApi.AssetOrderCancellationAttachment.Type.BID;
        }

        @Override
        String getAppendixName() {
            return "BidOrderCancellation";
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ColoredCoins.BID_ORDER_CANCELLATION;
        }

    }

    final class DigitalGoodsListing extends AbstractAttachment {

        private final String name;
        private final String description;
        private final String tags;
        private final int quantity;
        private final long priceNQT;

        DigitalGoodsListing(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.name = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_DESCRIPTION_LENGTH);
            this.tags = Convert.readString(buffer, buffer.getShort(), Constants.MAX_DGS_LISTING_TAGS_LENGTH);
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsListing(JsonObject attachmentData) {
            super(attachmentData);
            this.name = JSON.getAsString(attachmentData.get(NAME_RESPONSE));
            this.description = JSON.getAsString(attachmentData.get(DESCRIPTION_RESPONSE));
            this.tags = JSON.getAsString(attachmentData.get(TAGS_RESPONSE));
            this.quantity = JSON.getAsInt(attachmentData.get(QUANTITY_RESPONSE));
            this.priceNQT = JSON.getAsLong(attachmentData.get(PRICE_NQT_PARAMETER));
        }

        public DigitalGoodsListing(String name, String description, String tags, int quantity, long priceNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.name = name;
            this.description = description;
            this.tags = tags;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsListing";
        }

        @Override
        int getMySize() {
            return 2 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + 2
                    + Convert.toBytes(tags).length + 4 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.putShort((short) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);
            byte[] tagsBytes = Convert.toBytes(tags);
            buffer.putShort((short) tagsBytes.length);
            buffer.put(tagsBytes);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(NAME_RESPONSE, name);
            attachment.addProperty(DESCRIPTION_RESPONSE, description);
            attachment.addProperty(TAGS_RESPONSE, tags);
            attachment.addProperty(QUANTITY_RESPONSE, quantity);
            attachment.addProperty(PRICE_NQT_RESPONSE, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.LISTING;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getTags() {
            return tags;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsListingAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(name)
                    .setDescription(description)
                    .setTags(tags)
                    .setQuantity(quantity)
                    .setPrice(priceNQT)
                    .build());
        }
    }

    final class DigitalGoodsDelisting extends AbstractAttachment {

        private final long goodsId;

        DigitalGoodsDelisting(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
        }

        DigitalGoodsDelisting(JsonObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(GOODS_PARAMETER)));
        }

        public DigitalGoodsDelisting(long goodsId, int blockchainHeight) {
            super(blockchainHeight);
            this.goodsId = goodsId;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsDelisting";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(goodsId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELISTING;
        }

        public long getGoodsId() {
            return goodsId;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsDelistingAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setGoodsId(goodsId)
                    .build());
        }
    }

    final class DigitalGoodsPriceChange extends AbstractAttachment {

        private final long goodsId;
        private final long priceNQT;

        DigitalGoodsPriceChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.priceNQT = buffer.getLong();
        }

        DigitalGoodsPriceChange(JsonObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(GOODS_PARAMETER)));
            this.priceNQT = JSON.getAsLong(attachmentData.get(PRICE_NQT_PARAMETER));
        }

        public DigitalGoodsPriceChange(long goodsId, long priceNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.goodsId = goodsId;
            this.priceNQT = priceNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsPriceChange";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putLong(priceNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(goodsId));
            attachment.addProperty(PRICE_NQT_RESPONSE, priceNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PRICE_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsPriceChangeAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setGoodsId(goodsId)
                    .setPrice(priceNQT)
                    .build());
        }
    }

    final class DigitalGoodsQuantityChange extends AbstractAttachment {

        private final long goodsId;
        private final int deltaQuantity;

        DigitalGoodsQuantityChange(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.deltaQuantity = buffer.getInt();
        }

        DigitalGoodsQuantityChange(JsonObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(GOODS_PARAMETER)));
            this.deltaQuantity = JSON.getAsInt(attachmentData.get(DELTA_QUANTITY_PARAMETER));
        }

        public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity, int blockchainHeight) {
            super(blockchainHeight);
            this.goodsId = goodsId;
            this.deltaQuantity = deltaQuantity;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsQuantityChange";
        }

        @Override
        int getMySize() {
            return 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(deltaQuantity);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(goodsId));
            attachment.addProperty(DELTA_QUANTITY_RESPONSE, deltaQuantity);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.QUANTITY_CHANGE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getDeltaQuantity() {
            return deltaQuantity;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsQuantityChangeAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setGoodsId(goodsId)
                    .setDeltaQuantity(deltaQuantity)
                    .build());
        }
    }

    final class DigitalGoodsPurchase extends AbstractAttachment {

        private final long goodsId;
        private final int quantity;
        private final long priceNQT;
        private final int deliveryDeadlineTimestamp;

        DigitalGoodsPurchase(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.goodsId = buffer.getLong();
            this.quantity = buffer.getInt();
            this.priceNQT = buffer.getLong();
            this.deliveryDeadlineTimestamp = buffer.getInt();
        }

        DigitalGoodsPurchase(JsonObject attachmentData) {
            super(attachmentData);
            this.goodsId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(GOODS_PARAMETER)));
            this.quantity = JSON.getAsInt(attachmentData.get(QUANTITY_PARAMETER));
            this.priceNQT = JSON.getAsLong(attachmentData.get(PRICE_NQT_PARAMETER));
            this.deliveryDeadlineTimestamp = JSON.getAsInt(attachmentData.get(DELIVERY_DEADLINE_TIMESTAMP_PARAMETER));
        }

        public DigitalGoodsPurchase(long goodsId, int quantity, long priceNQT, int deliveryDeadlineTimestamp, int blockchainHeight) {
            super(blockchainHeight);
            this.goodsId = goodsId;
            this.quantity = quantity;
            this.priceNQT = priceNQT;
            this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsPurchase";
        }

        @Override
        int getMySize() {
            return 8 + 4 + 8 + 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(goodsId);
            buffer.putInt(quantity);
            buffer.putLong(priceNQT);
            buffer.putInt(deliveryDeadlineTimestamp);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(GOODS_RESPONSE, Convert.toUnsignedLong(goodsId));
            attachment.addProperty(QUANTITY_RESPONSE, quantity);
            attachment.addProperty(PRICE_NQT_RESPONSE, priceNQT);
            attachment.addProperty(DELIVERY_DEADLINE_TIMESTAMP_RESPONSE, deliveryDeadlineTimestamp);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.PURCHASE;
        }

        public long getGoodsId() {
            return goodsId;
        }

        public int getQuantity() {
            return quantity;
        }

        public long getPriceNQT() {
            return priceNQT;
        }

        public int getDeliveryDeadlineTimestamp() {
            return deliveryDeadlineTimestamp;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsPurchaseAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setGoodsId(goodsId)
                    .setQuantity(quantity)
                    .setPrice(priceNQT)
                    .setDeliveryDeadlineTimestmap(deliveryDeadlineTimestamp)
                    .build());
        }
    }

    final class DigitalGoodsDelivery extends AbstractAttachment {

        private final long purchaseId;
        private final EncryptedData goods;
        private final long discountNQT;
        private final boolean goodsIsText;

        DigitalGoodsDelivery(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            int length = buffer.getInt();
            goodsIsText = length < 0;
            if (length < 0) {
                length &= Integer.MAX_VALUE;
            }
            this.goods = EncryptedData.readEncryptedData(buffer, length, Constants.MAX_DGS_GOODS_LENGTH);
            this.discountNQT = buffer.getLong();
        }

        DigitalGoodsDelivery(JsonObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(PURCHASE_PARAMETER)));
            this.goods = new EncryptedData(Convert.parseHexString(JSON.getAsString(attachmentData.get(GOODS_DATA_PARAMETER))),
                    Convert.parseHexString(JSON.getAsString(attachmentData.get(GOODS_NONCE_PARAMETER))));
            this.discountNQT = JSON.getAsLong(attachmentData.get(DISCOUNT_NQT_PARAMETER));
            this.goodsIsText = Boolean.TRUE.equals(JSON.getAsBoolean(attachmentData.get(GOODS_IS_TEXT_PARAMETER)));
        }

        public DigitalGoodsDelivery(long purchaseId, EncryptedData goods, boolean goodsIsText, long discountNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.purchaseId = purchaseId;
            this.goods = goods;
            this.discountNQT = discountNQT;
            this.goodsIsText = goodsIsText;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsDelivery";
        }

        @Override
        int getMySize() {
            return 8 + 4 + goods.getSize() + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putInt(goodsIsText ? goods.getData().length | Integer.MIN_VALUE : goods.getData().length);
            buffer.put(goods.getData());
            buffer.put(goods.getNonce());
            buffer.putLong(discountNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(PURCHASE_RESPONSE, Convert.toUnsignedLong(purchaseId));
            attachment.addProperty(GOODS_DATA_RESPONSE, Convert.toHexString(goods.getData()));
            attachment.addProperty(GOODS_NONCE_RESPONSE, Convert.toHexString(goods.getNonce()));
            attachment.addProperty(DISCOUNT_NQT_RESPONSE, discountNQT);
            attachment.addProperty(GOODS_IS_TEXT_RESPONSE, goodsIsText);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.DELIVERY;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

        public EncryptedData getGoods() {
            return goods;
        }

        public long getDiscountNQT() {
            return discountNQT;
        }

        public boolean goodsIsText() {
            return goodsIsText;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsDeliveryAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setPurchaseId(purchaseId)
                    .setDiscount(discountNQT)
                    .setData(ByteString.copyFrom(goods.getData()))
                    .setNonce(ByteString.copyFrom(goods.getNonce()))
                    .setIsText(goodsIsText)
                    .build());
        }
    }

    final class DigitalGoodsFeedback extends AbstractAttachment {

        private final long purchaseId;

        DigitalGoodsFeedback(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
        }

        DigitalGoodsFeedback(JsonObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(PURCHASE_PARAMETER)));
        }

        public DigitalGoodsFeedback(long purchaseId, int blockchainHeight) {
            super(blockchainHeight);
            this.purchaseId = purchaseId;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsFeedback";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(PURCHASE_RESPONSE, Convert.toUnsignedLong(purchaseId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.FEEDBACK;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsFeedbackAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setPurchaseId(purchaseId)
                    .build());
        }
    }

    final class DigitalGoodsRefund extends AbstractAttachment {

        private final long purchaseId;
        private final long refundNQT;

        DigitalGoodsRefund(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.purchaseId = buffer.getLong();
            this.refundNQT = buffer.getLong();
        }

        DigitalGoodsRefund(JsonObject attachmentData) {
            super(attachmentData);
            this.purchaseId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(PURCHASE_PARAMETER)));
            this.refundNQT = JSON.getAsLong(attachmentData.get(REFUND_NQT_PARAMETER));
        }

        public DigitalGoodsRefund(long purchaseId, long refundNQT, int blockchainHeight) {
            super(blockchainHeight);
            this.purchaseId = purchaseId;
            this.refundNQT = refundNQT;
        }

        @Override
        String getAppendixName() {
            return "DigitalGoodsRefund";
        }

        @Override
        int getMySize() {
            return 8 + 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(purchaseId);
            buffer.putLong(refundNQT);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(PURCHASE_RESPONSE, Convert.toUnsignedLong(purchaseId));
            attachment.addProperty(REFUND_NQT_RESPONSE, refundNQT);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.DigitalGoods.REFUND;
        }

        public long getPurchaseId() {
            return purchaseId;
        }

        public long getRefundNQT() {
            return refundNQT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.DigitalGoodsRefundAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setPurchaseId(purchaseId)
                    .setRefund(refundNQT)
                    .build());
        }
    }

    final class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

        private final short period;

        AccountControlEffectiveBalanceLeasing(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.period = buffer.getShort();
        }

        AccountControlEffectiveBalanceLeasing(JsonObject attachmentData) {
            super(attachmentData);
            this.period = JSON.getAsShort(attachmentData.get(PERIOD_PARAMETER));
        }

        public AccountControlEffectiveBalanceLeasing(short period, int blockchainHeight) {
            super(blockchainHeight);
            this.period = period;
        }

        @Override
        String getAppendixName() {
            return "EffectiveBalanceLeasing";
        }

        @Override
        int getMySize() {
            return 2;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putShort(period);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(PERIOD_RESPONSE, period);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AccountControl.EFFECTIVE_BALANCE_LEASING;
        }

        public short getPeriod() {
            return period;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.EffectiveBalanceLeasingAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setPeriod(period)
                    .build());
        }
    }

    final class ChainMiningRewardRecipientAssignment extends AbstractAttachment {

        ChainMiningRewardRecipientAssignment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
        }

        ChainMiningRewardRecipientAssignment(JsonObject attachmentData) {
            super(attachmentData);
        }

        public ChainMiningRewardRecipientAssignment(int blockchainHeight) {
            super(blockchainHeight);
        }

        @Override
        String getAppendixName() {
            return "RewardRecipientAssignment";
        }

        @Override
        int getMySize() {
            return 0;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
        }

        @Override
        void putMyJSON(JsonObject attachment) {
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.ChainMining.REWARD_RECIPIENT_ASSIGNMENT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.RewardRecipientAssignmentAttachment.newBuilder()
                    .setVersion(getVersion())
                    .build());
        }
    }

    final class AdvancedPaymentEscrowCreation extends AbstractAttachment {

        private final Long amountNQT;
        private final byte requiredSigners;
        private final SortedSet<Long> signers = new TreeSet<>();
        private final int deadline;
        private final Escrow.DecisionType deadlineAction;

        AdvancedPaymentEscrowCreation(ByteBuffer buffer, byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);
            this.amountNQT = buffer.getLong();
            this.deadline = buffer.getInt();
            this.deadlineAction = Escrow.byteToDecision(buffer.get());
            this.requiredSigners = buffer.get();
            byte totalSigners = buffer.get();
            if (totalSigners > 10 || totalSigners <= 0) {
                throw new ValException.NotValidException("Invalid number of signers listed on create escrow transaction");
            }
            for (int i = 0; i < totalSigners; i++) {
                if (!this.signers.add(buffer.getLong())) {
                    throw new ValException.NotValidException("Duplicate signer on escrow creation");
                }
            }
        }

        AdvancedPaymentEscrowCreation(JsonObject attachmentData) throws ValException.NotValidException {
            super(attachmentData);
            this.amountNQT = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(AMOUNT_NQT_PARAMETER)));
            this.deadline = JSON.getAsInt(attachmentData.get(DEADLINE_PARAMETER));
            this.deadlineAction = Escrow.stringToDecision(JSON.getAsString(attachmentData.get(DEADLINE_ACTION_PARAMETER)));
            this.requiredSigners = JSON.getAsByte(attachmentData.get(REQUIRED_SIGNERS_PARAMETER));
            int totalSigners = (JSON.getAsJsonArray(attachmentData.get(SIGNERS_PARAMETER))).size();
            if (totalSigners > 10 || totalSigners <= 0) {
                throw new ValException.NotValidException("Invalid number of signers listed on create escrow transaction");
            }
            JsonArray signersJson = JSON.getAsJsonArray(attachmentData.get(SIGNERS_PARAMETER));
            for (JsonElement aSignersJson : signersJson) {
                this.signers.add(Convert.parseUnsignedLong(JSON.getAsString(aSignersJson)));
            }
            if (this.signers.size() != (JSON.getAsJsonArray(attachmentData.get(SIGNERS_PARAMETER))).size()) {
                throw new ValException.NotValidException("Duplicate signer on escrow creation");
            }
        }

        public AdvancedPaymentEscrowCreation(Long amountNQT, int deadline, Escrow.DecisionType deadlineAction,
                                             int requiredSigners, Collection<Long> signers, int blockchainHeight) throws ValException.NotValidException {
            super(blockchainHeight);
            this.amountNQT = amountNQT;
            this.deadline = deadline;
            this.deadlineAction = deadlineAction;
            this.requiredSigners = (byte) requiredSigners;
            if (signers.size() > 10 || signers.isEmpty()) {
                throw new ValException.NotValidException("Invalid number of signers listed on create escrow transaction");
            }
            this.signers.addAll(signers);
            if (this.signers.size() != signers.size()) {
                throw new ValException.NotValidException("Duplicate signer on escrow creation");
            }
        }

        @Override
        String getAppendixName() {
            return "EscrowCreation";
        }

        @Override
        int getMySize() {
            int size = 8 + 4 + 1 + 1 + 1;
            size += (signers.size() * 8);
            return size;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.amountNQT);
            buffer.putInt(this.deadline);
            buffer.put(Escrow.decisionToByte(this.deadlineAction));
            buffer.put(this.requiredSigners);
            byte totalSigners = (byte) this.signers.size();
            buffer.put(totalSigners);
            this.signers.forEach(buffer::putLong);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(AMOUNT_NQT_RESPONSE, Convert.toUnsignedLong(this.amountNQT));
            attachment.addProperty(DEADLINE_RESPONSE, this.deadline);
            attachment.addProperty(DEADLINE_ACTION_RESPONSE, Escrow.decisionToString(this.deadlineAction));
            attachment.addProperty(REQUIRED_SIGNERS_RESPONSE, (int) this.requiredSigners);
            JsonArray ids = new JsonArray();
            for (Long signer : this.signers) {
                ids.add(Convert.toUnsignedLong(signer));
            }
            attachment.add(SIGNERS_RESPONSE, ids);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.ESCROW_CREATION;
        }

        public Long getAmountNQT() {
            return amountNQT;
        }

        public int getDeadline() {
            return deadline;
        }

        public Escrow.DecisionType getDeadlineAction() {
            return deadlineAction;
        }

        public int getRequiredSigners() {
            return (int) requiredSigners;
        }

        public Collection<Long> getSigners() {
            return Collections.unmodifiableCollection(signers);
        }

        public int getTotalSigners() {
            return signers.size();
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.EscrowCreationAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setAmount(amountNQT)
                    .setRequiredSigners(requiredSigners)
                    .addAllSigners(signers)
                    .setDeadline(deadline)
                    .setDeadlineAction(Escrow.decisionToProtobuf(deadlineAction))
                    .build());
        }
    }

    final class AdvancedPaymentEscrowSign extends AbstractAttachment {

        private final Long escrowId;
        private final Escrow.DecisionType decision;

        AdvancedPaymentEscrowSign(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.escrowId = buffer.getLong();
            this.decision = Escrow.byteToDecision(buffer.get());
        }

        AdvancedPaymentEscrowSign(JsonObject attachmentData) {
            super(attachmentData);
            this.escrowId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(ESCROW_ID_PARAMETER)));
            this.decision = Escrow.stringToDecision(JSON.getAsString(attachmentData.get(DECISION_PARAMETER)));
        }

        public AdvancedPaymentEscrowSign(Long escrowId, Escrow.DecisionType decision, int blockchainHeight) {
            super(blockchainHeight);
            this.escrowId = escrowId;
            this.decision = decision;
        }

        @Override
        String getAppendixName() {
            return "EscrowSign";
        }

        @Override
        int getMySize() {
            return 8 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.escrowId);
            buffer.put(Escrow.decisionToByte(this.decision));
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ESCROW_ID_RESPONSE, Convert.toUnsignedLong(this.escrowId));
            attachment.addProperty(DECISION_RESPONSE, Escrow.decisionToString(this.decision));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.ESCROW_SIGN;
        }

        public Long getEscrowId() {
            return this.escrowId;
        }

        public Escrow.DecisionType getDecision() {
            return this.decision;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.EscrowSignAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setEscrowId(escrowId)
                    .setDecision(Escrow.decisionToProtobuf(decision))
                    .build());
        }
    }

    final class AdvancedPaymentEscrowResult extends AbstractAttachment {

        private final Long escrowId;
        private final Escrow.DecisionType decision;

        AdvancedPaymentEscrowResult(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.escrowId = buffer.getLong();
            this.decision = Escrow.byteToDecision(buffer.get());
        }

        AdvancedPaymentEscrowResult(JsonObject attachmentData) {
            super(attachmentData);
            this.escrowId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(ESCROW_ID_PARAMETER)));
            this.decision = Escrow.stringToDecision(JSON.getAsString(attachmentData.get(DECISION_PARAMETER)));
        }

        public AdvancedPaymentEscrowResult(Long escrowId, Escrow.DecisionType decision, int blockchainHeight) {
            super(blockchainHeight);
            this.escrowId = escrowId;
            this.decision = decision;
        }

        @Override
        String getAppendixName() {
            return "EscrowResult";
        }

        @Override
        int getMySize() {
            return 8 + 1;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.escrowId);
            buffer.put(Escrow.decisionToByte(this.decision));
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(ESCROW_ID_RESPONSE, Convert.toUnsignedLong(this.escrowId));
            attachment.addProperty(DECISION_RESPONSE, Escrow.decisionToString(this.decision));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.ESCROW_RESULT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.EscrowResultAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setEscrowId(2)
                    .setDecision(Escrow.decisionToProtobuf(decision))
                    .build());
        }
    }

    final class AdvancedPaymentSubscriptionSubscribe extends AbstractAttachment {

        private final Integer frequency;

        AdvancedPaymentSubscriptionSubscribe(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.frequency = buffer.getInt();
        }

        AdvancedPaymentSubscriptionSubscribe(JsonObject attachmentData) {
            super(attachmentData);
            this.frequency = JSON.getAsInt(attachmentData.get(FREQUENCY_PARAMETER));
        }

        public AdvancedPaymentSubscriptionSubscribe(int frequency, int blockchainHeight) {
            super(blockchainHeight);
            this.frequency = frequency;
        }

        @Override
        String getAppendixName() {
            return "SubscriptionSubscribe";
        }

        @Override
        int getMySize() {
            return 4;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putInt(this.frequency);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(FREQUENCY_RESPONSE, this.frequency);
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.SUBSCRIPTION_SUBSCRIBE;
        }

        public Integer getFrequency() {
            return this.frequency;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.SubscriptionSubscribeAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setFrequency(frequency)
                    .build());
        }
    }

    final class AdvancedPaymentSubscriptionCancel extends AbstractAttachment {

        private final Long subscriptionId;

        AdvancedPaymentSubscriptionCancel(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.subscriptionId = buffer.getLong();
        }

        AdvancedPaymentSubscriptionCancel(JsonObject attachmentData) {
            super(attachmentData);
            this.subscriptionId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(SUBSCRIPTION_ID_PARAMETER)));
        }

        public AdvancedPaymentSubscriptionCancel(Long subscriptionId, int blockchainHeight) {
            super(blockchainHeight);
            this.subscriptionId = subscriptionId;
        }

        @Override
        String getAppendixName() {
            return "SubscriptionCancel";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(subscriptionId);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(SUBSCRIPTION_ID_RESPONSE, Convert.toUnsignedLong(this.subscriptionId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.SUBSCRIPTION_CANCEL;
        }

        public Long getSubscriptionId() {
            return this.subscriptionId;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.SubscriptionCancelAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setSubscriptionId(subscriptionId)
                    .build());
        }
    }

    final class AdvancedPaymentSubscriptionPayment extends AbstractAttachment {

        private final Long subscriptionId;

        AdvancedPaymentSubscriptionPayment(ByteBuffer buffer, byte transactionVersion) {
            super(buffer, transactionVersion);
            this.subscriptionId = buffer.getLong();
        }

        AdvancedPaymentSubscriptionPayment(JsonObject attachmentData) {
            super(attachmentData);
            this.subscriptionId = Convert.parseUnsignedLong(JSON.getAsString(attachmentData.get(SUBSCRIPTION_ID_PARAMETER)));
        }

        public AdvancedPaymentSubscriptionPayment(Long subscriptionId, int blockchainHeight) {
            super(blockchainHeight);
            this.subscriptionId = subscriptionId;
        }

        @Override
        String getAppendixName() {
            return "SubscriptionPayment";
        }

        @Override
        int getMySize() {
            return 8;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(this.subscriptionId);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(SUBSCRIPTION_ID_RESPONSE, Convert.toUnsignedLong(this.subscriptionId));
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AdvancedPayment.SUBSCRIPTION_PAYMENT;
        }

        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.SubscriptionPaymentAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setSucscriptionId(subscriptionId)
                    .build());
        }
    }

    final class AutomatedTransactionsCreation extends AbstractAttachment {

        private final String name;
        private final String description;
        private final byte[] creationBytes;

        AutomatedTransactionsCreation(ByteBuffer buffer,
                                      byte transactionVersion) throws ValException.NotValidException {
            super(buffer, transactionVersion);

            this.name = Convert.readString(buffer, buffer.get(), Constants.MAX_AUTOMATED_TRANSACTION_NAME_LENGTH);
            this.description = Convert.readString(buffer, buffer.getShort(), Constants.MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH);

            // rest of the parsing is at related; code comes from
            // public AT_Machine_State( byte[] atId, byte[] creator, byte[] creationBytes, int height ) {
            int startPosition = buffer.position();
            buffer.getShort();

            buffer.getShort(); //future: reserved for future needs

            int pageSize = (int) AT_Constants.getInstance().PAGE_SIZE(ValNetwork.getBlockchain().getHeight());
            short codePages = buffer.getShort();
            short dataPages = buffer.getShort();
            buffer.getShort();
            buffer.getShort();

            buffer.getLong();

            int codeLen;
            if (codePages * pageSize < pageSize + 1) {
                codeLen = buffer.get();
                if (codeLen < 0)
                    codeLen += (Byte.MAX_VALUE + 1) * 2;
            } else if (codePages * pageSize < Short.MAX_VALUE + 1) {
                codeLen = buffer.getShort();
                if (codeLen < 0)
                    codeLen += (Short.MAX_VALUE + 1) * 2;
            } else {
                codeLen = buffer.getInt();
            }
            byte[] code = new byte[codeLen];
            buffer.get(code, 0, codeLen);

            int dataLen;
            if (dataPages * pageSize < 257) {
                dataLen = buffer.get();
                if (dataLen < 0)
                    dataLen += (Byte.MAX_VALUE + 1) * 2;
            } else if (dataPages * pageSize < Short.MAX_VALUE + 1) {
                dataLen = buffer.getShort();
                if (dataLen < 0)
                    dataLen += (Short.MAX_VALUE + 1) * 2;
            } else {
                dataLen = buffer.getInt();
            }
            byte[] data = new byte[dataLen];
            buffer.get(data, 0, dataLen);

            int endPosition = buffer.position();
            buffer.position(startPosition);
            byte[] dst = new byte[endPosition - startPosition];
            buffer.get(dst, 0, endPosition - startPosition);
            this.creationBytes = dst;
        }

        AutomatedTransactionsCreation(JsonObject attachmentData) {
            super(attachmentData);

            this.name = JSON.getAsString(attachmentData.get(NAME_PARAMETER));
            this.description = JSON.getAsString(attachmentData.get(DESCRIPTION_PARAMETER));

            this.creationBytes = Convert.parseHexString(JSON.getAsString(attachmentData.get(CREATION_BYTES_PARAMETER)));

        }

        public AutomatedTransactionsCreation(String name, String description, byte[] creationBytes, int blockchainHeight) {
            super(blockchainHeight);
            this.name = name;
            this.description = description;
            this.creationBytes = creationBytes;
        }

        @Override
        public TransactionType getTransactionType() {
            return TransactionType.AutomatedTransactions.AUTOMATED_TRANSACTION_CREATION;
        }

        @Override
        String getAppendixName() {
            return "AutomatedTransactionsCreation";
        }

        @Override
        int getMySize() {
            return 1 + Convert.toBytes(name).length + 2 + Convert.toBytes(description).length + creationBytes.length;
        }

        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] nameBytes = Convert.toBytes(name);
            buffer.put((byte) nameBytes.length);
            buffer.put(nameBytes);
            byte[] descriptionBytes = Convert.toBytes(description);
            buffer.putShort((short) descriptionBytes.length);
            buffer.put(descriptionBytes);

            buffer.put(creationBytes);
        }

        @Override
        void putMyJSON(JsonObject attachment) {
            attachment.addProperty(NAME_RESPONSE, name);
            attachment.addProperty(DESCRIPTION_RESPONSE, description);
            attachment.addProperty(CREATION_BYTES_RESPONSE, Convert.toHexString(creationBytes));
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public byte[] getCreationBytes() {
            return creationBytes;
        }


        @Override
        public Any getProtobufMessage() {
            return Any.pack(VlmApi.ATCreationAttachment.newBuilder()
                    .setVersion(getVersion())
                    .setName(name)
                    .setDescription(description)
                    .setCreationBytes(ByteString.copyFrom(creationBytes))
                    .build());
        }
    }


}
