package val.services.impl;

import org.jooq.Condition;
import val.*;
import val.db.DbIterator;
import val.db.DbKey;
import val.db.VersionedEntityTable;
import val.db.store.EscrowStore;
import val.schema.Tables;
import val.services.AccountService;
import val.services.AliasService;
import val.services.EscrowService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class EscrowServiceImpl implements EscrowService {

    private final VersionedEntityTable<Escrow> escrowTable;
    private final DbKey.LongKeyFactory<Escrow> escrowDbKeyFactory;
    private final VersionedEntityTable<Escrow.Decision> decisionTable;
    private final val.db.sql.DbKey.LinkKeyFactory<Escrow.Decision> decisionDbKeyFactory;
    private final EscrowStore escrowStore;
    private final Blockchain blockchain;
    private final AliasService aliasService;
    private final AccountService accountService;
    private final List<Transaction> resultTransactions;
    private final ConcurrentSkipListSet<Long> updatedEscrowIds = new ConcurrentSkipListSet<>();

    public EscrowServiceImpl(EscrowStore escrowStore, Blockchain blockchain, AliasService aliasService, AccountService accountService) {
        this.escrowStore = escrowStore;
        this.escrowTable = escrowStore.getEscrowTable();
        this.escrowDbKeyFactory = escrowStore.getEscrowDbKeyFactory();
        this.decisionTable = escrowStore.getDecisionTable();
        this.decisionDbKeyFactory = escrowStore.getDecisionDbKeyFactory();
        this.resultTransactions = escrowStore.getResultTransactions();
        this.blockchain = blockchain;
        this.aliasService = aliasService;
        this.accountService = accountService;
    }

    private static Condition getUpdateOnBlockClause(final int timestamp) {
        return Tables.ESCROW.DEADLINE.lt(timestamp);
    }

    @Override
    public DbIterator<Escrow> getAllEscrowTransactions() {
        return escrowTable.getAll(0, -1);
    }

    @Override
    public Escrow getEscrowTransaction(Long id) {
        return escrowTable.get(escrowDbKeyFactory.newKey(id));
    }

    @Override
    public Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId) {
        return escrowStore.getEscrowTransactionsByParticipant(accountId);
    }

    @Override
    public boolean isEnabled() {
        if (blockchain.getLastBlock().getHeight() >= Constants.CHAIN_ESCROW_START_BLOCK) {
            return true;
        }

        Alias escrowEnabled = aliasService.getAlias("featureescrow");
        return escrowEnabled != null && escrowEnabled.getAliasURI().equals("enabled");
    }

    @Override
    public void removeEscrowTransaction(Long id) {
        Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(id));
        if (escrow == null) {
            return;
        }
        DbIterator<Escrow.Decision> decisionIt = escrow.getDecisions();

        List<Escrow.Decision> decisions = new ArrayList<>();
        while (decisionIt.hasNext()) {
            Escrow.Decision decision = decisionIt.next();
            decisions.add(decision);
        }

        decisions.forEach(decisionTable::delete);
        escrowTable.delete(escrow);
    }

    @Override
    public void addEscrowTransaction(Account sender, Account recipient, Long id, Long amountNQT, int requiredSigners, Collection<Long> signers, int deadline, Escrow.DecisionType deadlineAction) {
        final DbKey dbKey = escrowDbKeyFactory.newKey(id);
        Escrow newEscrowTransaction = new Escrow(dbKey, sender, recipient, id, amountNQT, requiredSigners, deadline, deadlineAction);
        escrowTable.insert(newEscrowTransaction);
        DbKey senderDbKey = decisionDbKeyFactory.newKey(id, sender.getId());
        Escrow.Decision senderDecision = new Escrow.Decision(senderDbKey, id, sender.getId(), Escrow.DecisionType.UNDECIDED);
        decisionTable.insert(senderDecision);
        DbKey recipientDbKey = decisionDbKeyFactory.newKey(id, recipient.getId());
        Escrow.Decision recipientDecision = new Escrow.Decision(recipientDbKey, id, recipient.getId(), Escrow.DecisionType.UNDECIDED);
        decisionTable.insert(recipientDecision);
        for (Long signer : signers) {
            DbKey signerDbKey = decisionDbKeyFactory.newKey(id, signer);
            Escrow.Decision decision = new Escrow.Decision(signerDbKey, id, signer, Escrow.DecisionType.UNDECIDED);
            decisionTable.insert(decision);
        }
    }

    @Override
    public synchronized void sign(Long id, Escrow.DecisionType decision, Escrow escrow) {
        if (id.equals(escrow.getSenderId()) && decision != Escrow.DecisionType.RELEASE) {
            return;
        }

        if (id.equals(escrow.getRecipientId()) && decision != Escrow.DecisionType.REFUND) {
            return;
        }

        Escrow.Decision decisionChange = decisionTable.get(decisionDbKeyFactory.newKey(escrow.getId(), id));
        if (decisionChange == null) {
            return;
        }
        decisionChange.setDecision(decision);

        decisionTable.insert(decisionChange);

        updatedEscrowIds.add(escrow.getId());
    }

    @Override
    public Escrow.DecisionType checkComplete(Escrow escrow) {
        Escrow.Decision senderDecision = decisionTable.get(decisionDbKeyFactory.newKey(escrow.getId(), escrow.getSenderId()));
        if (senderDecision.getDecision() == Escrow.DecisionType.RELEASE) {
            return Escrow.DecisionType.RELEASE;
        }
        Escrow.Decision recipientDecision = decisionTable.get(decisionDbKeyFactory.newKey(escrow.getId(), escrow.getRecipientId()));
        if (recipientDecision.getDecision() == Escrow.DecisionType.REFUND) {
            return Escrow.DecisionType.REFUND;
        }

        int countRelease = 0;
        int countRefund = 0;
        int countSplit = 0;

        DbIterator<Escrow.Decision> decisions = ValNetwork.getStores().getEscrowStore().getDecisions(escrow.getId());
        while (decisions.hasNext()) {
            Escrow.Decision decision = decisions.next();
            if (decision.getAccountId().equals(escrow.getSenderId()) ||
                    decision.getAccountId().equals(escrow.getRecipientId())) {
                continue;
            }
            switch (decision.getDecision()) {
                case RELEASE:
                    countRelease++;
                    break;
                case REFUND:
                    countRefund++;
                    break;
                case SPLIT:
                    countSplit++;
                    break;
                default:
                    break;
            }
        }

        if (countRelease >= escrow.getRequiredSigners()) {
            return Escrow.DecisionType.RELEASE;
        }
        if (countRefund >= escrow.getRequiredSigners()) {
            return Escrow.DecisionType.REFUND;
        }
        if (countSplit >= escrow.getRequiredSigners()) {
            return Escrow.DecisionType.SPLIT;
        }

        return Escrow.DecisionType.UNDECIDED;
    }

    @Override
    public void updateOnBlock(Block block, int blockchainHeight) {
        resultTransactions.clear();

        DbIterator<Escrow> deadlineEscrows = escrowTable.getManyBy(getUpdateOnBlockClause(block.getTimestamp()), 0, -1);
        while (deadlineEscrows.hasNext()) {
            updatedEscrowIds.add(deadlineEscrows.next().getId());
        }

        if (updatedEscrowIds.size() > 0) {
            for (Long escrowId : updatedEscrowIds) {
                Escrow escrow = escrowTable.get(escrowDbKeyFactory.newKey(escrowId));
                Escrow.DecisionType result = checkComplete(escrow);
                if (result != Escrow.DecisionType.UNDECIDED || escrow.getDeadline() < block.getTimestamp()) {
                    if (result == Escrow.DecisionType.UNDECIDED) {
                        result = escrow.getDeadlineAction();
                    }
                    doPayout(result, block, blockchainHeight, escrow);

                    removeEscrowTransaction(escrowId);
                }
            }
            if (resultTransactions.size() > 0) {
                ValNetwork.getDbs().getTransactionDb().saveTransactions(resultTransactions);
            }
            updatedEscrowIds.clear();
        }
    }

    @Override
    public synchronized void doPayout(Escrow.DecisionType result, Block block, int blockchainHeight, Escrow escrow) {
        switch (result) {
            case RELEASE:
                accountService.addToBalanceAndUnconfirmedBalanceNQT(accountService.getAccount(escrow.getRecipientId()), escrow.getAmountNQT());
                saveResultTransaction(block, escrow.getId(), escrow.getRecipientId(), escrow.getAmountNQT(), Escrow.DecisionType.RELEASE, blockchainHeight);
                break;
            case REFUND:
                accountService.addToBalanceAndUnconfirmedBalanceNQT(accountService.getAccount(escrow.getSenderId()), escrow.getAmountNQT());
                saveResultTransaction(block, escrow.getId(), escrow.getSenderId(), escrow.getAmountNQT(), Escrow.DecisionType.REFUND, blockchainHeight);
                break;
            case SPLIT:
                Long halfAmountNQT = escrow.getAmountNQT() / 2;
                accountService.addToBalanceAndUnconfirmedBalanceNQT(accountService.getAccount(escrow.getRecipientId()), halfAmountNQT);
                accountService.addToBalanceAndUnconfirmedBalanceNQT(accountService.getAccount(escrow.getSenderId()), escrow.getAmountNQT() - halfAmountNQT);
                saveResultTransaction(block, escrow.getId(), escrow.getRecipientId(), halfAmountNQT, Escrow.DecisionType.SPLIT, blockchainHeight);
                saveResultTransaction(block, escrow.getId(), escrow.getSenderId(), escrow.getAmountNQT() - halfAmountNQT, Escrow.DecisionType.SPLIT, blockchainHeight);
                break;
            default: // should never get here
                break;
        }
    }

    @Override
    public boolean isIdSigner(Long id, Escrow escrow) {
        return decisionTable.get(decisionDbKeyFactory.newKey(escrow.getId(), id)) != null;
    }

    @Override
    public void saveResultTransaction(Block block, Long escrowId, Long recipientId, Long amountNQT, Escrow.DecisionType decision, int blockchainHeight) {
        Attachment.AbstractAttachment attachment = new Attachment.AdvancedPaymentEscrowResult(escrowId, decision, blockchainHeight);
        Transaction.Builder builder = new Transaction.Builder((byte) 1, Genesis.getCreatorPublicKey(),
                amountNQT, 0L, block.getTimestamp(), (short) 1440, attachment);
        builder.senderId(0L)
                .recipientId(recipientId)
                .blockId(block.getId())
                .height(block.getHeight())
                .blockTimestamp(block.getTimestamp())
                .ecBlockHeight(0)
                .ecBlockId(0L);

        Transaction transaction;
        try {
            transaction = builder.build();
        } catch (ValException.NotValidException e) {
            throw new RuntimeException(e.toString(), e);
        }

        if (!ValNetwork.getDbs().getTransactionDb().hasTransaction(transaction.getId())) {
            resultTransactions.add(transaction);
        }
    }
}
