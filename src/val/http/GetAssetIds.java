package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Asset;
import val.assetexchange.AssetExchange;
import val.db.DbIterator;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static val.http.common.Parameters.LAST_INDEX_PARAMETER;
import static val.http.common.ResultFields.ASSET_IDS_RESPONSE;

public final class GetAssetIds extends APIServlet.APIRequestHandler {

    private final AssetExchange assetExchange;

    public GetAssetIds(AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JsonArray assetIds = new JsonArray();
        try (DbIterator<Asset> assets = assetExchange.getAllAssets(firstIndex, lastIndex)) {
            while (assets.hasNext()) {
                assetIds.add(Convert.toUnsignedLong(assets.next().getId()));
            }
        }
        JsonObject response = new JsonObject();
        response.add(ASSET_IDS_RESPONSE, assetIds);
        return response;
    }

}
