# Proyecto ALPESCAB - Sistema de Transporte (Entrega 2)

Sistema de gestión de servicios de transporte desarrollado con **Spring Boot** y **SQL DATAMODELER**, implementando transaccionalidad completa y gestión de concurrencia.

---

##  Objetivos de la Entrega 2

Esta entrega se enfoca en la **implementación transaccional completa** del sistema, incluyendo:

1. **Implementación Transaccional (20%)**: Todos los RFs y RFCs implementados como transacciones en Java Spring
2. **RF8 como Transacción (20%)**: Implementación completa con validaciones y rollback automático
3. **RFC1 con Niveles de Aislamiento (20%)**: Implementación con `READ_COMMITTED` y `SERIALIZABLE`
4. **Escenarios de Concurrencia (40%)**: Pruebas de concurrencia entre RFC1 y RF8 con diferentes niveles de aislamiento
5. **Bono - Interfaz Gráfica (5%)**: Interfaz web completa para probar todos los RFs y RFCs

---

##  Configuración Inicial

### 1. Clonar o descargar el proyecto

```bash
git clone <url-del-repositorio>
cd S2_G4_2025-2
```

### 2. Configurar conexión a la base de datos

Edita el archivo `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@<host>:<puerto>:<sid>
spring.datasource.username=tu_usuario
spring.datasource.password=tu_password
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
```

**Ejemplo:**
```properties
spring.datasource.url=jdbc:oracle:thin:@localhost:1521:PROD
spring.datasource.username=ISIS2304B15202520
spring.datasource.password=tu_password
```

---

##  Estructura del Proyecto

```
S2_G4_2025-2/
├── docs/
│   ├── tablas.sql              # Script de creación de tablas (incluye columna estado)
│   ├── poblacion.sql           # Script para poblar la base de datos
│   ├── limpiar.sql             # Script para limpiar la base de datos
│   ├── GUIA-PRUEBAS-ENTREGA2.md # Guía completa de pruebas para Entrega 2
│   └── [otros archivos de documentación]
├── collections/
│   ├── RF - Proyecto Sistrans.postman_collection.json
│   ├── RFC - Proyecto Sistrans.postman_collection.json
│   └── New Environment.postman_environment.json
├── src/
│   └── main/
│       ├── java/com/sistrans/
│       │   ├── service/         # Servicios con @Transactional
│       │   ├── repository/      # Repositorios con queries SQL
│       │   ├── controller/      # Controladores REST y Web
│       │   └── ...
│       └── resources/
│           ├── templates/       # Plantillas Thymeleaf (interfaz web)
│           └── application.properties
└── pom.xml
```

---

##  Configuración de la Base de Datos

### Paso 1: Crear las tablas

1. Abre **SQL Developer**
2. Ejecuta el script `docs/tablas.sql`
3. **IMPORTANTE**: Este script incluye la columna `estado` en la tabla `USUARIO` (requerida para Entrega 2)

### Paso 2: Poblar la base de datos

1. Ejecuta el script `docs/poblacion.sql`
2. Este script crea:
   - 3 ciudades (Bogotá, Medellín, Cali)
   - 20 puntos de trayecto (IDs 1-20)
   - 100 conductores (cédulas 1001-1100) con estado `NULL` (disponibles)
   - 100 vehículos (placas AUT001-AUT100)
   - 200 pasajeros (cédulas 2001-2200)
   - 200 tarjetas de crédito
   - 200 servicios finalizados (IDs 1-200)
   - 3 servicios abiertos para RF9 (IDs 201-203)

---

## Ejecución de la Aplicación

1. Abre el proyecto en tu IDE
2. Busca la clase `SistransApplication.java`
3. Ejecuta como aplicación Java

La aplicación se ejecutará en: **http://localhost:8080**

---

##  Funcionalidades Principales de Entrega 2

### 1. Implementación Transaccional Completa (20%)

**Todos los Requisitos Funcionales (RF1-RF11) y Consultas (RFC1-RFC4) están implementados como transacciones:**

- ✅ **RF1-RF7**: Transacciones con `@Transactional` para garantizar atomicidad
- ✅ **RF8**: Transacción completa con validaciones y rollback automático
- ✅ **RF9**: Transacción con actualización de estado del conductor
- ✅ **RF10-RF11**: Transacciones con validaciones de permisos
- ✅ **RFC1-RFC4**: Consultas implementadas (RFC1 con niveles de aislamiento)

**Características:**
- Rollback automático en caso de error
- Validaciones antes de operaciones costosas
- Gestión de estado consistente

### 2. RF8 como Transacción Única (20%)

**RF8 - Solicitar Servicio** implementado como transacción atómica:

**Operaciones SQL dentro de la transacción:**
1. `SELECT ... FROM TARJETA_CREDITO` - Validar tarjeta de crédito
2. `SELECT ... FOR UPDATE` - Buscar y bloquear conductor disponible
3. `UPDATE USUARIO SET estado = 'OCUPADO'` - Actualizar estado del conductor
4. `INSERT INTO SERVICIO` - Crear servicio
5. `INSERT INTO DESTINOS_SERVICIO` - Insertar destinos
6. `INSERT INTO TRANSPORTE_PASAJEROS/ENTREGA_COMIDA/TRANSPORTE_MERCANCIA` - Según tipo

**Escenarios de prueba documentados:**
- ✅ Terminación exitosa
- ✅ Interrupción por tarjeta inválida (rollback)
- ✅ Interrupción por no hay conductores (rollback)
- ✅ Interrupción por validación de datos (rollback)

### 3. RFC1 con Niveles de Aislamiento (20%)

**RFC1 - Historial de Servicios** implementado con dos niveles de aislamiento:

