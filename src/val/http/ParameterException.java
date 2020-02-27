package val.http;

import com.google.gson.JsonElement;
import val.ValException;

public final class ParameterException extends ValException {

    private final JsonElement errorResponse;

    public ParameterException(JsonElement errorResponse) {
        this.errorResponse = errorResponse;
    }

    JsonElement getErrorResponse() {
        return errorResponse;
    }

}
