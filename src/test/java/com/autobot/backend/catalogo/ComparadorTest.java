package com.autobot.backend.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.autobot.backend.catalogo.model.FichaModelo;
import com.autobot.backend.catalogo.model.Modelo;
import com.autobot.backend.catalogo.model.RangoPrecio;
import com.autobot.backend.catalogo.repository.ModeloRepository;
import com.autobot.backend.catalogo.repository.RangoPrecioRepository;

class ComparadorTest {

    private final ModeloRepository modeloRepo = mock(ModeloRepository.class);
    private final RangoPrecioRepository precioRepo = mock(RangoPrecioRepository.class);
    private final ComparadorService service = new ComparadorService(modeloRepo, precioRepo, 3);

    private Modelo modelo(Long id, String marca, String nombre) {
        Modelo m = new Modelo(marca, nombre);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }

    private void assertEstado(HttpStatus esperado, Runnable accion) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, ex.getStatusCode());
    }

    @Test
    void devuelveModelosAlineadosEnElOrdenPedido() {
        when(modeloRepo.findAllById(List.of(2L, 1L))).thenReturn(List.of(
                modelo(1L, "Toyota", "Yaris"),
                modelo(2L, "Kia", "Rio")));

        var resultado = service.comparar(List.of(2L, 1L), null);

        assertEquals(List.of(2L, 1L), resultado.ids());
        assertEquals("Kia", resultado.modelos().get(0).marca());
        assertEquals("Toyota", resultado.modelos().get(1).marca());
    }

    @Test
    void incluyeLaFichaYDejaNullSiElModeloNoTiene() {
        Modelo conFicha = modelo(1L, "Toyota", "Yaris");
        conFicha.asignarFicha(new FichaModelo("5 L/100km", "Cada 10000 km", "Ninguna", "Baratos", "Embrague"));
        Modelo sinFicha = modelo(2L, "Kia", "Rio");
        when(modeloRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(conFicha, sinFicha));

        var resultado = service.comparar(List.of(1L, 2L), null);

        assertEquals("5 L/100km", resultado.modelos().get(0).ficha().consumo());
        assertNull(resultado.modelos().get(1).ficha());
    }

    @Test
    void incluyePrecioDelAnioPedidoYNullSiNoHayRango() {
        Modelo yaris = modelo(1L, "Toyota", "Yaris");
        Modelo rio = modelo(2L, "Kia", "Rio");
        when(modeloRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(yaris, rio));
        when(precioRepo.findByModeloIdAndAnioOrderByFechaFuenteDesc(1L, 2018)).thenReturn(List.of(
                new RangoPrecio(yaris, 2018, new BigDecimal("8000"), new BigDecimal("9500"),
                        "Facebook Marketplace", LocalDate.of(2026, 10, 1))));

        var resultado = service.comparar(List.of(1L, 2L), 2018);

        var precio = resultado.modelos().get(0).precio();
        assertNotNull(precio);
        assertEquals(new BigDecimal("8000"), precio.precioMin());
        assertEquals(new BigDecimal("9500"), precio.precioMax());
        assertNull(resultado.modelos().get(1).precio());
    }

    @Test
    void sinAnioElPrecioEsNull() {
        when(modeloRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                modelo(1L, "Toyota", "Yaris"), modelo(2L, "Kia", "Rio")));

        var resultado = service.comparar(List.of(1L, 2L), null);

        assertNull(resultado.modelos().get(0).precio());
    }

    @Test
    void responde400ConMenosDelMinimo() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> service.comparar(List.of(1L), null));
    }

    @Test
    void responde400ConMasDelMaximo() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> service.comparar(List.of(1L, 2L, 3L, 4L), null));
    }

    @Test
    void responde400SiNoHayIds() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> service.comparar(null, null));
    }

    @Test
    void responde400ConIdsRepetidos() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> service.comparar(List.of(1L, 1L), null));
    }

    @Test
    void responde400ConIdNulo() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> service.comparar(Arrays.asList(1L, null, 3L), null));
    }

    @Test
    void responde404SiUnModeloNoExiste() {
        when(modeloRepo.findAllById(List.of(1L, 99L))).thenReturn(List.of(
                modelo(1L, "Toyota", "Yaris")));

        assertEstado(HttpStatus.NOT_FOUND, () -> service.comparar(List.of(1L, 99L), null));
    }

    @Test
    void fallaAlArrancarSiMaxModelosEsMenorQueElMinimo() {
        assertThrows(IllegalStateException.class,
                () -> new ComparadorService(modeloRepo, precioRepo, 1));
    }
}
