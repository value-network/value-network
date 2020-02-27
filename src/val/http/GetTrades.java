package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Asset;
import val.Trade;
import val.ValException;
import val.assetexchange.AssetExchange;
import val.db.DbIterator;
import val.db.sql.DbUtils;
import val.http.common.Parameters;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.TRADES_RESPONSE;

public final class GetTrades extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AssetExchange assetExchange;

    GetTrades(ParameterService parameterService, AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, ASSET_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_ASSET_INFO_PARAMETER);
        this.parameterService = parameterService;
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        String assetId = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
        String accountId = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !Parameters.isFalse(req.getParameter(INCLUDE_ASSET_INFO_PARAMETER));

        JsonObject response = new JsonObject();
        JsonArray tradesData = new JsonArray();
        DbIterator<Trade> trades = null;
        try {
            if (accountId == null) {
                Asset asset = parameterService.getAsset(req);
                trades = assetExchange.getTrades(asset.getId(), firstIndex, lastIndex);
            } else if (assetId == null) {
                Account account = parameterService.getAccount(req);
                trades = assetExchange.getAccountTrades(account.getId(), firstIndex, lastIndex);
            } else {
                Asset asset = parameterService.getAsset(req);
                Account account = parameterService.getAccount(req);
                trades = assetExchange.getAccountAssetTrades(account.getId(), asset.getId(), firstIndex, lastIndex);
            }
            while (trades.hasNext()) {
                final Trade trade = trades.next();
                final Asset asset = includeAssetInfo ? assetExchange.getAsset(trade.getAssetId()) : null;

                tradesData.add(JSONData.trade(trade, asset));
            }
        } finally {
            DbUtils.close(trades);
        }
        response.add(TRADES_RESPONSE, tradesData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }

}
