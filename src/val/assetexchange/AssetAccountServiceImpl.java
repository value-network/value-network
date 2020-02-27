package val.assetexchange;

import val.Account;
import val.db.DbIterator;
import val.db.store.AccountStore;

class AssetAccountServiceImpl {

    private final AccountStore accountStore;

    public AssetAccountServiceImpl(AccountStore accountStore) {
        this.accountStore = accountStore;
    }

    public DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int from, int to) {
        return accountStore.getAssetAccounts(assetId, from, to);
    }

    public DbIterator<Account.AccountAsset> getAssetAccounts(long assetId, int height, int from, int to) {
        if (height < 0) {
            return getAssetAccounts(assetId, from, to);
        }
        return accountStore.getAssetAccounts(assetId, height, from, to);
    }

    public int getAssetAccountsCount(long assetId) {
        return accountStore.getAssetAccountsCount(assetId);
    }

}
