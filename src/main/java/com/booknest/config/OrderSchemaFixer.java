package com.booknest.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes the stale unique index on {@code orders.address_id}.
 * <p>
 * {@code Order.shippingAddress} used to be mapped {@code @OneToOne}, for which
 * Hibernate generates a unique foreign key, so a second order shipped to the
 * same saved address failed with a constraint violation. The mapping is now
 * {@code @ManyToOne}, but {@code ddl-auto=update} never drops constraints, so
 * databases created under the old mapping still carry it.
 * <p>
 * The foreign key on the column shares that unique index, so on MySQL a plain
 * index is added first for the foreign key to move onto; H2 cannot move it, so
 * there the foreign key is dropped and re-created around the index drop.
 * <p>
 * Best effort only (see the note on runtime DDL in {@link DataInitializer}):
 * any failure, e.g. a database user without ALTER privileges, is logged and
 * startup continues.
 */
@Component
@RequiredArgsConstructor
public class OrderSchemaFixer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderSchemaFixer.class);

    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String table = meta.storesUpperCaseIdentifiers() ? "ORDERS" : "orders";
            String catalog = conn.getCatalog();
            String schema = conn.getSchema();

            Set<String> uniqueIndexes = uniqueIndexesOnAddressId(meta, catalog, schema, table);
            if (uniqueIndexes.isEmpty()) {
                return;
            }

            boolean mysql = meta.getDatabaseProductName().toLowerCase().contains("mysql");
            try (Statement st = conn.createStatement()) {
                if (mysql) {
                    st.execute("CREATE INDEX idx_orders_address_id ON orders (address_id)");
                    for (String index : uniqueIndexes) {
                        st.execute("ALTER TABLE orders DROP INDEX `" + index + "`");
                    }
                } else {
                    Map<String, String> foreignKeys = foreignKeysOnAddressId(meta, catalog, schema, table);
                    for (String fk : foreignKeys.keySet()) {
                        st.execute("ALTER TABLE orders DROP CONSTRAINT \"" + fk + "\"");
                    }
                    for (String constraint : uniqueConstraintsOnAddressId(conn)) {
                        st.execute("ALTER TABLE orders DROP CONSTRAINT IF EXISTS \"" + constraint + "\"");
                    }
                    for (String index : uniqueIndexes) {
                        st.execute("DROP INDEX IF EXISTS \"" + index + "\"");
                    }
                    for (Map.Entry<String, String> fk : foreignKeys.entrySet()) {
                        st.execute("ALTER TABLE orders ADD CONSTRAINT \"" + fk.getKey()
                                + "\" FOREIGN KEY (address_id) REFERENCES " + fk.getValue() + " (id)");
                    }
                }
            }
            log.info("Removed stale unique index {} on orders.address_id", uniqueIndexes);
        } catch (Exception e) {
            log.warn("Could not remove the unique index on orders.address_id; repeat orders to the " +
                    "same saved address may fail until it is dropped manually", e);
        }
    }

    /** Names of unique indexes whose only column is address_id. */
    private Set<String> uniqueIndexesOnAddressId(DatabaseMetaData meta, String catalog, String schema,
                                                  String table) throws SQLException {
        Map<String, Integer> columnCounts = new HashMap<>();
        Set<String> result = new LinkedHashSet<>();
        try (ResultSet rs = meta.getIndexInfo(catalog, schema, table, true, false)) {
            while (rs.next()) {
                String index = rs.getString("INDEX_NAME");
                String column = rs.getString("COLUMN_NAME");
                if (index == null || column == null || rs.getBoolean("NON_UNIQUE")) {
                    continue;
                }
                columnCounts.merge(index, 1, Integer::sum);
                if ("address_id".equalsIgnoreCase(column)) {
                    result.add(index);
                }
            }
        }
        result.removeIf(index -> columnCounts.get(index) != 1);
        return result;
    }

    /** Foreign keys on address_id, name to referenced table. */
    private Map<String, String> foreignKeysOnAddressId(DatabaseMetaData meta, String catalog, String schema,
                                                       String table) throws SQLException {
        Map<String, String> result = new LinkedHashMap<>();
        try (ResultSet rs = meta.getImportedKeys(catalog, schema, table)) {
            while (rs.next()) {
                if ("address_id".equalsIgnoreCase(rs.getString("FKCOLUMN_NAME"))) {
                    result.put(rs.getString("FK_NAME"), rs.getString("PKTABLE_NAME"));
                }
            }
        }
        return result;
    }

    private List<String> uniqueConstraintsOnAddressId(Connection conn) throws SQLException {
        List<String> result = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc " +
                     "JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                     "AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA AND tc.TABLE_NAME = kcu.TABLE_NAME " +
                     "WHERE tc.CONSTRAINT_TYPE = 'UNIQUE' AND UPPER(tc.TABLE_NAME) = 'ORDERS' " +
                     "AND UPPER(kcu.COLUMN_NAME) = 'ADDRESS_ID' AND tc.TABLE_SCHEMA = SCHEMA()")) {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        }
        return result;
    }
}
