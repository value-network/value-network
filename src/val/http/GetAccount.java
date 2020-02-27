package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.ValException;
import val.db.DbIterator;
import val.services.AccountService;
import val.services.ParameterService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.ResultFields.*;

public final class GetAccount extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final AccountService accountService;

    GetAccount(ParameterService parameterService, AccountService accountService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
        this.accountService = accountService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        Account account = parameterService.getAccount(req);

        JsonObject response = JSONData.accountBalance(account);
        JSONData.putAccount(response, ACCOUNT_RESPONSE, account.getId());

        if (account.getPublicKey() != null) {
            response.addProperty(PUBLIC_KEY_RESPONSE, Convert.toHexString(account.getPublicKey()));
        }
        if (account.getName() != null) {
            response.addProperty(NAME_RESPONSE, account.getName());
        }
        if (account.getDescription() != null) {
            response.addProperty(DESCRIPTION_RESPONSE, account.getDescription());
        }
        response.addProperty(TOTAL_PLEDGE_RESPONSE, account.getTotalPledged());

        try (DbIterator<Account.AccountAsset> accountAssets = accountService.getAssets(account.getId(), 0, -1)) {
            JsonArray assetBalances = new JsonArray();
            JsonArray unconfirmedAssetBalances = new JsonArray();
            while (accountAssets.hasNext()) {
                Account.AccountAsset accountAsset = accountAssets.next();
                JsonObject assetBalance = new JsonObject();
                assetBalance.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
                assetBalance.addProperty(BALANCE_QNT_RESPONSE, String.valueOf(accountAsset.getQuantityQNT()));
                assetBalances.add(assetBalance);
                JsonObject unconfirmedAssetBalance = new JsonObject();
                unconfirmedAssetBalance.addProperty(ASSET_RESPONSE, Convert.toUnsignedLong(accountAsset.getAssetId()));
                unconfirmedAssetBalance.addProperty(UNCONFIRMED_BALANCE_QNT_RESPONSE, String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
                unconfirmedAssetBalances.add(unconfirmedAssetBalance);
            }

            if (assetBalances.size() > 0) {
                response.add(ASSET_BALANCES_RESPONSE, assetBalances);
            }
            if (unconfirmedAssetBalances.size() > 0) {
                response.add(UNCONFIRMED_ASSET_BALANCES_RESPONSE, unconfirmedAssetBalances);
            }
        }

        return response;
    }

}
