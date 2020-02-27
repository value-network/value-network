package val;

import org.jooq.Result;
import val.Attachment.MessagingGlobalPrameter;
import val.crypto.Crypto;
import val.db.store.AccountStore;
import val.db.store.GlobalParameterStore;
import val.props.Props;
import val.schema.tables.records.AccountRecord;
import val.services.impl.AccountServiceImpl;
import val.util.Convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

public class MinePool {

    // public static String getMinePool() {
    // 	return Volume.getPropertyService().getString(Props.MINE_POOL_WHITELIST);
    // }

    // public static String[] getMinePoolList() {
    // 	String minePools = Volume.getPropertyService().getString(Props.MINE_POOL_WHITELIST);
    // 	return minePools.split(",");
    // }

    // public static boolean verifyMinePooler(String recipientID) {
    // 	boolean isMiner = false;
    // 	String minePools = Volume.getPropertyService().getString(Props.MINE_POOL_WHITELIST);
    // 	System.out.printf("minePools:%s, recipientID:%s", minePools, recipientID);
    // 	if (minePools.indexOf(recipientID) >= 0) {
    // 		isMiner = true;
    // 	}
    // 	return isMiner;
    // }

    private static MinePool instance = null;
    private final Map<Long, byte[]> pool;
    private final List<PledgeRewardChangeLog> pledgeRewardChangeHistory; // block height -> max block reward
    private TransactionDb transactionDb;
    private boolean isPool;
    private String secretPhrase;
    private long accountID;
    private long poolMaxCapicity = 2 * 1024;
    private long poolCount = 44;
    private long maxPledgeReward = Constants.MAX_PLEDGE_REWARD;
    private long minPledge = Constants.MIN_PLEDGE_1T;
    private long maxPledge = Constants.MAX_PLEDGE_1T;
    private long poolRewardPercent = 1000L;
    private long minerRewardPercent = 5000L;
    private MinePool() {
        GlobalParameterStore globalParameterStore = ValNetwork.getStores().getGlobalParameterStore();
        globalParameterStore.getAllGlobalParameters();

        AccountStore accountStore = ValNetwork.getStores().getAccountStore();
        Result<AccountRecord> minePools = accountStore.getAccountRoles(1, 0, 0);
        pool = new ConcurrentHashMap<>();
        long firstAccount = Account.getId(Convert.parseHexString(Constants.FOUNDATION_PUBLIC_KEY_HEX));
        pool.put(firstAccount, Convert.parseHexString(Constants.FOUNDATION_PUBLIC_KEY_HEX));
        long masterAccount = Account.getId(Convert.parseHexString(Constants.MASTER_PUBLIC_KEY_HEX));
        pool.put(masterAccount, Convert.parseHexString(Constants.MASTER_PUBLIC_KEY_HEX));
//		while(minePoolsIter.hasNext()){
        for (AccountRecord minePool : minePools) {
//			Account minePool = minePoolsIter.next();
            if (minePool != null) {
                byte[] publicKey = minePool.getPublicKey();
                if (publicKey == null) {
                    publicKey = "0".getBytes();
                }
                pool.put(minePool.getId(), publicKey);
            }
        }

        pledgeRewardChangeHistory = new ArrayList();
        changePledgeReward(0, maxPledgeReward);

        minePoolConfig();
        initPoolNode();
    }

    public static MinePool getInstance() {
        if (null == instance) {
            synchronized (MinePool.class) {
                if (null == instance) {
                    instance = new MinePool();
                }
            }
        }
        return instance;
    }

    public long totalReward(long height) {
        long ret = 0;
        int idx = 1;
        for (; idx < pledgeRewardChangeHistory.size(); idx++) {
            ret += pledgeRewardChangeHistory.get(idx).calculateTotalRewardBefore(pledgeRewardChangeHistory.get(idx - 1), height);
        }
        ret += pledgeRewardChangeHistory.get(idx - 1).calcuateTotalRewardToHeight(height);
        return ret;
    }

    public void changePledgeReward(long height, long reward) {
        if (pledgeRewardChangeHistory.size() == 0) {
            pledgeRewardChangeHistory.add(new PledgeRewardChangeLog(height, reward));
        } else if (reward != pledgeRewardChangeHistory.get(pledgeRewardChangeHistory.size() - 1).reward) {
            pledgeRewardChangeHistory.add(new PledgeRewardChangeLog(height, reward));
        }
        out.printf("change Pledge Reward at height:%d, reward:%d, total change log:%d\n", height, reward, pledgeRewardChangeHistory.size());
    }

