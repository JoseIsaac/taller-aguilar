package mx.tallerdiesel;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
  @CrossOrigin(origins = {"${FRONTEND_ORIGIN:http://localhost:4200}", "http://127.0.0.1:4200"})
public class ApiController {
  private final JdbcTemplate db;
  public ApiController(JdbcTemplate db) { this.db = db; }

  @GetMapping("/health") public Map<String,String> health() { return Map.of("status", "ok", "service", "taller-api"); }

  @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Map<String,String> body) {
    try {
      return db.queryForMap("SELECT id,username,display_name AS name,role FROM app_user WHERE username=? AND password=? AND active=TRUE", body.get("username"), body.get("password"));
    } catch (EmptyResultDataAccessException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"); }
  }

  @GetMapping("/unit-types") public List<Map<String,Object>> unitTypes() { return db.queryForList("SELECT id,code,name FROM unit_type ORDER BY id"); }
  @GetMapping("/units") public List<Map<String,Object>> units() { return db.queryForList("SELECT v.id,v.unit_number AS code,v.unit_type_id AS unitTypeId,t.code AS unitTypeCode,t.name AS unitTypeName,v.make,v.model,v.year,v.plate,v.odometer AS km,v.last_service_odometer AS lastServiceKm,v.last_service_date AS lastServiceDate,v.service_interval_km AS serviceIntervalKm,CASE WHEN v.last_service_odometer IS NOT NULL AND v.service_interval_km IS NOT NULL THEN v.last_service_odometer + v.service_interval_km ELSE NULL END AS nextServiceKm,v.location,v.active FROM vehicle v JOIN unit_type t ON t.id=v.unit_type_id ORDER BY v.active DESC,v.unit_number"); }
  @PostMapping("/units") public Map<String,Object> createUnit(@RequestBody Map<String,Object> b) {
    String code = requiredText(b, "code");
    int unitTypeId = requiredInteger(b, "unitTypeId");
    boolean tank = "TANQUE".equals(unitTypeCode(unitTypeId));
    String make = requiredText(b, "make");
    String plate = tank ? requiredText(b, "plate") : optionalText(b, "plate");
    Integer year = tank ? requiredInteger(b, "year") : optionalInteger(b, "year");
    validateTankYear(tank, year);
    try {
      db.update("INSERT INTO vehicle(unit_number,unit_type_id,make,model,year,plate,odometer,last_service_odometer,last_service_date,service_interval_km,location) VALUES(?,?,?,?,?,?,?,?,?,?,?)", code, unitTypeId, make, optionalText(b, "model"), year, plate, tank ? null : optionalLong(b, "km"), tank ? null : optionalLong(b, "lastServiceKm"), optionalDate(b, "lastServiceDate"), tank ? null : optionalPositiveLong(b, "serviceIntervalKm"), optionalText(b, "location"));
    } catch (DuplicateKeyException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una unidad con ese número y tipo");
    }
    return Map.of("status", "created");
  }
  @PutMapping("/units/{id}") public Map<String,String> updateUnit(@PathVariable long id, @RequestBody Map<String,Object> b) {
    String code = requiredText(b, "code");
    int unitTypeId = requiredInteger(b, "unitTypeId");
    boolean tank = "TANQUE".equals(unitTypeCode(unitTypeId));
    String make = requiredText(b, "make");
    String plate = tank ? requiredText(b, "plate") : optionalText(b, "plate");
    Integer year = tank ? requiredInteger(b, "year") : optionalInteger(b, "year");
    validateTankYear(tank, year);
    try {
      Integer exists = db.queryForObject("SELECT COUNT(*) FROM vehicle WHERE id=? AND active=TRUE AND deleted_at IS NULL", Integer.class, id);
      if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad activa no encontrada");
      db.update("UPDATE vehicle SET unit_number=?,unit_type_id=?,make=?,model=?,year=?,plate=?,odometer=?,last_service_odometer=?,last_service_date=?,service_interval_km=?,location=? WHERE id=? AND active=TRUE AND deleted_at IS NULL", code, unitTypeId, make, optionalText(b, "model"), year, plate, tank ? null : optionalLong(b, "km"), tank ? null : optionalLong(b, "lastServiceKm"), optionalDate(b, "lastServiceDate"), tank ? null : optionalPositiveLong(b, "serviceIntervalKm"), optionalText(b, "location"), id);
    } catch (DuplicateKeyException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una unidad con ese número y tipo");
    }
    return Map.of("status", "updated");
  }
  @DeleteMapping("/units/{id}") public Map<String,String> deactivateUnit(@PathVariable long id) {
    int updated = db.update("UPDATE vehicle SET active=FALSE,deleted_at=CURRENT_TIMESTAMP WHERE id=? AND active=TRUE AND deleted_at IS NULL", id);
    if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad activa no encontrada");
    return Map.of("status", "deactivated");
  }
  @PostMapping("/units/{id}/restore") public Map<String,String> restoreUnit(@PathVariable long id) {
    int updated = db.update("UPDATE vehicle SET active=TRUE,deleted_at=NULL WHERE id=? AND active=FALSE", id);
    if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad inactiva no encontrada");
    return Map.of("status", "restored");
  }
  @GetMapping("/mechanics") public List<Map<String,Object>> mechanics() { return db.queryForList("SELECT id,name,kind AS type,specialty FROM mechanic WHERE active=TRUE AND deleted_at IS NULL ORDER BY name"); }
  @PostMapping("/mechanics") public Map<String,Object> createMechanic(@RequestBody Map<String,Object> b) {
    String name = requiredText(b, "name");
    String type = validMechanicType(requiredText(b, "type"));
    String specialty = requiredText(b, "specialty");
    db.update("INSERT INTO mechanic(name,kind,specialty) VALUES(?,?,?)", name, type, specialty);
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @PutMapping("/mechanics/{id}") public Map<String,String> updateMechanic(@PathVariable long id, @RequestBody Map<String,Object> b) {
    String name = requiredText(b, "name");
    String type = validMechanicType(requiredText(b, "type"));
    String specialty = requiredText(b, "specialty");
    int updated = db.update("UPDATE mechanic SET name=?,kind=?,specialty=? WHERE id=? AND active=TRUE AND deleted_at IS NULL", name, type, specialty, id);
    if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecánico activo no encontrado");
    return Map.of("status", "updated");
  }
  @DeleteMapping("/mechanics/{id}") @Transactional public Map<String,String> deactivateMechanic(@PathVariable long id) {
    try {
      db.queryForObject("SELECT id FROM mechanic WHERE id=? AND active=TRUE AND deleted_at IS NULL FOR UPDATE", Long.class, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecánico activo no encontrado");
    }
    Integer assigned = db.queryForObject("SELECT COUNT(*) FROM maintenance WHERE mechanic_id=?", Integer.class, id);
    if (assigned != null && assigned > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede dar de baja al mecánico porque está asignado a uno o más mantenimientos");
    }
    db.update("UPDATE mechanic SET active=FALSE,deleted_at=CURRENT_TIMESTAMP WHERE id=?", id);
    return Map.of("status", "deactivated");
  }
  @GetMapping("/users") public List<Map<String,Object>> users() { return db.queryForList("SELECT id,username,display_name AS name,role,active FROM app_user ORDER BY display_name"); }
  @PostMapping("/users") public Map<String,Object> createUser(@RequestBody Map<String,Object> b) {
    String username = requiredText(b, "username");
    String password = requiredText(b, "password");
    String name = requiredText(b, "name");
    String role = validRole(b.get("role"));
    try {
      db.update("INSERT INTO app_user(username,password,display_name,role) VALUES(?,?,?,?)", username, password, name, role);
    } catch (DuplicateKeyException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está registrado");
    }
    return Map.of("status", "created");
  }
  @PutMapping("/users/{id}") @Transactional public Map<String,String> updateUser(@PathVariable long id, @RequestBody Map<String,Object> b) {
    String name = requiredText(b, "name");
    String username = requiredText(b, "username");
    String role = validRole(b.get("role"));
    String currentRole;
    try {
      currentRole = db.queryForObject("SELECT role FROM app_user WHERE id=? AND active=TRUE", String.class, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario activo no encontrado");
    }
    if ("ADMINISTRADOR".equals(currentRole) && !"ADMINISTRADOR".equals(role)) {
      ensureAnotherActiveAdmin(id);
    }
    String password = b.get("password") == null ? "" : b.get("password").toString().trim();
    try {
      if (password.isEmpty()) {
        db.update("UPDATE app_user SET username=?,display_name=?,role=? WHERE id=? AND active=TRUE", username, name, role, id);
      } else {
        db.update("UPDATE app_user SET username=?,display_name=?,role=?,password=? WHERE id=? AND active=TRUE", username, name, role, password, id);
      }
    } catch (DuplicateKeyException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está registrado");
    }
    return Map.of("status", "updated");
  }
  @DeleteMapping("/users/{id}") @Transactional public Map<String,String> deactivateUser(@PathVariable long id) {
    String role;
    try {
      role = db.queryForObject("SELECT role FROM app_user WHERE id=? AND active=TRUE", String.class, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario activo no encontrado");
    }
    if ("ADMINISTRADOR".equals(role)) ensureAnotherActiveAdmin(id);
    db.update("UPDATE app_user SET active=FALSE,deleted_at=CURRENT_TIMESTAMP WHERE id=?", id);
    return Map.of("status", "deactivated");
  }
  @PostMapping("/users/{id}/restore") public Map<String,String> restoreUser(@PathVariable long id) {
    int updated = db.update("UPDATE app_user SET active=TRUE,deleted_at=NULL WHERE id=? AND active=FALSE", id);
    if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario inactivo no encontrado");
    return Map.of("status", "restored");
  }
  @GetMapping("/maintenances") public List<Map<String,Object>> maintenances() { return db.queryForList("SELECT m.id,m.vehicle_id AS unitId,m.mechanic_id AS mechanicId,COALESCE(me.name,'') AS mechanic,v.unit_number AS unit,m.observation AS note,m.requires_part AS requiresPart,m.part_description AS partDescription,m.status,m.start_date AS start,m.end_date AS due FROM maintenance m JOIN vehicle v ON v.id=m.vehicle_id LEFT JOIN mechanic me ON me.id=m.mechanic_id WHERE m.active=TRUE AND m.deleted_at IS NULL ORDER BY m.start_date DESC,m.id DESC"); }
  @PostMapping("/maintenances") @Transactional public Map<String,Object> createMaintenance(@RequestBody Map<String,Object> b) {
    int unitId = requiredInteger(b, "unitId");
    ensureActiveVehicle(unitId);
    String note = requiredText(b, "note");
    java.sql.Date start = requiredDate(b, "start");
    boolean requiresPart = optionalBoolean(b, "requiresPart");
    String partDescription = requiresPart ? requiredText(b, "partDescription") : null;
    Long mechanicId = optionalMechanicId(b);
    lockActiveMechanic(mechanicId);
    db.update("INSERT INTO maintenance(vehicle_id,mechanic_id,observation,requires_part,part_description,status,start_date) VALUES(?,?, ?,?,?, 'TRABAJANDO',?)", unitId, mechanicId, note, requiresPart, partDescription, start);
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @PutMapping("/maintenances/{id}") @Transactional public Map<String,String> updateMaintenance(@PathVariable long id, @RequestBody Map<String,Object> b) {
    int unitId = requiredInteger(b, "unitId");
    ensureActiveVehicle(unitId);
    String note = requiredText(b, "note");
    java.sql.Date start = requiredDate(b, "start");
    String status = validMaintenanceStatus(requiredText(b, "status"));
    boolean requiresPart = optionalBoolean(b, "requiresPart");
    String partDescription = requiresPart ? requiredText(b, "partDescription") : null;
    Long mechanicId = optionalMechanicId(b);
    lockActiveMechanic(mechanicId);
    int n = db.update("UPDATE maintenance SET vehicle_id=?,mechanic_id=?,observation=?,requires_part=?,part_description=?,status=?,start_date=?,end_date=? WHERE id=? AND active=TRUE AND deleted_at IS NULL", unitId, mechanicId, note, requiresPart, partDescription, status, start, "TERMINADO".equals(status) ? java.time.LocalDate.now() : null, id);
    if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado");
    return Map.of("status", "updated");
  }

  private String requiredText(Map<String,Object> body, String key) {
    Object value = body.get(key);
    if (value == null || value.toString().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " es obligatorio");
    }
    return value.toString().trim();
  }

  private String validRole(Object value) {
    if (value == null || !List.of("ADMINISTRADOR", "TALLER", "OBSERVADOR").contains(value.toString())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol de usuario no válido");
    }
    return value.toString();
  }

  private String optionalText(Map<String,Object> body, String key) {
    Object value = body.get(key);
    return value == null || value.toString().isBlank() ? null : value.toString().trim();
  }

  private Integer optionalInteger(Map<String,Object> body, String key) {
    String value = optionalText(body, key);
    if (value == null) return null;
    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " debe ser un número entero");
    }
  }

  private int requiredInteger(Map<String,Object> body, String key) {
    Integer value = optionalInteger(body, key);
    if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " es obligatorio");
    return value;
  }

  private String validMechanicType(String type) {
    if (!List.of("INTERNO", "EXTERNO").contains(type)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de mecánico debe ser Interno o Externo");
    }
    return type;
  }

  private Long optionalMechanicId(Map<String,Object> body) {
    Object value = body.get("mechanicId");
    if (value == null || value.toString().isBlank()) return null;
    try {
      long id = Long.parseLong(value.toString());
      if (id <= 0) throw new NumberFormatException();
      return id;
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del mecánico debe ser un entero positivo");
    }
  }

  private boolean optionalBoolean(Map<String,Object> body, String key) {
    Object value = body.get(key);
    if (value == null) return false;
    if (value instanceof Boolean booleanValue) return booleanValue;
    if ("true".equalsIgnoreCase(value.toString())) return true;
    if ("false".equalsIgnoreCase(value.toString())) return false;
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " debe ser verdadero o falso");
  }

  private java.sql.Date requiredDate(Map<String,Object> body, String key) {
    java.sql.Date value = optionalDate(body, key);
    if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " es obligatorio");
    return value;
  }

  private String validMaintenanceStatus(String status) {
    if (!List.of("TRABAJANDO", "TERMINADO").contains(status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado del mantenimiento no es válido");
    }
    return status;
  }

  private void ensureActiveVehicle(int id) {
    Integer count = db.queryForObject("SELECT COUNT(*) FROM vehicle WHERE id=? AND active=TRUE AND deleted_at IS NULL", Integer.class, id);
    if (count == null || count == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La unidad no existe o está dada de baja");
    }
  }

  private void lockActiveMechanic(Long id) {
    if (id == null) return;
    try {
      db.queryForObject("SELECT id FROM mechanic WHERE id=? AND active=TRUE AND deleted_at IS NULL FOR UPDATE", Long.class, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mecánico asignado no existe o está dado de baja");
    }
  }

  private void validateTankYear(boolean tank, Integer year) {
    if (tank && (year < 1900 || year > 2100)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año de un tanque debe estar entre 1900 y 2100");
    }
  }

  private String unitTypeCode(int id) {
    try {
      return db.queryForObject("SELECT code FROM unit_type WHERE id=?", String.class, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de unidad no válido");
    }
  }

  private Long optionalLong(Map<String,Object> body, String key) {
    String value = optionalText(body, key);
    if (value == null) return null;
    try {
      long number = Long.parseLong(value);
      if (number < 0) throw new NumberFormatException();
      return number;
    } catch (NumberFormatException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " debe ser un entero positivo o cero");
    }
  }

  private Long optionalPositiveLong(Map<String,Object> body, String key) {
    Long value = optionalLong(body, key);
    if (value != null && value == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " debe ser mayor que cero");
    }
    return value;
  }

  private java.sql.Date optionalDate(Map<String,Object> body, String key) {
    String value = optionalText(body, key);
    if (value == null) return null;
    try {
      return java.sql.Date.valueOf(value);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo " + key + " debe ser una fecha válida (AAAA-MM-DD)");
    }
  }

  private void ensureAnotherActiveAdmin(long excludedId) {
    Integer count = db.queryForObject(
        "SELECT COUNT(*) FROM app_user WHERE role='ADMINISTRADOR' AND active=TRUE AND id<>?",
        Integer.class, excludedId);
    if (count == null || count == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar al menos un administrador activo");
    }
  }
}
