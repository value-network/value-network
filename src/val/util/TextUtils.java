package val.util;

import val.Constants;

import java.util.Locale;

public class TextUtils {
    public static boolean isInAlphabet(String input) {
        if (input == null) return true;
        for (char c : input.toLowerCase(Locale.ENGLISH).toCharArray()) {
            if (!Constants.ALPHABET.contains(String.valueOf(c))) return false;
        }
        return true;
    }
}
