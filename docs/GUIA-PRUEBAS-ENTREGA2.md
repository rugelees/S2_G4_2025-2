# Guía de Pruebas - Entrega 2

Esta guía explica cómo probar las funcionalidades implementadas para la Entrega 2 del proyecto ALPESCAB.

---

## 📋 Preparación de la Base de Datos

### Paso 1: Limpiar la Base de Datos (si ya tienes datos)

1. Abre **SQL Developer**
2. Ejecuta el script `docs/limpiar.sql`
3. Espera a que termine

### Paso 2: Crear/Ejecutar las Tablas (Si es primera vez o después de limpiar)

1. Ejecuta el script `docs/tablas.sql`
   - **IMPORTANTE**: Este script ahora incluye la columna `estado` en la tabla `USUARIO` para Entrega 2
2. Si ya tienes las tablas creadas pero sin la columna `estado`, ejecuta:
   ```sql
   ALTER TABLE USUARIO ADD estado VARCHAR2(20) DEFAULT NULL;
   COMMIT;
   ```
   O ejecuta el script `docs/agregar_columna_estado.sql`

### Paso 3: Poblar la Base de Datos

1. Ejecuta el script `docs/poblacion.sql`
2. Este script crea:
   - 3 ciudades (Bogotá, Medellín, Cali)
   - 20 puntos de trayecto (IDs 1-20)
   - 100 conductores (cédulas 1001-1100)
   - 100 vehículos (placas AUT001-AUT100)
   - 200 pasajeros (cédulas 2001-2200)
   - 200 tarjetas de crédito
   - 200 servicios finalizados (IDs 1-200)
   - 3 servicios abiertos para RF9 (IDs 201-203)

---

## 🧪 Pruebas de la Entrega 2

### Prueba 1: Transaccionalidad en Todos los Servicios

**Objetivo**: Verificar que todas las operaciones de escritura usen `@Transactional`

#### Método 1: Prueba de Rollback (Recomendado)

1. **RF8 - Solicitar Servicio con Tarjeta Inválida**:
   - Intentar crear un servicio con una tarjeta que no pertenece al usuario
   - **Resultado esperado**: Debe fallar con el mensaje "Medio de pago no válido o no pertenece al usuario"
   - **Verificación de transacción**: Si la validación falla, NO debe haberse insertado nada en la base de datos

**Prueba en la Interfaz Web**:
1. Navega a: `http://localhost:8080/rf/servicio`
2. Completa el formulario:
   - Cédula Solicitante: `2001` (usuario poblado)
   - Punto Partida: `1`
   - Punto Destino: `2`
   - Tipo Servicio: `TRANSPORTE_PASAJEROS`
   - Nivel Transporte: `ESTANDAR`
   - **Número Tarjeta**: `9999999999999999` (tarjeta que NO existe)
3. Haz clic en "Solicitar Servicio"
4. Debes ver un error indicando que el medio de pago no es válido

**Prueba con Postman**:
```json
POST http://localhost:8080/api/servicios
{
  "cedula_solicitante": 2001,
  "id_punto_partida": 1,
  "ids_puntos_destino": [2],
  "tipo_servicio": "TRANSPORTE_PASAJEROS",
  "numero_tarjeta": "9999999999999999",
  "nivel_transporte": "ESTANDAR"
}
```\\

**Verificación en Base de Datos**:
```sql
-- Este query NO debe retornar ningún servicio nuevo creado
SELECT * FROM SERVICIO WHERE cedula_solicitante = 2001 
  AND fecha_hora_inicio > SYSTIMESTAMP - INTERVAL '1' MINUTE;
```

---

### Prueba 2: RF8 como Transacción Única (Nueva Validación de Tarjeta)

**Objetivo**: Verificar que RF8 valide la tarjeta antes de asignar conductor

#### Caso Exitoso:

**Prueba en la Interfaz Web**:
1. Navega a: `http://localhost:8080/rf/servicio`
2. Completa el formulario con datos válidos:
   - Cédula Solicitante: `2001`
   - **Número Tarjeta**: Usa el número de tarjeta del usuario 2001
     - Puedes obtenerlo ejecutando: `SELECT numero FROM TARJETA_CREDITO WHERE usuario_cedula = 2001;`
     - Formato poblado: `4444444444442001` (16 dígitos con padding)
   - Punto Partida: `1`
   - Punto Destino: `2`
   - Tipo Servicio: `TRANSPORTE_PASAJEROS`
   - Nivel Transporte: `ESTANDAR`
3. Haz clic en "Solicitar Servicio"
4. **Resultado esperado**: Servicio creado exitosamente

**Verificación en Base de Datos**:
```sql
-- Verificar que el conductor fue marcado como OCUPADO
SELECT cedula, nombre, estado FROM USUARIO WHERE rol = 'CONDUCTOR' AND estado = 'OCUPADO';

-- Verificar que se creó el servicio
SELECT * FROM SERVICIO ORDER BY id DESC FETCH FIRST 1 ROWS ONLY;
```

