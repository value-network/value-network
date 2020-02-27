package val.services;

import val.Account;
import val.Block;
import val.Escrow;
import val.db.DbIterator;

import java.util.Collection;

public interface EscrowService {

    DbIterator<Escrow> getAllEscrowTransactions();

    Escrow getEscrowTransaction(Long id);

    Collection<Escrow> getEscrowTransactionsByParticipant(Long accountId);

    boolean isEnabled();

    void removeEscrowTransaction(Long id);

    void updateOnBlock(Block block, int blockchainHeight);

    void addEscrowTransaction(Account sender, Account recipient, Long id, Long amountNQT, int requiredSigners, Collection<Long> signers, int deadline, Escrow.DecisionType deadlineAction);

    void sign(Long id, Escrow.DecisionType decision, Escrow escrow);

    Escrow.DecisionType checkComplete(Escrow escrow);

    void doPayout(Escrow.DecisionType result, Block block, int blockchainHeight, Escrow escrow);

    boolean isIdSigner(Long id, Escrow escrow);

    void saveResultTransaction(Block block, Long escrowId, Long recipientId, Long amountNQT, Escrow.DecisionType decision, int blockchainHeight);
}
