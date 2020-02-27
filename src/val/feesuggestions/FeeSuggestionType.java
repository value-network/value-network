package val.feesuggestions;

import java.util.Arrays;

public enum FeeSuggestionType {
    CHEAP("cheap"), STANDARD("standard"), PRIORITY("priority");

    private final String type;

    FeeSuggestionType(String type) {
        this.type = type;
    }

    public static FeeSuggestionType getByType(String type) {
        return Arrays.stream(values()).filter(s -> s.type.equals(type)).findFirst().orElse(null);
    }

    public String getType() {
        return type;
    }
}
