package val;

import val.db.DbIterator;
import val.db.DbKey;
import val.grpc.proto.VlmApi;

public class Escrow {

    public final Long senderId;
    public final Long recipientId;
    public final Long id;
    public final DbKey dbKey;
    public final Long amountNQT;
    public final int requiredSigners;
    public final int deadline;
    public final DecisionType deadlineAction;

    public Escrow(DbKey dbKey, Account sender,
                  Account recipient,
                  Long id,
                  Long amountNQT,
                  int requiredSigners,
                  int deadline,
                  DecisionType deadlineAction) {
        this.dbKey = dbKey;
        this.senderId = sender.getId();
        this.recipientId = recipient.getId();
        this.id = id;
        this.amountNQT = amountNQT;
        this.requiredSigners = requiredSigners;
        this.deadline = deadline;
        this.deadlineAction = deadlineAction;
    }
    protected Escrow(Long id, Long senderId, Long recipientId, DbKey dbKey, Long amountNQT,
                     int requiredSigners, int deadline, DecisionType deadlineAction) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.id = id;
        this.dbKey = dbKey;
        this.amountNQT = amountNQT;
        this.requiredSigners = requiredSigners;
        this.deadline = deadline;
        this.deadlineAction = deadlineAction;
    }

    public static String decisionToString(DecisionType decision) {
        switch (decision) {
            case UNDECIDED:
                return "undecided";
            case RELEASE:
                return "release";
            case REFUND:
                return "refund";
            case SPLIT:
                return "split";
        }

        return null;
    }

    public static DecisionType stringToDecision(String decision) {
        switch (decision) {
            case "undecided":
                return DecisionType.UNDECIDED;
            case "release":
                return DecisionType.RELEASE;
            case "refund":
                return DecisionType.REFUND;
            case "split":
                return DecisionType.SPLIT;
        }

        return null;
    }

    public static Byte decisionToByte(DecisionType decision) {
        switch (decision) {
            case UNDECIDED:
                return 0;
            case RELEASE:
                return 1;
            case REFUND:
                return 2;
            case SPLIT:
                return 3;
        }

        return null;
    }

    public static DecisionType byteToDecision(Byte decision) {
        switch (decision) {
            case 0:
                return DecisionType.UNDECIDED;
            case 1:
                return DecisionType.RELEASE;
            case 2:
                return DecisionType.REFUND;
            case 3:
                return DecisionType.SPLIT;
        }

        return null;
    }

    public static VlmApi.EscrowDecisionType decisionToProtobuf(DecisionType decision) {
        switch (decision) {
            case UNDECIDED:
                return VlmApi.EscrowDecisionType.UNDECIDED;
            case RELEASE:
                return VlmApi.EscrowDecisionType.RELEASE;
            case REFUND:
                return VlmApi.EscrowDecisionType.REFUND;
            case SPLIT:
                return VlmApi.EscrowDecisionType.SPLIT;
            default:
                return null;
        }
    }

    public static DecisionType protoBufToDecision(VlmApi.EscrowDecisionType decision) {
        switch (decision) {
            case UNDECIDED:
                return DecisionType.UNDECIDED;
            case RELEASE:
                return DecisionType.RELEASE;
            case REFUND:
                return DecisionType.REFUND;
            case SPLIT:
                return DecisionType.SPLIT;
            default:
                return null;
        }
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getAmountNQT() {
        return amountNQT;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public Long getId() {
        return id;
    }

    public int getRequiredSigners() {
        return requiredSigners;
    }

    public DbIterator<Decision> getDecisions() {
        return ValNetwork.getStores().getEscrowStore().getDecisions(id);
    }

    public int getDeadline() {
        return deadline;
    }

    public DecisionType getDeadlineAction() {
        return deadlineAction;
    }

    public enum DecisionType {
        UNDECIDED,
        RELEASE,
        REFUND,
        SPLIT
    }

    public static class Decision {

        public final Long escrowId;
        public final Long accountId;
        public final DbKey dbKey;
        private DecisionType decision;

        public Decision(DbKey dbKey, Long escrowId, Long accountId, DecisionType decision) {
            this.dbKey = dbKey;
            this.escrowId = escrowId;
            this.accountId = accountId;
            this.decision = decision;
        }


        public Long getEscrowId() {
            return this.escrowId;
        }

        public Long getAccountId() {
            return this.accountId;
        }

        public DecisionType getDecision() {
            return this.decision;
        }

        public void setDecision(DecisionType decision) {
            this.decision = decision;
        }
    }
}
