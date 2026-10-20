package mx.tallerdiesel;

import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
  @CrossOrigin(origins = {"${FRONTEND_ORIGIN:http://localhost:4200}"})
public class ApiController {
  private final JdbcTemplate db;
  public ApiController(JdbcTemplate db) { this.db = db; }

  @GetMapping("/health") public Map<String,String> health() { return Map.of("status", "ok", "service", "taller-api"); }

  @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Map<String,String> body) {
    try {
      return db.queryForMap("SELECT id,username,display_name AS name,role FROM app_user WHERE username=? AND password=? AND active=TRUE", body.get("username"), body.get("password"));
    } catch (EmptyResultDataAccessException ex) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"); }
  }

  @GetMapping("/units") public List<Map<String,Object>> units() { return db.queryForList("SELECT id,unit_number AS code,make,model,year,plate,odometer AS km,location FROM vehicle WHERE active=TRUE ORDER BY unit_number"); }
  @PostMapping("/units") public Map<String,Object> createUnit(@RequestBody Map<String,Object> b) {
    db.update("INSERT INTO vehicle(unit_number,make,model,year,plate,odometer,location) VALUES(?,?,?,?,?,?,?)", b.get("code"), b.get("make"), b.get("model"), b.get("year"), b.get("plate"), b.get("km"), b.get("location"));
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @GetMapping("/mechanics") public List<Map<String,Object>> mechanics() { return db.queryForList("SELECT id,name,kind AS type,specialty,phone FROM mechanic WHERE active=TRUE ORDER BY name"); }
  @PostMapping("/mechanics") public Map<String,Object> createMechanic(@RequestBody Map<String,Object> b) {
    db.update("INSERT INTO mechanic(name,kind,specialty,phone) VALUES(?,?,?,?)", b.get("name"), b.getOrDefault("kind","INTERNO"), b.get("specialty"), b.get("phone"));
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @GetMapping("/users") public List<Map<String,Object>> users() { return db.queryForList("SELECT id,username,display_name AS name,role,active FROM app_user ORDER BY display_name"); }
  @PostMapping("/users") public Map<String,Object> createUser(@RequestBody Map<String,Object> b) {
    db.update("INSERT INTO app_user(username,password,display_name,role) VALUES(?,?,?,?)", b.get("username"), b.get("password"), b.get("name"), b.get("role"));
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @GetMapping("/maintenances") public List<Map<String,Object>> maintenances() { return db.queryForList("SELECT m.id,m.vehicle_id AS unitId,m.mechanic_id AS mechanicId,v.unit_number AS unit,m.observation AS note,m.status,m.start_date AS start,m.end_date AS end FROM maintenance m JOIN vehicle v ON v.id=m.vehicle_id ORDER BY m.start_date DESC,m.id DESC"); }
  @PostMapping("/maintenances") public Map<String,Object> createMaintenance(@RequestBody Map<String,Object> b) {
    db.update("INSERT INTO maintenance(vehicle_id,mechanic_id,observation,status,start_date) VALUES(?,?,?,'TRABAJANDO',?)", b.get("unitId"), b.get("mechanicId"), b.get("note"), b.get("start"));
    return Map.of("id", db.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
  }
  @PutMapping("/maintenances/{id}") @Transactional public Map<String,String> updateMaintenance(@PathVariable long id, @RequestBody Map<String,Object> b) {
    int n = db.update("UPDATE maintenance SET vehicle_id=?,mechanic_id=?,observation=?,status=?,start_date=?,end_date=? WHERE id=?", b.get("unitId"), b.get("mechanicId"), b.get("note"), b.get("status"), b.get("start"), "TERMINADO".equals(b.get("status")) ? b.getOrDefault("end", java.time.LocalDate.now().toString()) : null, id);
    if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado");
    return Map.of("status", "updated");
  }
}