#### Caso de Falla (Tarjeta Inválida):

Sigue los mismos pasos pero usa una tarjeta que no pertenezca al usuario.

---

### Prueba 3: RFC1 con Niveles de Aislamiento

**Objetivo**: Probar los diferentes niveles de aislamiento de transacciones

#### Preparación:

Antes de probar, asegúrate de tener servicios para el conductor que vas a consultar:
```sql
-- Verificar que hay servicios para el conductor 1001
SELECT COUNT(*) FROM SERVICIO WHERE cedula_conductor = 1001;
```

#### Prueba en la Interfaz Web:

1. Navega a: `http://localhost:8080/rfc/historial`

2. **Prueba Normal (Sin Aislamiento)**:
   - Cédula: `1001`
   - Límite: `10`
   - Nivel de Aislamiento: `Ninguno (Consulta Normal)`
   - Haz clic en "Consultar Historial"
   - **Resultado**: Debe mostrar los resultados inmediatamente (sin espera de 30 segundos)

3. **Prueba Read Committed**:
   - Cédula: `1001`
   - Límite: `10`
   - Nivel de Aislamiento: `Read Committed`
   - Haz clic en "Consultar Historial"
   - **Resultado**: Debe mostrar dos tablas:
     - "Resultados Antes del Timer" (inmediato)
     - Espera 30 segundos
     - "Resultados Después del Timer" (después de la espera)
   - **Análisis IMPORTANTE**: 
     - En Oracle, incluso con READ_COMMITTED, dentro de la misma transacción puedes ver un snapshot consistente
     - Los resultados pueden ser **iguales** si no hay cambios confirmados por otras transacciones
     - Para ver diferencias, necesitas que otra sesión (SQL Developer) inserte datos Y haga COMMIT durante los 30 segundos

4. **Prueba Serializable**:
   - Cédula: `1001`
   - Límite: `10`
   - Nivel de Aislamiento: `Serializable`
   - Haz clic en "Consultar Historial"
   - **Resultado**: Similar a Read Committed, pero con mayor aislamiento
   - **Análisis**: 
     - Con SERIALIZABLE, ambos resultados **DEBEN ser idénticos** (aislamiento máximo)
     - Esto es **comportamiento correcto**: Serializable mantiene la misma vista durante toda la transacción
     - Si ves resultados diferentes, sería un problema de implementación
   
**Para ver diferencias con READ_COMMITTED**, necesitas que otra sesión haga COMMIT durante los 30 segundos:

**Pasos para probar READ_COMMITTED con cambios visibles**:

1. **Sesión 1 (Interfaz Web)**: 
   - Inicia consulta con **READ_COMMITTED** para conductor `1001`
   - Esperará 30 segundos

2. **Sesión 2 (SQL Developer - NUEVA CONEXIÓN)**:
   - Durante esos 30 segundos, ejecuta esto y haz COMMIT INMEDIATAMENTE:
   ```sql
   -- Script para probar READ_COMMITTED (Usuario 2001, Conductor 1001)
   DECLARE
       v_tarjeta VARCHAR2(20);
       v_id NUMBER;
   BEGIN
       SELECT numero INTO v_tarjeta 
       FROM TARJETA_CREDITO 
       WHERE usuario_cedula = 2001;
       
       -- Inserta servicio
       INSERT INTO SERVICIO (fecha_hora_inicio, tipo, cedula_solicitante, cedula_conductor, placa_vehiculo, id_punto_partida, tarjeta_credito, costo_total)
       VALUES (SYSTIMESTAMP, 'TRANSPORTE_PASAJEROS', 2001, 1001, 'AUT001', 1, v_tarjeta, 20000)
       RETURNING id INTO v_id;
       
       INSERT INTO DESTINOS_SERVICIO (id_servicio, id_punto_destino) 
       VALUES (v_id, 2);
       
       INSERT INTO TRANSPORTE_PASAJEROS (servicio_id, nivel) 
       VALUES (v_id, 'ESTANDAR');
       
       COMMIT; -- IMPORTANTE: Haz COMMIT inmediatamente
       DBMS_OUTPUT.PUT_LINE('Servicio insertado con ID: ' || v_id || ' para conductor 1001');
   END;
   /
   ```

3. Compara los resultados "antes" y "después" en Sesión 1

**Pasos para probar SERIALIZABLE con cambios visibles**:

1. **Sesión 1 (Interfaz Web)**: 
   - Inicia consulta con **SERIALIZABLE** para conductor `1002` (diferente conductor)
   - Esperará 30 segundos

