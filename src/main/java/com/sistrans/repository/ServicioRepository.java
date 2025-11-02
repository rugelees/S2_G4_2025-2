package com.sistrans.repository;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sistrans.entity.Servicio;
import com.sistrans.entity.ConductorDisponible;


@Repository
public class ServicioRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ConductorDisponible buscarConductorDisponible(String tipoServicio) {
        return buscarConductorDisponible(tipoServicio, 0);
    }
    
    private ConductorDisponible buscarConductorDisponible(String tipoServicio, int intentos) {
        // Limitar recursión a máximo 3 intentos para evitar loops infinitos
        if (intentos >= 3) {
            System.out.println("[DEBUG] Máximo de intentos alcanzado, no se encontró conductor disponible");
            return null;
        }
        
        // Map service types to the vehicle type used in the seeded data
        String tipoVehiculo = mapearTipoServicioATipoVehiculo(tipoServicio);
        
        System.out.println("[DEBUG] Buscando conductor para tipoServicio: " + tipoServicio + ", tipoVehiculo: " + tipoVehiculo + " (intento " + (intentos + 1) + ")");

        // Oracle no permite FOR UPDATE con FETCH FIRST, así que usamos un enfoque de dos pasos:
        // 1. Primero encontramos el conductor disponible
        // 2. Luego lo bloqueamos con FOR UPDATE
        
        String sqlBuscar = "SELECT u.cedula, v.placa FROM USUARIO u " +
                          "JOIN VEHICULO v ON u.cedula = v.cedula_dueño " +
                          "WHERE u.rol = 'CONDUCTOR' " +
                          "AND v.tipo = ? " +
                          "AND NOT EXISTS (SELECT 1 FROM SERVICIO s WHERE s.cedula_conductor = u.cedula AND s.fecha_hora_fin IS NULL) " +
                          "ORDER BY u.cedula " +
                          "FETCH FIRST 1 ROWS ONLY";

        try {
            // Primero buscar el conductor sin FOR UPDATE
            System.out.println("[DEBUG] Ejecutando consulta de búsqueda sin FOR UPDATE");
            ConductorDisponible conductorEncontrado = jdbcTemplate.queryForObject(sqlBuscar, (rs, rowNum) ->
                new ConductorDisponible(rs.getLong("cedula"), rs.getString("placa")), tipoVehiculo);
            
            if (conductorEncontrado == null) {
                System.out.println("[DEBUG] No se encontró ningún conductor disponible");
                return null;
            }
            
            System.out.println("[DEBUG] Conductor encontrado: " + conductorEncontrado.getCedula() + " - " + conductorEncontrado.getPlaca());
            
            // Luego bloquear el conductor específico con FOR UPDATE
            String sqlBloquear = "SELECT u.cedula, v.placa FROM USUARIO u " +
                                "JOIN VEHICULO v ON u.cedula = v.cedula_dueño " +
                                "WHERE u.cedula = ? " +
                                "AND u.rol = 'CONDUCTOR' " +
                                "AND v.tipo = ? " +
                                "AND NOT EXISTS (SELECT 1 FROM SERVICIO s WHERE s.cedula_conductor = u.cedula AND s.fecha_hora_fin IS NULL) " +
                                "FOR UPDATE";
            
            System.out.println("[DEBUG] Bloqueando conductor con FOR UPDATE");
            try {
                ConductorDisponible conductorBloqueado = jdbcTemplate.queryForObject(sqlBloquear, (rs, rowNum) ->
                    new ConductorDisponible(rs.getLong("cedula"), rs.getString("placa")), 
                    conductorEncontrado.getCedula(), tipoVehiculo);
                
                System.out.println("[DEBUG] Conductor bloqueado exitosamente: " + conductorBloqueado.getCedula());
                return conductorBloqueado;
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                // El conductor ya no está disponible (probablemente fue tomado por otra transacción)
                System.out.println("[DEBUG] El conductor " + conductorEncontrado.getCedula() + " ya no está disponible, intentando buscar otro...");
                // Intentar buscar otro conductor (con límite de intentos)
                return buscarConductorDisponible(tipoServicio, intentos + 1);
            }
            
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            System.out.println("[DEBUG] No se encontró ningún conductor disponible (EmptyResultDataAccessException)");
            return null;
        } catch (Exception e) {
            System.err.println("[DEBUG] Error al buscar conductor disponible: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Valida que un conductor no tenga servicios activos antes de asignarlo
     * Útil como doble verificación después del bloqueo pesimista
     */
    public boolean verificarConductorDisponible(Long cedulaConductor) {
        String sql = "SELECT COUNT(*) FROM SERVICIO " +
                    "WHERE cedula_conductor = ? AND fecha_hora_fin IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, cedulaConductor);
        return count == null || count == 0;
    }

    private String mapearTipoServicioATipoVehiculo(String tipoServicio) {
        if (tipoServicio == null) return "carro";
        String ts = tipoServicio.trim().toUpperCase();
        switch (ts) {
            case "TRANSPORTE_PASAJEROS":
            case "ENTREGA_COMIDA":
            case "TRANSPORTE_MERCANCIA":
                return "carro"; 
            default:
                return "carro";
        }
    }

    public Long insertarServicio(Servicio servicio) {
        String sql = "INSERT INTO SERVICIO (fecha_hora_inicio, fecha_hora_fin, distancia, costo_total, tipo, cedula_solicitante, cedula_conductor, placa_vehiculo, id_punto_partida, tarjeta_credito) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, servicio.getFechaHoraInicio(), servicio.getFechaHoraFin(), 
                          servicio.getDistancia(), servicio.getCostoTotal(), servicio.getTipo(), 
                          servicio.getCedulaSolicitante(), servicio.getCedulaConductor(), 
                          servicio.getPlacaVehiculo(), servicio.getIdPuntoPartida(), servicio.getTarjetaCredito());
        
        String getIdSql = "SELECT id FROM SERVICIO WHERE cedula_solicitante = ? AND cedula_conductor = ? ORDER BY id DESC FETCH FIRST 1 ROWS ONLY";
        return jdbcTemplate.queryForObject(getIdSql, Long.class, servicio.getCedulaSolicitante(), servicio.getCedulaConductor());
    }

    public void insertarDestino(Long idServicio, Long idPuntoDestino) {
        String sql = "INSERT INTO DESTINOS_SERVICIO (id_servicio, id_punto_destino, orden) VALUES (?, ?, 1)";
        jdbcTemplate.update(sql, idServicio, idPuntoDestino);
    }

    public boolean existeServicio(Long idServicio) {
        String sql = "SELECT COUNT(*) FROM SERVICIO WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, idServicio);
        return count != null && count > 0;
    }

    public Long obtenerConductorDelServicio(Long idServicio) {
        String sql = "SELECT cedula_conductor FROM SERVICIO WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class, idServicio);
        } catch (Exception e) {
            return null;
        }
    }

    public void finalizarServicio(Long idServicio, String distancia, Double costoTotal) {
        String sql = "UPDATE SERVICIO SET fecha_hora_fin = CURRENT_TIMESTAMP, distancia = ?, costo_total = ? WHERE id = ?";
        jdbcTemplate.update(sql, distancia, costoTotal, idServicio);
    }

    public void insertarTransportePasajeros(Long idServicio, String nivel) {
        String sql = "INSERT INTO TRANSPORTE_PASAJEROS (servicio_id, nivel) VALUES (?, ?)";
        jdbcTemplate.update(sql, idServicio, nivel);
    }

    public void insertarEntregaComida(Long idServicio, String nombreRestaurante) {
        String sql = "INSERT INTO ENTREGA_COMIDA (servicio_id, nombre_restaurante) VALUES (?, ?)";
        jdbcTemplate.update(sql, idServicio, nombreRestaurante);
    }

    public void insertarTransporteMercancia(Long idServicio, Double pesoCarga) {
        String sql = "INSERT INTO TRANSPORTE_MERCANCIA (servicio_id, carga) VALUES (?, ?)";
        jdbcTemplate.update(sql, idServicio, pesoCarga);
    }

}
