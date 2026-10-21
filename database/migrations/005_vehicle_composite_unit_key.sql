USE taller_diesel;

SET @old_unit_number_unique_index = (
  SELECT index_name
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'vehicle'
    AND non_unique = 0
  GROUP BY index_name
  HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') = 'unit_number'
  LIMIT 1
);

SET @drop_old_unit_number_index_sql = IF(
  @old_unit_number_unique_index IS NULL,
  'SELECT ''No single-column unit_number unique index to remove'' AS migration_status',
  CONCAT('ALTER TABLE vehicle DROP INDEX `', REPLACE(@old_unit_number_unique_index, '`', '``'), '`')
);
PREPARE drop_old_unit_number_index_statement FROM @drop_old_unit_number_index_sql;
EXECUTE drop_old_unit_number_index_statement;
DEALLOCATE PREPARE drop_old_unit_number_index_statement;

SET @composite_unit_key_sql = IF(
  EXISTS (
    SELECT 1
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'vehicle'
      AND non_unique = 0
    GROUP BY index_name
    HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',') = 'unit_number,unit_type_id'
  ),
  'SELECT ''Composite unit number/type key already exists'' AS migration_status',
  'ALTER TABLE vehicle ADD UNIQUE KEY uq_vehicle_unit_number_type (unit_number, unit_type_id)'
);
PREPARE composite_unit_key_statement FROM @composite_unit_key_sql;
EXECUTE composite_unit_key_statement;
DEALLOCATE PREPARE composite_unit_key_statement;
