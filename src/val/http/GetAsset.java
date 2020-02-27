package val.http;

import com.google.gson.JsonElement;
import val.Asset;
import val.ValException;
import val.assetexchange.AssetExchange;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ASSET_PARAMETER;

public final class GetAsset extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AssetExchange assetExchange;

    GetAsset(ParameterService parameterService, AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE}, ASSET_PARAMETER);
        this.parameterService = parameterService;
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {
        final Asset asset = parameterService.getAsset(req);

        int tradeCount = assetExchange.getTradeCount(asset.getId());
        int transferCount = assetExchange.getTransferCount(asset.getId());
        int accountsCount = assetExchange.getAssetAccountsCount(asset.getId());

        return JSONData.asset(asset, tradeCount, transferCount, accountsCount);
    }

}
