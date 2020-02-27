/*
 * This file is generated by jOOQ.
 */
package val.schema;


import val.schema.tables.*;

import javax.annotation.Generated;


/**
 * Convenience access to all tables in DB
 */
@Generated(
        value = {
                "http://www.jooq.org",
                "jOOQ version:3.10.5"
        },
        comments = "This class is generated by jOOQ"
)
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Tables {

    /**
     * The table <code>DB.account</code>.
     */
    public static final Account ACCOUNT = val.schema.tables.Account.ACCOUNT;

    /**
     * The table <code>DB.account_asset</code>.
     */
    public static final AccountAsset ACCOUNT_ASSET = val.schema.tables.AccountAsset.ACCOUNT_ASSET;

    /**
     * The table <code>DB.alias</code>.
     */
    public static final Alias ALIAS = val.schema.tables.Alias.ALIAS;

    /**
     * The table <code>DB.alias_offer</code>.
     */
    public static final AliasOffer ALIAS_OFFER = val.schema.tables.AliasOffer.ALIAS_OFFER;

    /**
     * The table <code>DB.ask_order</code>.
     */
    public static final AskOrder ASK_ORDER = val.schema.tables.AskOrder.ASK_ORDER;

    /**
     * The table <code>DB.asset</code>.
     */
    public static final Asset ASSET = val.schema.tables.Asset.ASSET;

    /**
     * The table <code>DB.asset_transfer</code>.
     */
    public static final AssetTransfer ASSET_TRANSFER = val.schema.tables.AssetTransfer.ASSET_TRANSFER;

    /**
     * The table <code>DB.at</code>.
     */
    public static final At AT = val.schema.tables.At.AT;

    /**
     * The table <code>DB.at_state</code>.
     */
    public static final AtState AT_STATE = val.schema.tables.AtState.AT_STATE;

    /**
     * The table <code>DB.bid_order</code>.
     */
    public static final BidOrder BID_ORDER = val.schema.tables.BidOrder.BID_ORDER;

    /**
     * The table <code>DB.block</code>.
     */
    public static final Block BLOCK = val.schema.tables.Block.BLOCK;

    /**
     * The table <code>DB.escrow</code>.
     */
    public static final Escrow ESCROW = val.schema.tables.Escrow.ESCROW;

    /**
     * The table <code>DB.escrow_decision</code>.
     */
    public static final EscrowDecision ESCROW_DECISION = val.schema.tables.EscrowDecision.ESCROW_DECISION;

    /**
     * The table <code>DB.global_parameter</code>.
     */
    public static final GlobalParameter GLOBAL_PARAMETER = val.schema.tables.GlobalParameter.GLOBAL_PARAMETER;

    /**
     * The table <code>DB.goods</code>.
     */
    public static final Goods GOODS = val.schema.tables.Goods.GOODS;

    /**
     * The table <code>DB.peer</code>.
     */
    public static final Peer PEER = val.schema.tables.Peer.PEER;

    /**
     * The table <code>DB.pledges</code>.
     */
    public static final Pledges PLEDGES = val.schema.tables.Pledges.PLEDGES;

    /**
     * The table <code>DB.pool_miner</code>.
     */
    public static final PoolMiner POOL_MINER = val.schema.tables.PoolMiner.POOL_MINER;

    /**
     * The table <code>DB.purchase</code>.
     */
    public static final Purchase PURCHASE = val.schema.tables.Purchase.PURCHASE;

    /**
     * The table <code>DB.purchase_feedback</code>.
     */
    public static final PurchaseFeedback PURCHASE_FEEDBACK = val.schema.tables.PurchaseFeedback.PURCHASE_FEEDBACK;

    /**
     * The table <code>DB.purchase_public_feedback</code>.
     */
    public static final PurchasePublicFeedback PURCHASE_PUBLIC_FEEDBACK = val.schema.tables.PurchasePublicFeedback.PURCHASE_PUBLIC_FEEDBACK;

    /**
     * The table <code>DB.reward_recip_assign</code>.
     */
    public static final RewardRecipAssign REWARD_RECIP_ASSIGN = val.schema.tables.RewardRecipAssign.REWARD_RECIP_ASSIGN;

    /**
     * The table <code>DB.subscription</code>.
     */
    public static final Subscription SUBSCRIPTION = val.schema.tables.Subscription.SUBSCRIPTION;

    /**
     * The table <code>DB.trade</code>.
     */
    public static final Trade TRADE = val.schema.tables.Trade.TRADE;

    /**
     * The table <code>DB.transaction</code>.
     */
    public static final Transaction TRANSACTION = val.schema.tables.Transaction.TRANSACTION;

    /**
     * The table <code>DB.unconfirmed_transaction</code>.
     */
    public static final UnconfirmedTransaction UNCONFIRMED_TRANSACTION = val.schema.tables.UnconfirmedTransaction.UNCONFIRMED_TRANSACTION;

    /**
     * The table <code>DB.version</code>.
     */
    public static final Version VERSION = val.schema.tables.Version.VERSION;
}
