USE taller_diesel;

SET @requires_part_column_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'maintenance'
      AND column_name = 'requires_part'
  ),
  'SELECT ''maintenance.requires_part already exists'' AS migration_status',
  'ALTER TABLE maintenance ADD COLUMN requires_part BOOLEAN NOT NULL DEFAULT FALSE'
);
PREPARE requires_part_column_statement FROM @requires_part_column_sql;
EXECUTE requires_part_column_statement;
DEALLOCATE PREPARE requires_part_column_statement;

SET @part_description_column_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'maintenance'
      AND column_name = 'part_description'
  ),
  'SELECT ''maintenance.part_description already exists'' AS migration_status',
  'ALTER TABLE maintenance ADD COLUMN part_description VARCHAR(500) NULL'
);
PREPARE part_description_column_statement FROM @part_description_column_sql;
EXECUTE part_description_column_statement;
DEALLOCATE PREPARE part_description_column_statement;
