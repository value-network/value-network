package val.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import val.services.ATService;
import val.util.Convert;

import javax.servlet.http.HttpServletRequest;

import static val.http.common.ResultFields.AT_IDS_RESPONSE;

final class GetATIds extends APIServlet.APIRequestHandler {

    private final ATService atService;

    GetATIds(ATService atService) {
        super(new APITag[]{APITag.AT});
        this.atService = atService;
    }

    @Override
    JsonElement processRequest(HttpServletRequest req) {

        JsonArray atIds = new JsonArray();
        for (Long id : atService.getAllATIds()) {
            atIds.add(Convert.toUnsignedLong(id));
        }

        JsonObject response = new JsonObject();
        response.add(AT_IDS_RESPONSE, atIds);
        return response;
    }

}