- **READ_COMMITTED**: `rfc1ConcurrenciaReadCommitted()`
- **SERIALIZABLE**: `rfc1ConcurrenciaSerializable()`

**Características:**
- Timer de 30 segundos para observar interacciones
- Consulta ANTES del timer
- Consulta DESPUÉS del timer
- Interfaz web con selector de nivel de aislamiento

### 4. Escenarios de Concurrencia (40%)

#### Escenario 1: Concurrencia con SERIALIZABLE (20%)
- RFC1 con SERIALIZABLE ejecutándose primero
- RF8 ejecutándose concurrentemente antes de que pasen 30 segundos
- Análisis de si RFC1 espera a RF8
- Verificación de si el servicio de RF8 aparece en resultados de RFC1

#### Escenario 2: Concurrencia con READ_COMMITTED (20%)
- RFC1 con READ_COMMITTED ejecutándose primero
- RF8 ejecutándose concurrentemente antes de que pasen 30 segundos
- Análisis de si RFC1 espera a RF8
- Verificación de si el servicio de RF8 aparece en resultados de RFC1

**Documentación completa** en `docs/GUIA-PRUEBAS-ENTREGA2.md` (Secciones Prueba 5 y Prueba 6)

### 5. Bono: Interfaz Gráfica Web (5%)

**Interfaz completa desarrollada con Thymeleaf:**

**Requisitos Funcionales (RF1-RF11):**
- ✅ Todos los formularios implementados y funcionales
- ✅ Validación en tiempo real
- ✅ Mensajes de éxito/error

**Consultas RFC (RFC1-RFC4):**
- ✅ RFC1 con selector de nivel de aislamiento para pruebas de concurrencia
- ✅ RFC2, RFC3, RFC4 con visualización de datos en tablas

**Acceso:** `http://localhost:8080`

---

##  Guía de Pruebas

### Documentación Completa

📖 **Consulta la guía completa de pruebas**: `docs/GUIA-PRUEBAS-ENTREGA2.md`

Esta guía incluye:
- Preparación de la base de datos
- Pruebas de transaccionalidad
- Pruebas detalladas de RF8 (5 escenarios)
- Pruebas de RFC1 con diferentes niveles de aislamiento
- **Prueba 5**: Escenario de concurrencia con SERIALIZABLE (con línea de tiempo y análisis)
- **Prueba 6**: Escenario de concurrencia con READ_COMMITTED (con línea de tiempo y análisis)
- Comparación de escenarios de concurrencia

### Pruebas Rápidas

#### 1. Probar RF8 con Transacción

**Opción A - Interfaz Web:**
1. Navega a: `http://localhost:8080/rf/servicio`
2. Completa el formulario con datos válidos
3. Verifica que el servicio se crea y el conductor queda marcado como "OCUPADO"

#### 2. Probar RFC1 con Niveles de Aislamiento

1. Navega a: `http://localhost:8080/rfc/historial`
2. Ingresa:
   - Cédula: `1001`
   - Límite: `10`
   - Nivel de Aislamiento: `Read Committed` o `Serializable`
3. Haz clic en "Consultar Historial"
4. La consulta tardará 30 segundos y mostrará resultados "antes" y "después"

#### 3. Probar Escenario de Concurrencia

**Ver secciones "Prueba 5" y "Prueba 6" en `docs/GUIA-PRUEBAS-ENTREGA2.md`**

Incluye:
- Pasos detallados con línea de tiempo
- Qué esperar en cada escenario
- Cómo interpretar los resultados

---

## Limpieza y Re-ejecución

### Antes de cada nueva ejecución completa de tests

**En SQL Developer:**

1. **Limpiar la base de datos:**
   - Ejecuta `docs/limpiar.sql`

2. **Crear/Ejecutar tablas (si es necesario):**
   - Ejecuta `docs/tablas.sql`

3. **Poblar la base de datos:**
   - Ejecuta `docs/poblacion.sql`

4. **Reiniciar la aplicación Spring Boot**

---

## Mejoras Implementadas sobre Entrega 1

### Correcciones y Mejoras:

1. **RF8 Completamente Rediseñado:**
   - Validación de tarjeta de crédito antes de asignar conductor
   - Actualización de estado del conductor a "OCUPADO"
   - Bloqueo pesimista con `SELECT FOR UPDATE` (compatible con Oracle)
   - Rollback automático en caso de error

2. **RF9 Mejorado:**
   - Reseteo automático del estado del conductor cuando se finaliza un servicio

3. **Todos los RFs con Transaccionalidad:**
   - Garantía de atomicidad en todas las operaciones
   - Rollback automático en caso de error

4. **RFC1 con Pruebas de Concurrencia:**
   - Versiones con diferentes niveles de aislamiento
   - Timer de 30 segundos para pruebas
   - Interfaz web mejorada

---

##  Documentación Adicional

- **`docs/GUIA-PRUEBAS-ENTREGA2.md`**: Guía completa de pruebas para Entrega 2
- **`GUIA-INTERFAZ.md`**: Guía de uso de la interfaz gráfica web

---

##  Licencia

Este proyecto es parte del curso de **Sistemas Transaccionales - Universidad de los Andes**

---

## Estado del Proyecto

- ✅ Implementación transaccional completa (RF1-RF11, RFC1-RFC4)
- ✅ RF8 como transacción única con validaciones
- ✅ RFC1 con niveles de aislamiento (READ_COMMITTED, SERIALIZABLE)
- ✅ Escenarios de concurrencia documentados
- ✅ Interfaz gráfica web completa
- ✅ Documentación de pruebas completa

Ejecuta `limpiar.sql`, luego `poblacion.sql`, inicia la aplicación y prueba los RFs y escenarios de concurrencia.
