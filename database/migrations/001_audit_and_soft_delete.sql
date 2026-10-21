USE taller_diesel;

DELIMITER $$

CREATE PROCEDURE add_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition VARCHAR(255)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Expected table is missing; create the original schema first';
  END IF;

  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @add_column_sql = CONCAT(
      'ALTER TABLE `', p_table_name, '` ADD COLUMN `',
      p_column_name, '` ', p_column_definition
    );
    PREPARE add_column_statement FROM @add_column_sql;
    EXECUTE add_column_statement;
    DEALLOCATE PREPARE add_column_statement;
  END IF;
END$$

DELIMITER ;

CALL add_column_if_missing(
  'app_user', 'updated_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
);
CALL add_column_if_missing('app_user', 'deleted_at', 'TIMESTAMP NULL DEFAULT NULL');

CALL add_column_if_missing(
  'mechanic', 'created_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP'
);
CALL add_column_if_missing(
  'mechanic', 'updated_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
);
CALL add_column_if_missing('mechanic', 'deleted_at', 'TIMESTAMP NULL DEFAULT NULL');

CALL add_column_if_missing(
  'vehicle', 'created_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP'
);
CALL add_column_if_missing(
  'vehicle', 'updated_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
);
CALL add_column_if_missing('vehicle', 'deleted_at', 'TIMESTAMP NULL DEFAULT NULL');

CALL add_column_if_missing('maintenance', 'active', 'BOOLEAN NOT NULL DEFAULT TRUE');
CALL add_column_if_missing(
  'maintenance', 'updated_at',
  'TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP'
);
CALL add_column_if_missing('maintenance', 'deleted_at', 'TIMESTAMP NULL DEFAULT NULL');

DROP PROCEDURE add_column_if_missing;
