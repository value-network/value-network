package val.fluxcapacitor;

import java.util.Arrays;
import java.util.List;

class FluxHistory<T> {

    private final T defaultValue;
    private final List<Element<T>> history;

    @SafeVarargs
    FluxHistory(T defaultValue, Element<T>... history) {
        this.defaultValue = defaultValue;

        this.history = Arrays.asList(history);
    }

    T getDefaultValue() {
        return defaultValue;
    }

    List<Element<T>> getHistory() {
        return history;
    }

    static class Element<T> {

        private final HistoricalMoments moment;
        private final T value;

        public Element(HistoricalMoments moment, T value) {
            this.moment = moment;
            this.value = value;
        }


        public HistoricalMoments getMoment() {
            return moment;
        }

        T getValue() {
            return value;
        }
    }
}
