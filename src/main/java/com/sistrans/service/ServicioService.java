package com.sistrans.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistrans.entity.SolicitudServicio;
import com.sistrans.entity.Servicio;
import com.sistrans.entity.ConductorDisponible;
import com.sistrans.repository.ServicioRepository;
import com.sistrans.repository.UsuarioRepository;
import com.sistrans.exception.AppLogicException;

import java.time.LocalDateTime;

@Service
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public Servicio crearServicio(SolicitudServicio solicitud) {
        // 1. Verificar Medio de Pago (NUEVO)
        if (!usuarioRepository.verificarTarjeta(solicitud.getCedulaSolicitante(), solicitud.getNumeroTarjeta())) {
            throw new AppLogicException("Medio de pago no válido o no pertenece al usuario.");
        }
        
        // 2. Buscar Conductor Disponible (EXISTENTE) - Con bloqueo pesimista
        ConductorDisponible conductor = servicioRepository.buscarConductorDisponible(solicitud.getTipoServicio());
        if (conductor == null) {
            throw new AppLogicException("No hay conductores disponibles para este tipo de servicio.");
        }
        
        // 2b. Validación adicional para prevenir asignación múltiple (doble verificación)
        if (!servicioRepository.verificarConductorDisponible(conductor.getCedula())) {
            // Si el conductor ya tiene un servicio activo (race condition), buscar otro
            conductor = servicioRepository.buscarConductorDisponible(solicitud.getTipoServicio());
            if (conductor == null || !servicioRepository.verificarConductorDisponible(conductor.getCedula())) {
                throw new AppLogicException("No hay conductores disponibles para este tipo de servicio.");
            }
        }
        
        // 3. Actualizar Estado del Conductor (NUEVO)
        usuarioRepository.actualizarEstado(conductor.getCedula(), "OCUPADO");

        // 4. Registrar el Viaje (EXISTENTE)
        double costoTotal = calcularCostoEstimado();

        Servicio servicio = new Servicio();
        servicio.setCedulaSolicitante(solicitud.getCedulaSolicitante());
        servicio.setCedulaConductor(conductor.getCedula());
        servicio.setPlacaVehiculo(conductor.getPlaca());
        servicio.setIdPuntoPartida(solicitud.getIdPuntoPartida());
        servicio.setTarjetaCredito(solicitud.getNumeroTarjeta());
        servicio.setTipo(solicitud.getTipoServicio());
        servicio.setCostoTotal(costoTotal);
        servicio.setFechaHoraInicio(LocalDateTime.now()); // RF8: El servicio inicia cuando se solicita
        // fechaHoraFin y distancia quedan NULL hasta que se finalice el servicio (RF9)

        Long idNuevoServicio = servicioRepository.insertarServicio(servicio);

        // Insertar destinos
        for (Long idDestino : solicitud.getIdsPuntosDestino()) {
            servicioRepository.insertarDestino(idNuevoServicio, idDestino);
        }

        // Insertar en tabla de especialización según el tipo de servicio
        switch (solicitud.getTipoServicio().toUpperCase()) {
            case "TRANSPORTE_PASAJEROS":
                if (solicitud.getNivelTransporte() == null) {
                    throw new AppLogicException("El nivel de transporte es requerido para servicios de transporte de pasajeros.");
                }
                servicioRepository.insertarTransportePasajeros(idNuevoServicio, solicitud.getNivelTransporte());
                break;
                
            case "ENTREGA_COMIDA":
                if (solicitud.getNombreRestaurante() == null || solicitud.getNombreRestaurante().trim().isEmpty()) {
                    throw new AppLogicException("El nombre del restaurante es requerido para servicios de entrega de comida.");
                }
                servicioRepository.insertarEntregaComida(idNuevoServicio, solicitud.getNombreRestaurante());
                break;
                
            case "TRANSPORTE_MERCANCIA":
                if (solicitud.getPesoCarga() == null || solicitud.getPesoCarga() <= 0) {
                    throw new AppLogicException("El peso de la carga es requerido y debe ser mayor a 0 para servicios de transporte de mercancía.");
                }
                servicioRepository.insertarTransporteMercancia(idNuevoServicio, solicitud.getPesoCarga());
                break;
                
            default:
                throw new AppLogicException("Tipo de servicio no válido: " + solicitud.getTipoServicio());
        }

        servicio.setId(idNuevoServicio);
        return servicio;
    }

    private double calcularCostoEstimado() {
        return 20000.0;
    }

    @Transactional
    public void finalizarServicio(Long idServicio, String distancia, Double costoTotal) {
        if (!servicioRepository.existeServicio(idServicio)) {
            throw new AppLogicException("El servicio con ID " + idServicio + " no existe");
        }
        
        Long cedulaConductor = servicioRepository.obtenerConductorDelServicio(idServicio);
        
        servicioRepository.finalizarServicio(idServicio, distancia, costoTotal);
        
        if (cedulaConductor != null) {
            usuarioRepository.actualizarEstado(cedulaConductor, null);
        }
    }
}
