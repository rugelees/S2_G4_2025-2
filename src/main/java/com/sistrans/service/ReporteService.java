package com.sistrans.service;

import com.sistrans.dto.Rfc1HistorialDTO;
import com.sistrans.dto.Rfc2TopConductorDTO;
import com.sistrans.dto.Rfc3GananciasVehiculoDTO;
import com.sistrans.dto.Rfc4UsoServicioDTO;
import com.sistrans.repository.RfcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

//rfc 1
@Service
public class ReporteService {

    private final RfcRepository repo;

    public ReporteService(RfcRepository repo) {
        this.repo = repo;
    }

    public List<Rfc1HistorialDTO> rfc1(String cedula, Integer limite) {
        return repo.rfc1Historial(cedula, limite);
    }
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Object> rfc1ConcurrenciaSerializable(String cedula, Integer limite) throws InterruptedException {
        Map<String, Object> resultados = new HashMap<>();

        // 1. Consulta ANTES del timer
        List<Rfc1HistorialDTO> resultadoAntes = repo.rfc1Historial(cedula, limite);
        resultados.put("antes", resultadoAntes);

        // 2. Timer de 30 segundos
        Thread.sleep(30000); 

        // 3. Consulta DESPUÉS del timer
        List<Rfc1HistorialDTO> resultadoDespues = repo.rfc1Historial(cedula, limite);
        resultados.put("despues", resultadoDespues);

        return resultados;
    }
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Map<String, Object> rfc1ConcurrenciaReadCommitted(String cedula, Integer limite) throws InterruptedException {
        Map<String, Object> resultados = new HashMap<>();

        // 1. Consulta ANTES del timer
        List<Rfc1HistorialDTO> resultadoAntes = repo.rfc1Historial(cedula, limite);
        resultados.put("antes", resultadoAntes);

        // 2. Timer de 30 segundos
        Thread.sleep(30000);

        // 3. Consulta DESPUÉS del timer
        List<Rfc1HistorialDTO> resultadoDespues = repo.rfc1Historial(cedula, limite);
        resultados.put("despues", resultadoDespues);

        return resultados;
    }

    public List<Rfc2TopConductorDTO> rfc2(Integer limite){
    return repo.rfc2TopConductores(limite);
    }

    public List<Rfc3GananciasVehiculoDTO> rfc3(String cedulaConductor){
    return repo.rfc3GananciasPorConductor(cedulaConductor);
    }

    public List<Rfc4UsoServicioDTO> rfc4(LocalDateTime desde, LocalDateTime hasta){
    return repo.rfc4UsoServicios(desde, hasta);
    }
}
