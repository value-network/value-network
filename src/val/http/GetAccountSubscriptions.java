package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Account;
import val.Subscription;
import val.ValException;
import val.db.DbIterator;
import val.services.ParameterService;
import val.services.SubscriptionService;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.Parameters.ACCOUNT_PARAMETER;
import static val.http.common.Parameters.SUBSCRIPTIONS_RESPONSE;

public final class GetAccountSubscriptions extends APIServlet.APIRequestHandler {

    private final ParameterService parameterService;
    private final SubscriptionService subscriptionService;

    GetAccountSubscriptions(ParameterService parameterService, SubscriptionService subscriptionService) {
        super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
        this.parameterService = parameterService;
        this.subscriptionService = subscriptionService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) throws ValException {

        Account account = parameterService.getAccount(req);

        JsonObject response = new JsonObject();

        JsonArray subscriptions = new JsonArray();

        DbIterator<Subscription> accountSubscriptions = subscriptionService.getSubscriptionsByParticipant(account.getId());

        while (accountSubscriptions.hasNext()) {
            subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
        }

        response.add(SUBSCRIPTIONS_RESPONSE, subscriptions);
        return response;
    }
}
