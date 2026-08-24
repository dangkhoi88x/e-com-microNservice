package com.example.inventoryservice.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reconciles local databases created before inventory supported product variants.
 * Spring's SQL initializer splits PostgreSQL DO blocks on semicolons, so this
 * dynamic migration is intentionally executed through JdbcTemplate instead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventorySchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void removeLegacyProductUniqueConstraint() {
        List<String> constraintNames = jdbcTemplate.queryForList("""
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = tc.constraint_name
                 AND ccu.table_schema = tc.table_schema
                WHERE tc.table_schema = current_schema()
                  AND tc.table_name = 'inventories'
                  AND tc.constraint_type = 'UNIQUE'
                GROUP BY tc.constraint_name
                HAVING array_agg(ccu.column_name::text ORDER BY ccu.column_name) = ARRAY['product_id']::text[]
                """, String.class);

        for (String constraintName : constraintNames) {
            jdbcTemplate.execute("ALTER TABLE inventories DROP CONSTRAINT " + quoteIdentifier(constraintName));
            log.info("Removed legacy unique inventory constraint={}", constraintName);
        }

        List<String> indexNames = jdbcTemplate.queryForList("""
                SELECT indexrelid::regclass::text
                FROM pg_index
                WHERE indrelid = 'inventories'::regclass
                  AND indisunique
                  AND NOT indisprimary
                  AND indpred IS NULL
                  AND indkey::smallint[] = ARRAY[
                      (SELECT attnum FROM pg_attribute
                       WHERE attrelid = 'inventories'::regclass AND attname = 'product_id')
                  ]::smallint[]
                """, String.class);

        for (String indexName : indexNames) {
            jdbcTemplate.execute("DROP INDEX " + indexName);
            log.info("Removed legacy unique inventory index={}", indexName);
        }
    }

    private String quoteIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }
}
