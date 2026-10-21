# Taller Diésel

Sistema interno para coordinar mantenimientos, unidades, personal de taller y usuarios.

## Estructura

- `frontend/`: Angular, interfaz adaptable a escritorio y móvil.
- `backend/`: API REST Java con Spring Boot y persistencia MySQL.
- `database/schema.sql`: esquema inicial MySQL.

## Inicio

Requisitos: Java 25 LTS+, Maven, Node.js 20+ y MySQL 8+. Java 25 puede usarse con distribuciones gratuitas como Eclipse Temurin.

1. Para una instalación nueva, ejecutar `database/schema.sql` en MySQL 8; crea las tablas e incluye datos de ejemplo para desarrollo. Si ya existe una base creada con el esquema anterior, hacer primero un respaldo y ejecutar en orden `database/migrations/001_audit_and_soft_delete.sql`, `database/migrations/002_vehicle_service_odometer.sql`, `database/migrations/003_vehicle_service_history.sql`, `database/migrations/004_vehicle_unit_type.sql`, `database/migrations/005_vehicle_composite_unit_key.sql` y `database/migrations/006_mechanic_specialty_required.sql` para conservar sus datos y actualizar el esquema. Antes de la migración 006, completar la especialidad de cualquier mecánico que actualmente no tenga una.
2. Configurar `backend/src/main/resources/application.properties` (o variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
3. Ejecutar la API: `cd backend; mvn spring-boot:run` (puerto 8080).
4. En `frontend`, instalar dependencias con `npm install`, luego `npm start`.

La gestión y autenticación de usuarios, el catálogo de unidades y el catálogo de mecánicos (incluidas sus bajas lógicas) se conectan al backend y a MySQL. No se permite dar de baja a un mecánico asignado a un mantenimiento. Los mantenimientos siguen usando datos guardados localmente en el navegador.

## Estado de esta primera entrega

El tablero y las altas/ediciones de unidades, mecánicos y mantenimientos se ejecutan en el navegador y guardan cambios en el almacenamiento local del navegador. El módulo de usuarios permite altas. La API Spring Boot ya expone rutas REST para login y consultas/altas/actualización de datos, pero todavía falta conectar Angular con esas rutas, completar bajas/cambios de usuarios y validar roles en el servidor. Por ello, esta entrega es un prototipo navegable y una base técnica, no debe exponerse en Internet ni usarse todavía como registro productivo.

El usuario y la contraseña `admin / admin123` son solo para desarrollo local. El ejemplo SQL guarda contraseñas sin hash y las rutas de la API no tienen autorización por rol; no uses datos reales ni expongas esta configuración en Internet. Antes de producción, hace falta agregar almacenamiento seguro de contraseñas y autorización de servidor en cada operación.

## Roles

- Administrador: catálogos, usuarios y operación completa.
- Taller: unidades, mecánicos y mantenimientos.
- Observador: lectura del tablero.

## Despliegue sugerido

Angular puede publicarse en Cloudflare Pages, que ofrece alojamiento gratuito para archivos estáticos. Para el sistema completo sin pago mensual, una posibilidad es una máquina OCI Always Free autoadministrada con Java y MySQL; Oracle indica límites por región/capacidad y el usuario debe mantener sistema operativo, firewall, parches y respaldos. Railway facilita desplegar MySQL, pero el plan gratuito aporta solo USD 1 de crédito mensual y es adecuado para pruebas, no para operación permanente. Para uso real, presupuestar un servicio administrado con respaldos y monitoreo es más confiable.

El repositorio remoto de este proyecto es [JoseIsaac/taller-aguilar](https://github.com/JoseIsaac/taller-aguilar).
