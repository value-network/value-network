package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.feesuggestions.FeeSuggestion;
import val.feesuggestions.FeeSuggestionCalculator;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.*;

public class SuggestFee extends APIServlet.APIRequestHandler {

    private final FeeSuggestionCalculator feeSuggestionCalculator;

    public SuggestFee(FeeSuggestionCalculator feeSuggestionCalculator) {
        super(new APITag[]{APITag.FEES});
        this.feeSuggestionCalculator = feeSuggestionCalculator;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {
        final FeeSuggestion feeSuggestion = feeSuggestionCalculator.giveFeeSuggestion();

        final JsonObject response = new JsonObject();

        response.addProperty(CHEAP_FEE_RESPONSE, feeSuggestion.getCheapFee());
        response.addProperty(STANDARD_FEE_RESPONSE, feeSuggestion.getStandardFee());
        response.addProperty(PRIORITY_FEE_RESPONSE, feeSuggestion.getPriorityFee());

        return response;
    }

}
