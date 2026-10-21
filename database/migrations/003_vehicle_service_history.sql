USE taller_diesel;

SET @last_service_odometer_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND COLUMN_NAME = 'last_service_odometer'
  ),
  'SELECT ''vehicle.last_service_odometer already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD COLUMN last_service_odometer BIGINT NULL AFTER odometer'
);
PREPARE last_service_odometer_statement FROM @last_service_odometer_ddl;
EXECUTE last_service_odometer_statement;
DEALLOCATE PREPARE last_service_odometer_statement;

SET @last_service_date_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND COLUMN_NAME = 'last_service_date'
  ),
  'SELECT ''vehicle.last_service_date already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD COLUMN last_service_date DATE NULL AFTER last_service_odometer'
);
PREPARE last_service_date_statement FROM @last_service_date_ddl;
EXECUTE last_service_date_statement;
DEALLOCATE PREPARE last_service_date_statement;

SET @service_interval_km_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND COLUMN_NAME = 'service_interval_km'
  ),
  'SELECT ''vehicle.service_interval_km already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD COLUMN service_interval_km BIGINT NULL AFTER last_service_date'
);
PREPARE service_interval_km_statement FROM @service_interval_km_ddl;
EXECUTE service_interval_km_statement;
DEALLOCATE PREPARE service_interval_km_statement;
