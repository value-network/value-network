package val;

import val.props.Props;

import java.util.Calendar;
import java.util.TimeZone;


public final class Constants {

    public static final int CHAIN_DIFF_ADJUST_CHANGE_BLOCK = 2700;

    public static final long CHAIN_REWARD_RECIPIENT_ASSIGNMENT_WAIT_TIME = 4;

    // not sure when these were enabled, but they each do an alias lookup every block if greater than the current height
    public static final long CHAIN_ESCROW_START_BLOCK = 0;
    public static final long CHAIN_SUBSCRIPTION_START_BLOCK = 0;
    public static final int CHAIN_SUBSCRIPTION_MIN_FREQ = 3600;
    public static final int CHAIN_SUBSCRIPTION_MAX_FREQ = 31536000;

    public static final int BLOCK_HEADER_LENGTH = 232;

    public static final long MAX_BALANCE_CHAIN = 10000000000L; // modify due to vol volumes

    public static final long FEE_QUANT = 735000 * 5;
    public static final long ONE_COIN = 100000000;
    public static final long BLOCK_REWARD_MONTH_BASE = 500000 * ONE_COIN;

    public static final long WITHDRAW_ALLOW_CYCLE = 0;

    public static final int MIDDLE_PAYLOAD_CYCLE = 2 * 7 * 24 * 60 * 60;
    public static final int TRANSACTION_RATE_CYCLE = 24 * 60 * 60;
    public static final String MIDDLE_PAYLOAD = "MiddlePayload";
    public static final String TRANSACTION_RATE = "TransactionRate";

    public static final long MAX_BALANCE_NQT = MAX_BALANCE_CHAIN * ONE_COIN;
    public static final long INITIAL_BASE_TARGET = 1832519379600L;
    public static final long MAX_BASE_TARGET = 1832519379600L;
    public static final int MAX_ROLLBACK = ValNetwork.getPropertyService().getInt(Props.DB_MAX_ROLLBACK);

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 1000;

    public static final int MAX_MULTI_OUT_RECIPIENTS = 64;
    public static final int MAX_MULTI_SAME_OUT_RECIPIENTS = 128;

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final int MAX_PARAMETER_CHAIN_LENGTH = 65535;
    public static final int MAX_PARAMETER_POOLLIST_LENGTH = Integer.MAX_VALUE;
    ;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final long ASSET_ISSUANCE_FEE_NQT = 1000 * ONE_COIN;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 10240;

    public static final int NQT_BLOCK = 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP = 0;
    public static final int PUBLIC_KEY_ANNOUNCEMENT_BLOCK = Integer.MAX_VALUE;

    public static final int MAX_AUTOMATED_TRANSACTION_NAME_LENGTH = 30;
    public static final int MAX_AUTOMATED_TRANSACTION_DESCRIPTION_LENGTH = 1000;

    public static final long GLOBAL_PLEDGE_RANGE_MIN_DEFAULT = 300 * ONE_COIN;
    public static final long GLOBAL_PLEDGE_RANGE_MAX_DEFAULT = 1200 * ONE_COIN;
    public static final long GLOBAL_MAX_PLEDGE_REWARD_DEFAULT = 600 * ONE_COIN;
    public static final long GLOBAL_POOL_MAX_CAPICITY_DEFAULT = 2 * 1024;//2P
    public static final long GLOBAL_GEN_BLOCK_RATIO_DEFAULT = 196227800000L; //1962.278 * ONE_BURST
    public static final long GLOBAL_POOL_REWARD_PERCENT_DEFAULT = 1000;
    public static final long GLOBAL_MINER_REWARD_PERCENT_DEFAULT = 5000;
    public static final long GLOBAL_POOL_COUNT_DEFAULT = 44;

    public static final long GLOBAL_POOL_BASE_PLEDGE = 2000000 * ONE_COIN;

    public static final long GLOBAL_PLEDGE_RANGE_MIN = 100001L;
    public static final long GLOBAL_PLEDGE_RANGE_MAX = 100002L;
    public static final long GLOBAL_MAX_PLEDGE_REWARD = 100003L;
    public static final long GLOBAL_POOL_MAX_CAPICITY = 100004L;
    public static final long GLOBAL_GEN_BLOCK_RATIO = 100005L;
    public static final long GLOBAL_POOL_REWARD_PERCENT = 100006L;
    public static final long GLOBAL_MINER_REWARD_PERCENT = 100007L;
    public static final long GLOBAL_POOL_COUNT = 100008L;
    public static final long GLOBAL_POOLER_ADDRESS_LIST = 100009L;

    public static final String HTTP = "http://";

    public static final Version MIN_VERSION = Version.parse("0.1.2");

    public static final Version MINER_MIN_VERSION = Version.parse("0.1.1");
    public static final long EPOCH_BEGINNING;
    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    public static final int EC_RULE_TERMINATOR = 2400; /* cfb: This constant defines a straight edge when "longest chain"
                                                        rule is outweighed by "economic majority" rule; the terminator
                                                        is set as number of seconds before the current time. */
    public static final int EC_BLOCK_DISTANCE_LIMIT = 60;
    public static final int EC_CHANGE_BLOCK_1 = 67000;
    public static final String RESPONSE = "response";
    public static final String TOKEN = "token";
    public static final String WEBSITE = "website";
    public static final String PROTOCOL = "protocol";
    public static final String FOUNDATION_PUBLIC_KEY_HEX = "d3070cf1c904ad853538f45493462c6af701df9c6e767315f23ee4b8233c3a1b";// for bonus
    public static final String MASTER_PUBLIC_KEY_HEX = "d3070cf1c904ad853538f45493462c6af701df9c6e767315f23ee4b8233c3a1b";// for operation
    public static final String FOUNDATION_MINER_KEY_HEX = "6c8a03e5c64d20775f97e55e45419565c313ec12bacf957996dedc61faf72648";// for operation
    public static final int START_TIME = 14748773;
    public static final long MIN_PLEDGE_1T = GLOBAL_PLEDGE_RANGE_MIN_DEFAULT;
    public static final long MAX_PLEDGE_1T = GLOBAL_PLEDGE_RANGE_MAX_DEFAULT;
    public static final long MAX_PLEDGE_REWARD = GLOBAL_MAX_PLEDGE_REWARD_DEFAULT;
    public static final long BLOCK_REWARD_FOUNDATION_PERCENT = 49L;
    public static final long BLOCK_REWARD_FOUNDATION_PERCENT_BASE = 100L;
    public static final long BLOCK_REWARD_MINER_PERCENT = 2L;
    static final long UNCONFIRMED_POOL_DEPOSIT_NQT = (ValNetwork.getPropertyService().getBoolean(Props.DEV_TESTNET) ? 50 : 100) * ONE_COIN;

    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 9);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 9);
        calendar.set(Calendar.SECOND, 9);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH_BEGINNING = calendar.getTimeInMillis();

        if (MAX_ROLLBACK < 1440) {
            throw new RuntimeException("vlm.maxRollback must be at least 1440");
        }
    }

    private Constants() {
    } // never

}
