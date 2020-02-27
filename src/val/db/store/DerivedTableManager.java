package val.db.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import val.db.DerivedTable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DerivedTableManager {

    private final Logger logger = LoggerFactory.getLogger(DerivedTableManager.class);

    private final List<DerivedTable> derivedTables = new CopyOnWriteArrayList<>();

    public List<DerivedTable> getDerivedTables() {
        return derivedTables;
    }

    public void registerDerivedTable(DerivedTable table) {
        logger.info("Registering derived table " + table.getClass());
        derivedTables.add(table);
    }

}
