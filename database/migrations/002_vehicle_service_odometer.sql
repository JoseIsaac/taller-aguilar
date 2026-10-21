USE taller_diesel;

SET @service_odometer_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND COLUMN_NAME = 'service_odometer'
  ),
  'SELECT ''vehicle.service_odometer already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD COLUMN service_odometer BIGINT NULL AFTER odometer'
);

PREPARE service_odometer_statement FROM @service_odometer_ddl;
EXECUTE service_odometer_statement;
DEALLOCATE PREPARE service_odometer_statement;
