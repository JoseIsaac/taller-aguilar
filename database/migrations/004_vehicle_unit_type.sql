USE taller_diesel;

CREATE TABLE IF NOT EXISTS unit_type (
  id TINYINT UNSIGNED PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL UNIQUE
);

INSERT IGNORE INTO unit_type (id, code, name) VALUES
  (1, 'TRACTOCAMION', 'Tractocamion'),
  (2, 'TANQUE', 'Tanque');

SET @unit_type_id_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND COLUMN_NAME = 'unit_type_id'
  ),
  'SELECT ''vehicle.unit_type_id already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD COLUMN unit_type_id TINYINT UNSIGNED NULL DEFAULT 1 AFTER unit_number'
);
PREPARE unit_type_id_statement FROM @unit_type_id_ddl;
EXECUTE unit_type_id_statement;
DEALLOCATE PREPARE unit_type_id_statement;

UPDATE vehicle SET unit_type_id = 1 WHERE unit_type_id IS NULL;

SET @unit_type_fk_ddl = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'vehicle'
      AND CONSTRAINT_NAME = 'fk_vehicle_unit_type'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
  ),
  'SELECT ''fk_vehicle_unit_type already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD CONSTRAINT fk_vehicle_unit_type FOREIGN KEY (unit_type_id) REFERENCES unit_type(id)'
);
PREPARE unit_type_fk_statement FROM @unit_type_fk_ddl;
EXECUTE unit_type_fk_statement;
DEALLOCATE PREPARE unit_type_fk_statement;

ALTER TABLE vehicle
  MODIFY COLUMN unit_type_id TINYINT UNSIGNED NOT NULL DEFAULT 1;
