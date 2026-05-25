package com.guerrero.Inventario;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println("Executing manual database migration for precio_compra...");
            stmt.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS precio_compra double precision DEFAULT 0.0 NOT NULL");
            stmt.execute("ALTER TABLE detalle_venta ADD COLUMN IF NOT EXISTS precio_compra double precision");
            System.out.println("Database migration completed successfully!");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
        }
    }
}
