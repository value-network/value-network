package val.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.Constants;
import val.Token;
import val.services.TimeService;

import javax.servlet.http.HttpServletRequest;

import static val.http.JSONResponses.*;
import static val.http.common.Parameters.SECRET_PHRASE_PARAMETER;

final class GenerateToken extends APIServlet.APIRequestHandler {

    private final TimeService timeService;

    GenerateToken(TimeService timeService) {
        super(new APITag[]{APITag.TOKENS}, Constants.WEBSITE, SECRET_PHRASE_PARAMETER);
        this.timeService = timeService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
        String website = req.getParameter(Constants.WEBSITE);
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        } else if (website == null) {
            return MISSING_WEBSITE;
        }

        try {

            String tokenString = Token.generateToken(secretPhrase, website.trim(), timeService.getEpochTime());

            JsonObject response = new JsonObject();
            response.addProperty(Constants.TOKEN, tokenString);

            return response;

        } catch (RuntimeException e) {
            return INCORRECT_WEBSITE;
        }

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
