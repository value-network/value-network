package val;

import io.grpc.Server;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.AT.HandleATBlockTransactionsListener;
import val.assetexchange.AssetExchange;
import val.blockchainlistener.DevNullListener;
import val.db.BlockDb;
import val.db.cache.DBCacheManagerImpl;
import val.db.sql.Db;
import val.db.store.BlockchainStore;
import val.db.store.Dbs;
import val.db.store.DerivedTableManager;
import val.db.store.Stores;
import val.deeplink.DeeplinkQRCodeGenerator;
import val.feesuggestions.FeeSuggestionCalculator;
import val.fluxcapacitor.FluxCapacitor;
import val.fluxcapacitor.FluxCapacitorImpl;
import val.http.API;
import val.http.APITransactionManager;
import val.http.APITransactionManagerImpl;
import val.peer.Peers;
import val.props.PropertyService;
import val.props.PropertyServiceImpl;
import val.props.Props;
import val.services.*;
import val.services.impl.*;
import val.statistics.StatisticsManagerImpl;
import val.util.DownloadCacheImpl;
import val.util.LoggerConfigurator;
import val.util.ThreadPool;
import val.util.Time;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static val.schema.Tables.UNCONFIRMED_TRANSACTION;

public final class ValNetwork {

    public static final Version VERSION = Version.parse("v0.1.3");
    public static final String APPLICATION = "VLM";

    private static final String DEFAULT_PROPERTIES_NAME = "val-default.properties";

    //fork 3328
    private static final String DEFAULT_HEIGHT_NAME = ".vlm";
    private static final int DEFAULT_HEIGHT_VALUE = 0;

    private static final Logger logger = LoggerFactory.getLogger(ValNetwork.class);

    private static Stores stores;
    private static Dbs dbs;

    private static ThreadPool threadPool;

    private static BlockchainImpl blockchain;
    private static BlockchainProcessorImpl blockchainProcessor;
    private static TransactionProcessorImpl transactionProcessor;

    private static PropertyService propertyService;
    private static FluxCapacitor fluxCapacitor;

    private static DBCacheManagerImpl dbCacheManager;

    private static API api;
    private static Server apiV2Server;

    private ValNetwork() {
    } // never

    private static PropertyService loadProperties() {
        final Properties defaultProperties = new Properties();

        logger.info("Initializing Vol Reference Software (VLM) version {}", VERSION);
        try (InputStreamReader is = new InputStreamReader(ClassLoader.getSystemResourceAsStream(DEFAULT_PROPERTIES_NAME), "UTF-8")) {
            if (is != null) {
                logger.info("property file 1 {}", DEFAULT_PROPERTIES_NAME);
                defaultProperties.load(is);
            } else {
                String configFile = System.getProperty(DEFAULT_PROPERTIES_NAME);

                if (configFile != null) {
                    // try (InputStream fis = new InputStreamReader(configFile, "UTF-8")) {
                    try (InputStreamReader fis = new InputStreamReader(new FileInputStream(configFile), "UTF-8")) {
                        defaultProperties.load(fis);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME + " from " + configFile);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME + " from " + configFile);
                    }
                } else {
                    throw new RuntimeException(DEFAULT_PROPERTIES_NAME + " not in classpath and system property " + DEFAULT_PROPERTIES_NAME + " not defined either");
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME, e);
        } catch (IOException e) {
            throw new RuntimeException("Error loading " + DEFAULT_PROPERTIES_NAME, e);
        }

        Properties properties;
        try (InputStreamReader is = new InputStreamReader(ClassLoader.getSystemResourceAsStream("val.properties"), "UTF-8")) {
            logger.info("property file 2 {}", DEFAULT_PROPERTIES_NAME);
            properties = new Properties(defaultProperties);
            if (is != null) { // parse if val.properties was loaded
                properties.load(is);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Error loading val.properties", e);
        } catch (IOException e) {
            throw new RuntimeException("Error loading val.properties", e);
        }

        return new PropertyServiceImpl(properties);
    }