    //  get pool from config file
    private void minePoolConfig() {
        String poolPublicKeys = ValNetwork.getPropertyService().getString(Props.MINE_POOL_PUBLIC_KEY_LIST);

        String[] poolKeyList = poolPublicKeys.split(";");

        //pool = new ConcurrentHashMap<>();

        for (int i = 0; i < poolKeyList.length; i++) {
            byte[] publicKey = Convert.parseHexString(poolKeyList[i]);
            byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
            Long id = Convert.fullHashToId(publicKeyHash);
            System.out.printf("pool ID:%d, pk:%s\n", id, poolKeyList[i]);
            pool.put(id, publicKey);
        }
        //initPoolNode();
    }

    public void loadSetting(TransactionDb transactionDb) {
        if (null != transactionDb) {
            this.transactionDb = transactionDb;

            byte type = 1;
            byte subType = 2;
            List<Transaction> txns = transactionDb.findTransactionsByType(type, subType);

            txns.forEach(txn -> {
                Attachment rawAttachment = txn.getAttachment();
                // System.out.printf("loadSetting: txn height:%d attachment:%s\n", txn.getHeight(), rawAttachment.getJsonObject().toString());
                TransactionType txnType = rawAttachment.getTransactionType();
                if (txnType.getType() == type && txnType.getSubtype() == subType && rawAttachment instanceof MessagingGlobalPrameter) {
                    MessagingGlobalPrameter attachment = (MessagingGlobalPrameter) txn.getAttachment();
                    if (!Convert.isNullorEmpty(attachment.getMaxPledgeReward())) {
                        changePledgeReward(txn.getHeight(), Long.parseLong(attachment.getMaxPledgeReward()));
                    }
                }
            });
        }
    }
//	private long K = 196227800000L; // 1962.278

//	private long dreaseRewardCycle = 360 * 365 / 2; // 每半年为周期
//	private long dreaseRate = 6; // 每周期减产百分比 6%

    public long getPoolMaxCapCache() {
        long poolMaxCapicityCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getPoolMaxCapicityCache());
        return poolMaxCapicityCache > 0 ? poolMaxCapicityCache : poolMaxCapicity;
    }

    public long getPoolCountCache() {
        long poolCountCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getPoolCountCache());
        return poolCountCache > 0 ? poolCountCache : poolCount;
    }

    public long getMaxPledgeRewardCache() {
        long maxPledgeRewardCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getMaxPledgeRewardCache());
        return maxPledgeRewardCache > 0 ? maxPledgeRewardCache : maxPledgeReward;
    }

    public long getMinPledgeCache() {
        long minPledgeCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getPledgeRangeMinCache());
        return minPledgeCache > 0 ? minPledgeCache : minPledge;
    }

    public long getMaxPledgeCache() {
        long maxPledgeCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getPledgeRangeMaxCache());
        return maxPledgeCache > 0 ? maxPledgeCache : maxPledge;
    }

    public long getPoolRewardPercentCache() {
        long poolRewardPercentCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getPoolRewardPercentCache());
        return poolRewardPercentCache > 0 ? poolRewardPercentCache : poolRewardPercent;
    }

    public long getMinerRewardPercentCache() {
        long minerRewardPercentCache = Convert.parseUnsignedLong(ValNetwork.getStores().getGlobalParameterStore().getMinerRewardPercentCache());
        return minerRewardPercentCache > 0 ? minerRewardPercentCache : minerRewardPercent;
    }

    private long getK(long height) {
        // return (360*365) * 1500*Constants.ONE_BURST/(44*2048) - (1200-300)*Constants.ONE_BURST/4;
        // 360 * 365 * 1500: 一年的总块数 最大产币量 -> 一年的最大产币量
        // 44 * 2048: 全网最大算力
        // 360 * 365 * 1500/(44*2048): 1T算力的一年的最大产币量
        // (1200-300) 1T算力的质押差值
        // 每 180 * 365 个块（半年时间）最大产币量减少6%
        long round = height / 180 / 365;
        long numerator = (long) Math.pow(94, round);
        long denominator = (long) Math.pow(100, round);
        long max = getMaxPledgeRewardCache(height);
        long K = (360 * 365) * max * numerator / denominator / (getPoolCountCache() * getPoolMaxCapCache()) - (getMaxPledgeCache() - getMinPledgeCache()) / 4;
        out.printf("getK- max:%s, poolcount:%s, poolMaxCap:%s, maxPledge:%s, minPledge:%s, height:%s, round:%s, numerator:%s, denominator:%s, K:%s\n",
                max, getPoolCountCache(), getPoolMaxCapCache(), getMaxPledgeCache(), getMinPledgeCache(), height, round, numerator, denominator, K);
        return K;
    }
