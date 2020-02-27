package val.unconfirmedtransactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.Account;
import val.Transaction;
import val.TransactionType;
import val.ValException;
import val.db.store.AccountStore;
import val.db.store.PledgeStore;
import val.util.Convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class ReservedBalanceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservedBalanceCache.class);

    private final AccountStore accountStore;
    private final PledgeStore pledgeStore;

    private final HashMap<Long, Long> reservedBalanceCache;
    private final HashMap<Long, Long> reservedPledgeCache;
    private final HashMap<Long, Long> reservedUnpledgeCache;
    private final HashMap<Long, Long> reservedPledgeAccountCache;

    public ReservedBalanceCache(AccountStore accountStore, PledgeStore pledgeStore) {
        this.accountStore = accountStore;
        this.pledgeStore = pledgeStore;

        this.reservedBalanceCache = new HashMap<>();
        this.reservedPledgeCache = new HashMap<>();// account - > pledgeTotal
        this.reservedUnpledgeCache = new HashMap<>();// account - > // unpledgeTotal
        this.reservedPledgeAccountCache = new HashMap<>(); //pledge account -> pool account
    }

    void reserveBalanceAndPut(Transaction transaction) throws ValException.ValidationException {
        Account senderAccount = null;
        Account.Pledges pledgeAccount = null;

        if (transaction.getSenderId() != 0) {
            senderAccount = accountStore.getAccountTable()
                    .get(accountStore.getAccountKeyFactory().newKey(transaction.getSenderId()));
            pledgeAccount = pledgeStore.getPledgesTable().get(pledgeStore.getPledgeKeyFactory().newKey(transaction.getSenderId()));
            //System.out.printf("pledge account:%s,pledgeTotal:%s,unpledgeTotal:%s", pledgeAccount.getAccountID(),pledgeAccount.getPledgeTotal(),pledgeAccount.getUnpledgeTotal());
        }
//		System.out.printf("getType():%s, getsubtyepe():%s", transaction.getType().getType(),transaction.getType().getSubtype());
//		System.out.printf("type():%s, subtyepe():%s", TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getType(),TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getSubtype());
        if (transaction.getType().getType() == TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getType() && transaction.getType().getSubtype() == TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getSubtype()) {
            Long recepientCache = reservedPledgeAccountCache.getOrDefault(transaction.getSenderId(), 0L);
            if (recepientCache != 0 && transaction.getRecipientId() != recepientCache) {
                LOGGER.info(String.format("DO NOT pledge more than one pool Account at same time. Transaction %s, sender: %s: want to pledge to: %s, but already pledge to : %s, ",
                        transaction.getId(), transaction.getSenderId(), transaction.getRecipientId(), recepientCache));

                throw new ValException.NotCurrentlyValidException("DO NOT pledge more than one pool Account at same time.");
            }
            reservedPledgeAccountCache.put(transaction.getSenderId(), transaction.getRecipientId());
            verifyBalanceAndPut(transaction, senderAccount, reservedBalanceCache, reservedPledgeCache);
        } else if (transaction.getType().getType() == TransactionType.Payment.PAYMENT_TRANSACTIONS_UNPLEDGE.getType() && transaction.getType().getSubtype() == TransactionType.Payment.PAYMENT_TRANSACTIONS_UNPLEDGE.getSubtype()) {
            if (pledgeAccount == null) {
                LOGGER.info(String.format(
                        "Transaction %d: Account %d has no pledge balance",
                        transaction.getId(), transaction.getSenderId()));

                throw new ValException.NotCurrentlyValidException("Pledge Account unknown");
            }
            this.reservedPledgeAccountCache.remove(transaction.getSenderId());
            verifyPledgeAndPut(transaction, senderAccount, pledgeAccount.getPledgeTotal(), reservedUnpledgeCache);
        }  else {
            verifyBalanceAndPut(transaction, senderAccount, reservedPledgeCache, reservedBalanceCache);
        }
    }

    void verifyPledgeAndPut(Transaction transaction, Account senderAccount, Long verifyAmount, HashMap<Long, Long> CacheMap) throws ValException.ValidationException {
        final Long amountNQT = Convert.safeAdd(CacheMap.getOrDefault(transaction.getSenderId(), 0L),
                transaction.getType().calculateTotalAmountNQT(transaction)) - transaction.getFeeNQT();
        System.out.printf("amount:%s, verifyAmount:%s", amountNQT, verifyAmount);
        if (senderAccount == null) {
            LOGGER.info(String.format(
                    "Transaction %d: Account %d does not exist and has no balance. Required funds: %d",
                    transaction.getId(), transaction.getSenderId(), amountNQT));

            throw new ValException.NotCurrentlyValidException("Account unknown");
        } else if (amountNQT > verifyAmount) {
            LOGGER.info(
                    String.format("Transaction %d: Account %d pledge balance too low. You have  %d > %d Balance",
                            transaction.getId(), transaction.getSenderId(), amountNQT,
                            verifyAmount));

            throw new ValException.NotCurrentlyValidException("Insufficient funds to pledge");
        }
        CacheMap.put(transaction.getSenderId(), amountNQT);
    }

    void verifyBalanceAndPut(Transaction transaction, Account senderAccount, HashMap<Long, Long> cacheMap, HashMap<Long, Long> writeCacheMap) throws ValException.ValidationException {
        final Long amountNQT = Convert.safeAdd(writeCacheMap.getOrDefault(transaction.getSenderId(), 0L),
                transaction.getType().calculateTotalAmountNQT(transaction));
        final Long amountTotalNQT = Convert.safeAdd(amountNQT, cacheMap.getOrDefault(transaction.getSenderId(), 0L));

        if (senderAccount == null) {
            LOGGER.info(String.format(
                    "Transaction %d: Account %d does not exist and has no balance. Required funds: %d",
                    transaction.getId(), transaction.getSenderId(), amountNQT));

            throw new ValException.NotCurrentlyValidException("Account unknown");
        } else if (amountTotalNQT > senderAccount.getUnconfirmedBalanceNQT()) {
            LOGGER.info(String.format("Transaction %d: Account %d balance too low. You have  %d > %d Balance",
                    transaction.getId(), transaction.getSenderId(), amountTotalNQT,
                    senderAccount.getUnconfirmedBalanceNQT()));

            throw new ValException.NotCurrentlyValidException("Insufficient funds");
        }
        writeCacheMap.put(transaction.getSenderId(), amountNQT);
    }

    void refundBalance(Transaction transaction) {
        if (transaction.getType().getType() == TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getType() && transaction.getType().getSubtype() == TransactionType.Payment.PAYMENT_TRANSACTIONS_PLEDGE.getSubtype()) {
            refundBanlance(transaction, reservedPledgeCache);
        } else if (transaction.getType().getType() == TransactionType.Payment.PAYMENT_TRANSACTIONS_UNPLEDGE.getType() && transaction.getType().getSubtype() == TransactionType.Payment.PAYMENT_TRANSACTIONS_UNPLEDGE.getSubtype()) {
            refundBanlance(transaction, reservedUnpledgeCache);
        }  else {
            refundBanlance(transaction, reservedBalanceCache);
        }
    }

    void refundBanlance(Transaction transaction, HashMap<Long, Long> balanceCache) {
        Long amountNQT = Convert.safeSubtract(balanceCache.getOrDefault(transaction.getSenderId(), 0L),
                transaction.getType().calculateTotalAmountNQT(transaction));

        if (amountNQT > 0) {
            balanceCache.put(transaction.getSenderId(), amountNQT);
        } else {
            balanceCache.remove(transaction.getSenderId());
        }
    }

    public List<Transaction> rebuild(List<Transaction> transactions) {
        clear();

        final List<Transaction> insufficientFundsTransactions = new ArrayList<>();

        for (Transaction t : transactions) {
            try {
                this.reserveBalanceAndPut(t);
            } catch (ValException.ValidationException e) {
                insufficientFundsTransactions.add(t);
            }
        }

        return insufficientFundsTransactions;
    }

    public void clear() {
        reservedBalanceCache.clear();
        reservedPledgeCache.clear();
        reservedUnpledgeCache.clear();
        reservedPledgeAccountCache.clear();
    }

}
