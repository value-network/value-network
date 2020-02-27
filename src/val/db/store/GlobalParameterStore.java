package val.db.store;

import val.GlobalParameter;
import val.db.DbIterator;
import val.db.VersionedEntityTable;
import val.db.sql.DbKey;

import java.util.HashMap;

/**
 * Interface for Database operations related to Accounts
 */
public interface GlobalParameterStore {

    VersionedEntityTable<GlobalParameter> getGlobalParameterTable();

    DbKey.LongKeyFactory<GlobalParameter> getGlobalParameterKeyFactory();

    DbIterator<GlobalParameter> getAllGlobalParameters();

    GlobalParameter getGlobalParameterByName(long name, int height);

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

    String setPledgeRangeMinCache(String value);

    String setPledgeRangeMaxCache(String value);

    String setMaxPledgeRewardCache(String value);

    String setPoolMaxCapicityCache(String value);

    String setGenBlockRatioCache(String value);

    String setPoolRewardPercentCache(String value);

    String setMinerRewardPercentCache(String value);

    String setPoolCountCache(String value);

    String setPoolerAddressListCache(String value);

}
