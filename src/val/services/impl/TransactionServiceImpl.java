package val.services.impl;

import val.*;
import val.services.AccountService;
import val.services.TransactionService;

import java.util.Arrays;

public class TransactionServiceImpl implements TransactionService {

    private final AccountService accountService;
    private final Blockchain blockchain;

    public TransactionServiceImpl(AccountService accountService, Blockchain blockchain) {
        this.accountService = accountService;
        this.blockchain = blockchain;
    }

    @Override
    public boolean verifyPublicKey(Transaction transaction) {

        // all 0 public key is reserved
        if (blockchain.getHeight() > 0 && Arrays.equals(Genesis.getCreatorPublicKey(), transaction.getSenderPublicKey())) {
            return false;
        }

        Account account = accountService.getAccount(transaction.getSenderId());
//    System.out.printf("transaction.getSenderId():%s, transaction.getSignature(): %s\n", transaction.getSenderId(),transaction.getSignature());
//    System.out.printf("transaction.getSenderPublicKey(): %s,transaction.getHeight() :%s ",transaction.getSenderPublicKey(),transaction.getHeight());
        if (account == null) {
            System.out.println("account is null");
            return false;
        }
        if (transaction.getSignature() == null) {
            return false;
        }
        return account.setOrVerify(transaction.getSenderPublicKey(), transaction.getHeight());
    }

    @Override
    public void validate(Transaction transaction) throws ValException.ValidationException {

        // accountID == 0 is reserved Genesis.CREATOR_ID can't be the sender
        if (blockchain.getHeight() > 0 && Genesis.CREATOR_ID == transaction.getSenderId()) {
            throw new ValException.NotCurrentlyValidException(String.format("Transaction sender ID can't be %d at height %d",
                    transaction.getSenderId(), blockchain.getHeight()));
        }

        for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
            appendage.validate(transaction);
        }

        // System.out.printf("after attachment validated");
        long minimumFeeNQT = transaction.getType().minimumFeeNQT(blockchain.getHeight(), transaction.getAppendagesSize());

        // System.out.printf("transaction.getFeeNQT() : %s, inimumFeeNQT: %s\n", transaction.getFeeNQT() ,minimumFeeNQT);
        if (transaction.getFeeNQT() < minimumFeeNQT) {
            throw new ValException.NotCurrentlyValidException(String.format("Transaction fee %d less than minimum fee %d at height %d",
                    transaction.getFeeNQT(), minimumFeeNQT, blockchain.getHeight()));
        }
        if (blockchain.getHeight() >= Constants.PUBLIC_KEY_ANNOUNCEMENT_BLOCK) {
            if (transaction.getType().hasRecipient() && transaction.getRecipientId() != 0) {
                Account recipientAccount = accountService.getAccount(transaction.getRecipientId());
                if ((recipientAccount == null || recipientAccount.getPublicKey() == null) && transaction.getPublicKeyAnnouncement() == null) {
                    throw new ValException.NotCurrentlyValidException("Recipient account does not have a public key, must attach a public key announcement");
                }
            }
        }
    }

    @Override
    public boolean applyUnconfirmed(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        return senderAccount != null && transaction.getType().applyUnconfirmed(transaction, senderAccount);
    }

    @Override
    public void apply(Transaction transaction) {
        Account senderAccount = accountService.getAccount(transaction.getSenderId());
        if (transaction.getHeight() == -1) {
            senderAccount = accountService.getOrAddAccount(transaction.getSenderId());
            senderAccount.setBalanceNQT(Genesis.TNX_AMOUNT_NQT);
        }
        senderAccount.apply(transaction.getSenderPublicKey(), transaction.getHeight());
        Account recipientAccount = accountService.getOrAddAccount(transaction.getRecipientId());
        for (Appendix.AbstractAppendix appendage : transaction.getAppendages()) {
            appendage.apply(transaction, senderAccount, recipientAccount);
        }
    }

    @Override
    public void undoUnconfirmed(Transaction transaction) {
        final Account senderAccount = accountService.getAccount(transaction.getSenderId());
        transaction.getType().undoUnconfirmed(transaction, senderAccount);
    }

}