//	public long getGenBlockRetioCache(){
//		long genBlockRetioCache = Convert.parseUnsignedLong(Volume.getStores().getGlobalParameterStore().getGenBlockRatioCache());
//		return genBlockRetioCache > 0 ? genBlockRetioCache : K;
//	}

    public long getMaxPledgeRewardCache(long height) { // 每半年减少当前产量的6%
        long round = height / 180 / 365;
        long numerator = (long) Math.pow(94, round);
        long denominator = (long) Math.pow(100, round);
        return getMaxPledgeRewardCache() * numerator / denominator;
    }

    public long getPledgeReward(long pledgedAmount, long blockHeight) {
        long a = (pledgedAmount - Constants.GLOBAL_POOL_BASE_PLEDGE) / getPoolMaxCapCache() - getMinPledgeCache();
        if (a < 0) {
            return 0;
        }
        // long b = (a / 4 + getGenBlockRetioCache()) / 365;
        long b = (a / 4 + getK(blockHeight)) / 365;
        long c = b * getPoolMaxCapCache() * getPoolCountCache();
        long d = c / 360;
        long maxReward = getMaxPledgeRewardCache(blockHeight);
        if (d > maxReward) {
            d = maxReward;
        }

        out.printf("getPledgeReward, a:%s, b:%s, c:%s, d:%s, maxReward:%s\n", a, b, c, d, maxReward);
//		long a = pledgedAmount / poolMaxCapicity - minPledge;
//		if (a < 0) {
//			return 0;
//		}
//		long b = (a / 4 + K) / 365;
//		long c = b * poolMaxCapicity * poolCount;
//		long d = c / 360;
//		if (d > maxPledgeReward) {
//			d = maxPledgeReward;
//		}
        return d;
    }

    public long getPoolRewardRate() {
        return poolRewardPercent;
    }

    public long getMinerRewardRate() {
        return minerRewardPercent;
    }

    public boolean verifyMinePooler(long account) {
        return pool.keySet().contains(new Long(account));
    }

    public Map<Long, byte[]> getMinePoolMap() {
        return pool;
    }

    public byte[] getPublicKey(long accountID) {
        return pool.get(new Long(accountID));
    }

    public boolean isPoolNode() {
        return isPool;
    }

    public String getPoolSecretPhrase() {
        if (isPool) {
            return secretPhrase;
        }
        return null;
    }

    public long getPoolAccountID() {
        if (isPool) {
            return accountID;
        }
        return 0;
    }

    public void initPoolNode() {
        secretPhrase = ValNetwork.getPropertyService().getString(Props.MINE_POOL_PASSPHASE);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        accountID = Convert.fullHashToId(publicKeyHash);
        byte[] knownPublicKey = getPublicKey(accountID);

        System.out.printf("accountID:%d, pk:%s\n", accountID, Convert.toHexString(publicKey));
        isPool = Arrays.equals(publicKey, knownPublicKey);
    }

    public void updateIsPool(List<String> pkStrList, int flag) {
        secretPhrase = ValNetwork.getPropertyService().getString(Props.MINE_POOL_PASSPHASE);
        byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        byte[] publicKeyHash = Crypto.sha256().digest(publicKey);
        accountID = Convert.fullHashToId(publicKeyHash);


        for (String pkStr : pkStrList) {
            byte[] pk = Convert.parseHexString(pkStr);
            long accID = AccountServiceImpl.getId(pk);
            if (flag == 1) {
                pool.put(new Long(accID), pk);
            } else {
                pool.remove(new Long(accID));
            }

            if (accID == accountID) {
                this.isPool = flag == 1;
            }
            out.printf("accID:%s, accountID:%s, isPool:%s\n", accID, accountID, this.isPool);
        }

    }

    private class PledgeRewardChangeLog {
        public long height; // 生效高度
        public long reward; // 奖励值 最小单位

        public PledgeRewardChangeLog(long height, long reward) {
            this.height = height;
            this.reward = reward;
        }

        public long calculateTotalRewardBefore(PledgeRewardChangeLog b, long curHeight) {
            if (height > b.height) {
                return b.calcuateTotalRewardToHeight(Math.min(this.height - 1, curHeight));
            }
            return calcuateTotalRewardToHeight(height);
        }

        public long calcuateTotalRewardToHeight(long height) {
            if (height < this.height) {
                return 0L;
            }
            if (0 == this.height) {
                return (height - this.height) * this.reward;
            }
            return (height - this.height + 1) * this.reward;
        }
    }

}