2. **Sesión 2 (SQL Developer - NUEVA CONEXIÓN)**:
   - Durante esos 30 segundos, ejecuta esto y haz COMMIT INMEDIATAMENTE:
   ```sql
   -- Script para probar SERIALIZABLE (Usuario 2002, Conductor 1002)
   DECLARE
       v_tarjeta VARCHAR2(20);
       v_id NUMBER;
   BEGIN
       SELECT numero INTO v_tarjeta 
       FROM TARJETA_CREDITO 
       WHERE usuario_cedula = 2002;
       
       -- Inserta servicio
       INSERT INTO SERVICIO (fecha_hora_inicio, tipo, cedula_solicitante, cedula_conductor, placa_vehiculo, id_punto_partida, tarjeta_credito, costo_total)
       VALUES (SYSTIMESTAMP, 'ENTREGA_COMIDA', 2002, 1002, 'AUT002', 3, v_tarjeta, 25000)
       RETURNING id INTO v_id;
       
       INSERT INTO DESTINOS_SERVICIO (id_servicio, id_punto_destino) 
       VALUES (v_id, 4);
       
       INSERT INTO ENTREGA_COMIDA (servicio_id, nombre_restaurante) 
       VALUES (v_id, 'Restaurante Test');
       
       COMMIT; -- IMPORTANTE: Haz COMMIT inmediatamente
       DBMS_OUTPUT.PUT_LINE('Servicio insertado con ID: ' || v_id || ' para conductor 1002');
   END;
   /
   ```

3. Compara los resultados "antes" y "después" en Sesión 1
   - **Con SERIALIZABLE**: Los resultados deberían ser **idénticos** (no ve los cambios de otras transacciones)

**Resumen**:
- ✅ **SERIALIZABLE = resultados iguales**: Comportamiento correcto
- ⚠️ **READ_COMMITTED = resultados iguales (si no hay COMMITs externos)**: También puede ser correcto
- 🔍 **Para ver diferencia en READ_COMMITTED**: Necesitas COMMIT en otra sesión durante la espera

---

### Prueba 4: Actualización de Estado del Conductor

**Objetivo**: Verificar que el estado del conductor se actualiza cuando se crea un servicio

#### ⚠️ IMPORTANTE: Asignación Automática del Conductor

**El conductor se asigna AUTOMÁTICAMENTE** por el sistema cuando solicitas un servicio. No especificas qué conductor quieres; el sistema busca uno disponible y lo asigna.

#### Prueba Manual:

1. **Paso 1: Verifica conductores disponibles antes**:
   ```sql
   -- Ver conductores disponibles (sin servicios activos)
   SELECT u.cedula, u.nombre, u.estado, COUNT(s.id) as servicios_activos
   FROM USUARIO u
   LEFT JOIN SERVICIO s ON u.cedula = s.cedula_conductor 
       AND s.fecha_hora_fin IS NULL
   WHERE u.rol = 'CONDUCTOR'
   GROUP BY u.cedula, u.nombre, u.estado
   ORDER BY u.cedula
   FETCH FIRST 5 ROWS ONLY;
   ```
   - Los conductores disponibles deben tener `estado = NULL` y `servicios_activos = 0`

2. **Paso 2: Obtén un número de tarjeta válido**:
   ```sql
   -- Para el usuario 2001 (pasajero)
   SELECT usuario_cedula, numero 
   FROM TARJETA_CREDITO 
   WHERE usuario_cedula = 2001;
   -- Anota el número de tarjeta (ej: 4444444444442001)
   ```

3. **Paso 3: Crea un servicio** (el conductor se asigna automáticamente):
   - **Opción A - Interfaz Web**: 
     - Navega a `http://localhost:8080/rf/servicio`
     - Cédula Solicitante: `2001`
     - Número Tarjeta: El número obtenido en Paso 2
     - Punto Partida: `1`, Destino: `2`
     - Tipo: `TRANSPORTE_PASAJEROS`, Nivel: `ESTANDAR`
     - Haz clic en "Solicitar Servicio"
     - **Anota el ID del servicio creado** (aparece en el mensaje de éxito)
   

4. **Paso 4: Identifica qué conductor fue asignado**:
   ```sql
   -- Reemplaza [ID_SERVICIO] con el ID obtenido en el Paso 3
   SELECT id, cedula_solicitante, cedula_conductor, placa_vehiculo
   FROM SERVICIO
   WHERE id = [ID_SERVICIO];
   -- Anota la cédula del conductor asignado (ej: 1004)
   ```

5. **Paso 5: Verifica que el estado del conductor asignado cambió**:
   ```sql
   -- Reemplaza [CEDULA_CONDUCTOR] con la cédula obtenida en el Paso 4
   SELECT cedula, nombre, estado 
   FROM USUARIO 
   WHERE cedula = [CEDULA_CONDUCTOR];
   -- Estado debe ser 'OCUPADO'
   ```

6. **Paso 6: Verifica todos los conductores ocupados** (alternativa):
   ```sql
   -- Ver todos los conductores que están OCUPADOS
   SELECT u.cedula, u.nombre, u.estado, s.id as servicio_activo
   FROM USUARIO u
   JOIN SERVICIO s ON u.cedula = s.cedula_conductor
   WHERE u.rol = 'CONDUCTOR'
     AND u.estado = 'OCUPADO'
     AND s.fecha_hora_fin IS NULL;
   ```
   - Deberías ver el conductor que acabas de asignar en esta lista

---