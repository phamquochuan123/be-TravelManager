package com.example.travelManager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrationConfig {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateStatusColumns() {
        migrateColumn("tour_bookings", "status", "VARCHAR(20)");
        migrateColumn("restaurant_bookings", "status", "VARCHAR(20)");
        migrateColumn("booked_rooms", "status", "VARCHAR(20)");
    }

    private void migrateColumn(String table, String column, String newType) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
            if (!rs.next()) return;

            String typeName = rs.getString("TYPE_NAME");
            int size = rs.getInt("COLUMN_SIZE");

            boolean needsMigration = typeName.equalsIgnoreCase("ENUM")
                    || (typeName.equalsIgnoreCase("VARCHAR") && size < 20);

            if (needsMigration) {
                stmt.execute(String.format(
                        "ALTER TABLE %s MODIFY COLUMN %s %s NOT NULL DEFAULT 'PENDING'",
                        table, column, newType));
                log.info("Migrated {}.{} from {}({}) to {}", table, column, typeName, size, newType);
            }
        } catch (Exception e) {
            log.warn("Could not migrate {}.{}: {}", table, column, e.getMessage());
        }
    }
}
