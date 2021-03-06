package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Asset;
import val.assetexchange.AssetExchange;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.INCORRECT_ASSET;
import static val.http.JSONResponses.UNKNOWN_ASSET;
import static val.http.common.Parameters.ASSETS_PARAMETER;
import static val.http.common.ResultFields.ASSETS_RESPONSE;

public final class GetAssets extends APIServlet.APIRequestHandler {

    private final AssetExchange assetExchange;

    public GetAssets(AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, ASSETS_PARAMETER, ASSETS_PARAMETER, ASSETS_PARAMETER); // limit to 3 for testing
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        String[] assets = req.getParameterValues(ASSETS_PARAMETER);

        JsonObject response = new JsonObject();
        JsonArray assetsJsonArray = new JsonArray();
        response.add(ASSETS_RESPONSE, assetsJsonArray);
        for (String assetIdString : assets) {
            if (assetIdString == null || assetIdString.isEmpty()) {
                continue;
            }
            try {
                Asset asset = assetExchange.getAsset(Convert.parseUnsignedLong(assetIdString));
                if (asset == null) {
                    return UNKNOWN_ASSET;
                }

                int tradeCount = assetExchange.getTradeCount(asset.getId());
                int transferCount = assetExchange.getTransferCount(asset.getId());
                int accountsCount = assetExchange.getAssetAccountsCount(asset.getId());

                assetsJsonArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
            } catch (RuntimeException e) {
                return INCORRECT_ASSET;
            }
        }
        return response;
    }

}
