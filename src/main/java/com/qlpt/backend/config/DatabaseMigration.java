package com.qlpt.backend.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Configuration
public class DatabaseMigration {

    @Bean
    CommandLineRunner migrateDropStaleBillingColumns(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // Drop stale columns that were removed from entities
                String[] sqls = {
                    "ALTER TABLE contracts DROP COLUMN IF EXISTS billing_mode",
                    "ALTER TABLE contracts DROP COLUMN IF EXISTS fixed_billing_day",
                    "ALTER TABLE boarding_houses DROP COLUMN IF EXISTS billing_timing"
                };
                
                for (String sql : sqls) {
                    try {
                        stmt.execute(sql);
                        System.out.println("[Migration] OK: " + sql);
                    } catch (Exception e) {
                        System.out.println("[Migration] Skipped (already done): " + sql + " -> " + e.getMessage());
                    }
                }
                
                System.out.println("[Migration] Stale billing columns cleanup complete.");
            }
        };
    }
}
