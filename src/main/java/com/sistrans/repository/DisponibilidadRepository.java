package com.sistrans.repository;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.sistrans.entity.Disponibilidad;

@Repository
public class DisponibilidadRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public boolean verificarSuperposicion(Long cedulaConductor, LocalDate dia, String nuevaFranjaHoraria, Long idDisponibilidadExcluir) {

        String sql = "SELECT COUNT(*) FROM DISPONIBILIDAD d " +
                  "WHERE d.dia = ? " +
                  "AND (? IS NULL OR d.id_disponibilidad != ?) " +
                  "AND (" +
                  "  (SUBSTR(?, 1, 5) < SUBSTR(d.franja_horaria, 7, 5) " +
                  "   AND SUBSTR(?, 7, 5) > SUBSTR(d.franja_horaria, 1, 5)) " +
                  "  OR " +
                  "  (SUBSTR(d.franja_horaria, 1, 5) < SUBSTR(?, 7, 5) " +
                  "   AND SUBSTR(d.franja_horaria, 7, 5) > SUBSTR(?, 1, 5))" +
                  ")";
        
        Long excludeId = idDisponibilidadExcluir != null ? idDisponibilidadExcluir : null;
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 
            dia, 
            excludeId,
            excludeId != null ? excludeId : -1,
            nuevaFranjaHoraria, nuevaFranjaHoraria,
            nuevaFranjaHoraria, nuevaFranjaHoraria
        );
        return count != null && count > 0;
    }

    public Long insertarDisponibilidad(Disponibilidad disponibilidad, Long cedulaConductor, String placaVehiculo) {
        String sql = "INSERT INTO DISPONIBILIDAD (dia, franja_horaria, tipo_transporte) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, disponibilidad.getDia(), disponibilidad.getFranjaHoraria(), disponibilidad.getTipoTransporte());
        
        String getIdSql = "SELECT id_disponibilidad FROM DISPONIBILIDAD WHERE dia = ? AND franja_horaria = ? AND tipo_transporte = ? ORDER BY id_disponibilidad DESC FETCH FIRST 1 ROWS ONLY";
        Long idDisponibilidad = jdbcTemplate.queryForObject(getIdSql, Long.class, disponibilidad.getDia(), disponibilidad.getFranjaHoraria(), disponibilidad.getTipoTransporte());
        
        return idDisponibilidad;
    }

    public void actualizarDisponibilidad(Disponibilidad disponibilidad, Long cedulaConductor, String placaVehiculo) {
        String sql = "UPDATE DISPONIBILIDAD SET dia = ?, franja_horaria = ?, tipo_transporte = ? WHERE id_disponibilidad = ?";
        jdbcTemplate.update(sql, disponibilidad.getDia(), disponibilidad.getFranjaHoraria(), 
                          disponibilidad.getTipoTransporte(), disponibilidad.getIdDisponibilidad());
    }
}