    public static BlockchainImpl getBlockchain() {
        return blockchain;
    }

    public static BlockchainProcessorImpl getBlockchainProcessor() {
        return blockchainProcessor;
    }

    public static TransactionProcessorImpl getTransactionProcessor() {
        return transactionProcessor;
    }

    public static Stores getStores() {
        return stores;
    }

    public static Dbs getDbs() {
        return dbs;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(ValNetwork::shutdown));
        init();
    }

    private static void validateVersionNotDev(PropertyService propertyService) {
        if (VERSION.isPrelease() && !propertyService.getBoolean(Props.DEV_TESTNET)) {
            logger.error("THIS IS A DEVELOPMENT WALLET, PLEASE DO NOT USE THIS");
            System.exit(0);
        }
    }

    public static void init(Properties customProperties) {
        loadWallet(new PropertyServiceImpl(customProperties));
    }

    private static void init() {
        loadWallet(loadProperties());
        URL hiPath = ClassLoader.getSystemResource(DEFAULT_HEIGHT_NAME);
        try {
            if (hiPath != null) {
                Path path = Paths.get(hiPath.getPath());
                if (Files.exists(path)) {
                    StringBuilder sb = new StringBuilder();
                    Files.readAllLines(path).forEach(line -> sb.append(line));
                    if (blockchain.getLastBlock() != null) {
                        int height = Integer.valueOf(sb.toString().trim());
                        if (blockchain.getLastBlock().getHeight() > height) {
                            blockchainProcessor.popOffTo(height);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.info("{}", e);
        } finally {
            if (hiPath != null) {
                try {
                    Path path = Paths.get(hiPath.getPath());
                    Files.delete(path);
                } catch (IOException e) {
                }
            }
        }
    }

    private static void loadWallet(PropertyService propertyService) {
        validateVersionNotDev(propertyService);
        ValNetwork.propertyService = propertyService;

        try {
            long startTime = System.currentTimeMillis();

            final TimeService timeService = new TimeServiceImpl();

            final DerivedTableManager derivedTableManager = new DerivedTableManager();

            final StatisticsManagerImpl statisticsManager = new StatisticsManagerImpl(timeService);
            dbCacheManager = new DBCacheManagerImpl(statisticsManager);

            threadPool = new ThreadPool(propertyService);

            LoggerConfigurator.init();

            Db.init(propertyService, dbCacheManager);
            dbs = Db.getDbsByDatabaseType();

            stores = new Stores(derivedTableManager, dbCacheManager, timeService, propertyService);

            final TransactionDb transactionDb = dbs.getTransactionDb();
            final BlockDb blockDb = dbs.getBlockDb();
            final BlockchainStore blockchainStore = stores.getBlockchainStore();
            blockchain = new BlockchainImpl(transactionDb, blockDb, blockchainStore);

            final AliasService aliasService = null;//new AliasServiceImpl(stores.getAliasStore());
            fluxCapacitor = new FluxCapacitorImpl(blockchain, propertyService);

            EconomicClustering economicClustering = new EconomicClustering(blockchain);

            final GlobalParameterServiceImpl globalParameterService = new GlobalParameterServiceImpl(stores.getGlobalParameterStore(), stores.getAccountStore());

            final Generator generator = propertyService.getBoolean(Props.DEV_MOCK_MINING) ? new GeneratorImpl.MockGenerator(propertyService, blockchain, timeService, fluxCapacitor) : new GeneratorImpl(blockchain, timeService, fluxCapacitor);

            final AccountService accountService = new AccountServiceImpl(stores.getAccountStore(), stores.getAssetTransferStore(), stores.getPledgeStore(), stores.getPoolMinerStore());

            final TransactionService transactionService = new TransactionServiceImpl(accountService, blockchain);

            transactionProcessor = new TransactionProcessorImpl(propertyService, economicClustering, blockchain, stores, timeService, dbs,
                    accountService, transactionService, threadPool);

            final ATService atService = new ATServiceImpl(stores.getAtStore());
            final SubscriptionService subscriptionService = null; // new SubscriptionServiceImpl(stores.getSubscriptionStore(), transactionDb, blockchain, aliasService, accountService);
            final DGSGoodsStoreService digitalGoodsStoreService = null; // new DGSGoodsStoreServiceImpl(blockchain, stores.getDigitalGoodsStoreStore(), accountService);
            final EscrowService escrowService = null; // new EscrowServiceImpl(stores.getEscrowStore(), blockchain, aliasService, accountService);

            final AssetExchange assetExchange = null; //new AssetExchangeImpl(accountService, stores.getTradeStore(), stores.getAccountStore(), stores.getAssetTransferStore(), stores.getAssetStore(), stores.getOrderStore());

            final DownloadCacheImpl downloadCache = new DownloadCacheImpl(propertyService, fluxCapacitor, blockchain);

            final BlockService blockService = new BlockServiceImpl(accountService, transactionService, blockchain, downloadCache, generator);
            blockchainProcessor = new BlockchainProcessorImpl(threadPool, blockService, transactionProcessor, blockchain, propertyService, subscriptionService,
                    timeService, derivedTableManager,
                    blockDb, transactionDb, economicClustering, blockchainStore, stores, escrowService, transactionService, downloadCache, generator, statisticsManager,
                    dbCacheManager, accountService, globalParameterService);

            final FeeSuggestionCalculator feeSuggestionCalculator = new FeeSuggestionCalculator(blockchainProcessor, blockchainStore, 10);

            generator.generateForBlockchainProcessor(threadPool, blockchainProcessor);

            final DeeplinkQRCodeGenerator deepLinkQRCodeGenerator = new DeeplinkQRCodeGenerator();

            final ParameterService parameterService = new ParameterServiceImpl(accountService, aliasService, assetExchange,
                    digitalGoodsStoreService, blockchain, blockchainProcessor, transactionProcessor, atService);

            addBlockchainListeners(blockchainProcessor, accountService, digitalGoodsStoreService, blockchain, dbs.getTransactionDb());

            final APITransactionManager apiTransactionManager = new APITransactionManagerImpl(parameterService, transactionProcessor, blockchain, accountService, transactionService);

            Peers.init(timeService, accountService, blockchain, transactionProcessor, blockchainProcessor, propertyService, threadPool);


            TransactionType.init(blockchain, fluxCapacitor, accountService, digitalGoodsStoreService, aliasService, assetExchange, subscriptionService, escrowService, globalParameterService);


            api = new API(transactionProcessor, blockchain, blockchainProcessor, parameterService,
                    accountService, aliasService, assetExchange, escrowService, digitalGoodsStoreService,
                    subscriptionService, atService, timeService, economicClustering, propertyService, threadPool,
                    transactionService, blockService, generator, apiTransactionManager, feeSuggestionCalculator, globalParameterService, deepLinkQRCodeGenerator);

            // if (propertyService.getBoolean(Props.API_V2_SERVER)) {
            //     int port = propertyService.getBoolean(Props.DEV_TESTNET) ? propertyService.getInt(Props.DEV_API_V2_PORT) : propertyService.getInt(Props.API_V2_PORT);
            //     logger.info("Starting V2 API Server on port {}", port);
            //     VlmService apiV2 = new VlmService(blockchainProcessor, blockchain, blockService, accountService, generator, transactionProcessor);
            //     apiV2Server = ServerBuilder.forPort(port).addService(apiV2).build().start();
            // } else {
            //     logger.info("Not starting V2 API Server - it is disabled.");
            // }

            DebugTrace.init(propertyService, blockchainProcessor, accountService, assetExchange, digitalGoodsStoreService);

            MinePool.getInstance().loadSetting(transactionDb);

            // backward compatibility for those who have some unconfirmed transactions in their db
            try {
                stores.beginTransaction();
                try (DSLContext ctx = Db.getDSLContext()) {
                    ResultSet rs = ctx.selectFrom(UNCONFIRMED_TRANSACTION).fetchResultSet();
                    while (rs.next()) {
                        byte[] transactionBytes = rs.getBytes("transaction_bytes");
                        Transaction transaction = Transaction.parseTransaction(transactionBytes);
                        transaction.setHeight(rs.getInt("transaction_height"));
                        transactionService.undoUnconfirmed(transaction);
                    }
                    ctx.truncate(UNCONFIRMED_TRANSACTION).execute();
                }
                accountService.flushAccountTable();
                stores.commitTransaction();
            } catch (Exception e) {
                logger.error(e.toString(), e);
                stores.rollbackTransaction();
                throw e;
            } finally {
                stores.endTransaction();
            }

            int timeMultiplier = (propertyService.getBoolean(Props.DEV_TESTNET) && propertyService.getBoolean(Props.DEV_OFFLINE)) ? Math.max(propertyService.getInt(Props.DEV_TIMEWARP), 1) : 1;

            threadPool.start(timeMultiplier);
            if (timeMultiplier > 1) {
                timeService.setTime(new Time.FasterTime(Math.max(timeService.getEpochTime(), getBlockchain().getLastBlock().getTimestamp()), timeMultiplier));
                logger.info("TIME WILL FLOW " + timeMultiplier + " TIMES FASTER!");
            }

            long currentTime = System.currentTimeMillis();
            logger.info("Initialization took " + (currentTime - startTime) + " ms");
            logger.info("VLM " + VERSION + " started successfully.");

            if (propertyService.getBoolean(Props.DEV_TESTNET)) {
                logger.info("RUNNING ON TESTNET - DO NOT USE REAL ACCOUNTS!");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
        (new Thread(ValNetwork::commandHandler)).start();
    }

    private static void addBlockchainListeners(BlockchainProcessor blockchainProcessor, AccountService accountService, DGSGoodsStoreService goodsService, Blockchain blockchain,
                                               TransactionDb transactionDb) {

        final HandleATBlockTransactionsListener handleATBlockTransactionListener = new HandleATBlockTransactionsListener(accountService, blockchain, transactionDb);
        final DevNullListener devNullListener = new DevNullListener(accountService, goodsService);

        blockchainProcessor.addListener(handleATBlockTransactionListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
        blockchainProcessor.addListener(devNullListener, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static void shutdown() {
        shutdown(false);
    }

    private static void commandHandler() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            String command;
            while ((command = reader.readLine()) != null) {
                logger.debug("received command: >" + command + "<");
                if (command.equals(".shutdown")) {
                    shutdown(false);
                    System.exit(0);
                } else if (command.startsWith(".popoff ")) {
                    Pattern r = Pattern.compile("^\\.popoff (\\d+)$");
                    Matcher m = r.matcher(command);
                    if (m.find()) {
                        int numBlocks = Integer.parseInt(m.group(1));
                        if (numBlocks > 0) {
                            blockchainProcessor.popOffTo(blockchain.getHeight() - numBlocks);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public static void shutdown(boolean ignoreDBShutdown) {
        logger.info("Shutting down...");
        if (api != null)
            api.shutdown();
        if (apiV2Server != null)
            apiV2Server.shutdownNow();
        Peers.shutdown(threadPool);
        threadPool.shutdown();
        if (!ignoreDBShutdown) {
            Db.shutdown();
        }
        dbCacheManager.close();
        if (blockchainProcessor != null && blockchainProcessor.getOclVerify()) {
            OCLPoC.destroy();
        }
        logger.info("VLM " + VERSION + " stopped.");
        LoggerConfigurator.shutdown();
    }

    public static PropertyService getPropertyService() {
        return propertyService;
    }

    public static FluxCapacitor getFluxCapacitor() {
        return fluxCapacitor;
    }

}
