CREATE DATABASE IF NOT EXISTS taller_diesel
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE taller_diesel;

CREATE TABLE IF NOT EXISTS app_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  role ENUM('ADMINISTRADOR', 'TALLER', 'OBSERVADOR') NOT NULL DEFAULT 'OBSERVADOR',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL DEFAULT NULL,
  INDEX idx_app_user_active_deleted (active, deleted_at)
);

CREATE TABLE IF NOT EXISTS mechanic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(140) NOT NULL,
  kind ENUM('INTERNO', 'EXTERNO') NOT NULL DEFAULT 'INTERNO',
  specialty VARCHAR(140) NOT NULL,
  phone VARCHAR(40),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL DEFAULT NULL,
  INDEX idx_mechanic_active_deleted (active, deleted_at)
);

CREATE TABLE IF NOT EXISTS unit_type (
  id TINYINT UNSIGNED PRIMARY KEY,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL UNIQUE
);

INSERT IGNORE INTO unit_type (id, code, name) VALUES
  (1, 'TRACTOCAMION', 'Tractocamion'),
  (2, 'TANQUE', 'Tanque');

CREATE TABLE IF NOT EXISTS vehicle (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  unit_number VARCHAR(50) NOT NULL,
  unit_type_id TINYINT UNSIGNED NOT NULL DEFAULT 1,
  make VARCHAR(100),
  model VARCHAR(100),
  year INT,
  plate VARCHAR(30),
  odometer BIGINT,
  service_odometer BIGINT,
  last_service_odometer BIGINT,
  last_service_date DATE,
  service_interval_km BIGINT,
  location VARCHAR(120),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL DEFAULT NULL,
  INDEX idx_vehicle_active_deleted (active, deleted_at),
  UNIQUE KEY uq_vehicle_unit_number_type (unit_number, unit_type_id),
  CONSTRAINT fk_vehicle_unit_type FOREIGN KEY (unit_type_id) REFERENCES unit_type(id)
);

CREATE TABLE IF NOT EXISTS maintenance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  vehicle_id BIGINT NOT NULL,
  mechanic_id BIGINT,
  observation TEXT NOT NULL,
  status ENUM('TRABAJANDO', 'TERMINADO') NOT NULL DEFAULT 'TRABAJANDO',
  start_date DATE NOT NULL,
  end_date DATE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at TIMESTAMP NULL DEFAULT NULL,
  INDEX idx_maintenance_active_deleted (active, deleted_at),
  INDEX idx_maintenance_vehicle (vehicle_id),
  INDEX idx_maintenance_mechanic (mechanic_id),
  CONSTRAINT fk_maintenance_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicle(id),
  CONSTRAINT fk_maintenance_mechanic FOREIGN KEY (mechanic_id) REFERENCES mechanic(id)
);

-- Cuenta únicamente para desarrollo local. Cambia la contraseña antes de usarla.
INSERT INTO app_user (username, password, display_name, role)
SELECT 'admin', 'admin123', 'Administrador', 'ADMINISTRADOR'
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE username = 'admin');

INSERT INTO mechanic (name, kind, specialty, phone)
SELECT 'Mecánico de ejemplo', 'INTERNO', 'Mantenimiento general', '5550000001'
WHERE NOT EXISTS (SELECT 1 FROM mechanic WHERE name = 'Mecánico de ejemplo');

INSERT INTO vehicle (unit_number, unit_type_id, make, model, year, plate, odometer, location)
SELECT 'U-001', 1, 'Freightliner', 'M2', 2022, 'ABC-123-A', 25000, 'Taller principal'
WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE unit_number = 'U-001');
