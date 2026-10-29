package com.autobot.backend.chatbot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class VisitanteLimitService {

    private final int limiteDiario;
    
    // Almacena por clave (Identificador/IP + Fecha) las peticiones realizadas
    private final Map<String, AtomicInteger> contadorVisitas = new ConcurrentHashMap<>();

    public VisitanteLimitService(@Value("${chatbot.visitante.limite-diario:10}") int limiteDiario) {
        this.limiteDiario = limiteDiario;
    }

    public boolean permitirInteraccion(String identificador) {
        String clave = generarClave(identificador);
        AtomicInteger contador = contadorVisitas.computeIfAbsent(clave, k -> new AtomicInteger(0));
        return contador.get() < limiteDiario;
    }

    public void registrarInteraccion(String identificador) {
        String clave = generarClave(identificador);
        AtomicInteger contador = contadorVisitas.computeIfAbsent(clave, k -> new AtomicInteger(0));
        contador.incrementAndGet();
    }

    public int getLimiteDiario() {
        return limiteDiario;
    }

    public void reiniciarContadores() {
        contadorVisitas.clear();
    }

    private String generarClave(String identificador) {
        return identificador + "_" + LocalDate.now();
    }
}
