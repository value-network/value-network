package val.services;

import val.Attachment;
import val.Transaction;

import java.util.HashMap;

/**
 * Interface for Database operations related to Accounts
 */
public interface GlobalParameterService {

    HashMap<Long, String> getAllGlobalParametersCache();

    String getPledgeRangeMinCache();

    String getPledgeRangeMaxCache();

    String getMaxPledgeRewardCache();

    String getPoolMaxCapicityCache();

    String getGenBlockRatioCache();

    String getPoolRewardPercentCache();

    String getMinerRewardPercentCache();

    String getPoolCountCache();

    String getPoolerAddressListCache();

    String setPledgeRangeMinCache(String value, long transactionId, int blockHeight);

    String setPledgeRangeMaxCache(String value, long transactionId, int blockHeight);

    String setMaxPledgeRewardCache(String value, long transactionId, int blockHeight);

    String setPoolMaxCapicityCache(String value, long transactionId, int blockHeight);

    String setGenBlockRatioCache(String value, long transactionId, int blockHeight);

    String setPoolRewardPercentCache(String value, long transactionId, int blockHeight);

    String setMinerRewardPercentCache(String value, long transactionId, int blockHeight);

    String setPoolCountCache(String value, long transactionId, int blockHeight);

    String setPoolerAddressListCache(String value, long transactionId, int blockHeight);

    void addOrUpdateParams(Transaction transaction, Attachment attachment);

    void flushGlobalParameterTable();

    void addOrUpdatePoolAccount(String account, int opType, int blockHeight);

}
