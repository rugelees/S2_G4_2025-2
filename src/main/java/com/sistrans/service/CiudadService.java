package com.sistrans.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistrans.repository.CiudadRepository;

@Service
public class CiudadService {

    @Autowired
    private CiudadRepository ciudadRepository;

    @Transactional
    public void crearCiudad(String nombre) {
        ciudadRepository.insertarCiudad(nombre);
    }
}
