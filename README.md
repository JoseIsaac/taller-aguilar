# Taller Diésel

Sistema interno para coordinar mantenimientos, unidades, personal de taller y usuarios.

## Estructura

- `frontend/`: Angular, interfaz adaptable a escritorio y móvil.
- `backend/`: API REST Java con Spring Boot y persistencia MySQL.
- `database/schema.sql`: esquema inicial MySQL.

## Inicio

Requisitos: Java 17+, Maven, Node.js 20+ y MySQL 8+.

1. Crear base de datos ejecutando `database/schema.sql`.
2. Configurar `backend/src/main/resources/application.properties` (o variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).
3. Ejecutar la API: `cd backend; mvn spring-boot:run` (puerto 8080).
4. En `frontend`, instalar dependencias con `npm install`, luego `npm start`.

La interfaz inicia con datos de muestra para revisión. La conexión persistente se implementa en backend y esquema. Antes de conectarla a un despliegue, configura credenciales y sustituye el login de demostración.

## Estado de esta primera entrega

El tablero y las altas/ediciones de unidades, mecánicos y mantenimientos se ejecutan en el navegador y guardan cambios en el almacenamiento local del navegador. El módulo de usuarios permite altas. La API Spring Boot ya expone rutas REST para login y consultas/altas/actualización de datos, pero todavía falta conectar Angular con esas rutas, completar bajas/cambios de usuarios y validar roles en el servidor. Por ello, esta entrega es un prototipo navegable y una base técnica, no debe exponerse en Internet ni usarse todavía como registro productivo.

El login y la contraseña `admin / admin123` son solo para la demostración. El ejemplo SQL guarda la contraseña sin hash; no uses datos reales ni despliegues con esta configuración. Antes de producción, hace falta agregar autenticación de servidor, autorización por rol en cada operación y almacenamiento adecuado de contraseñas.

## Roles

- Administrador: catálogos, usuarios y operación completa.
- Taller: unidades, mecánicos y mantenimientos.
- Observador: lectura del tablero.

## Despliegue sugerido

Angular puede publicarse en Cloudflare Pages, que ofrece alojamiento gratuito para archivos estáticos. Para el sistema completo sin pago mensual, una posibilidad es una máquina OCI Always Free autoadministrada con Java y MySQL; Oracle indica límites por región/capacidad y el usuario debe mantener sistema operativo, firewall, parches y respaldos. Railway facilita desplegar MySQL, pero el plan gratuito aporta solo USD 1 de crédito mensual y es adecuado para pruebas, no para operación permanente. Para uso real, presupuestar un servicio administrado con respaldos y monitoreo es más confiable.

El repositorio remoto de este proyecto es [JoseIsaac/taller-aguilar](https://github.com/JoseIsaac/taller-aguilar).
