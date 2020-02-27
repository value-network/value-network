package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Asset;
import val.assetexchange.AssetExchange;
import val.db.DbIterator;
import val.services.ParameterService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static val.http.common.Parameters.*;
import static val.http.common.ResultFields.ASSETS_RESPONSE;

public final class GetAssetsByIssuer extends AbstractAssetsRetrieval {

    private final ParameterService parameterService;
    private final AssetExchange assetExchange;

    GetAssetsByIssuer(ParameterService parameterService, AssetExchange assetExchange) {
        super(new APITag[]{APITag.AE, APITag.ACCOUNTS}, assetExchange, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
        this.parameterService = parameterService;
        this.assetExchange = assetExchange;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ParameterException {
        List<Account> accounts = parameterService.getAccounts(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JsonObject response = new JsonObject();
        JsonArray accountsJsonArray = new JsonArray();
        response.add(ASSETS_RESPONSE, accountsJsonArray);
        for (Account account : accounts) {
            try (DbIterator<Asset> assets = assetExchange.getAssetsIssuedBy(account.getId(), firstIndex, lastIndex)) {
                accountsJsonArray.add(assetsToJson(assets));
            }
        }
        return response;
    }

}
