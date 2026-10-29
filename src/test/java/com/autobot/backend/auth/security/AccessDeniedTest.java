package com.autobot.backend.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class AccessDeniedTest {

    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        accessDeniedHandler = new RestAccessDeniedHandler();
    }

    @Test
    void testAccesoRestringidoRetorna403ConFormatoUnico() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/admin/recurso");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Acceso denegado");

        accessDeniedHandler.handle(request, response, exception);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        
        String jsonResponse = response.getContentAsString();
        assertTrue(jsonResponse.contains("403"));
        assertTrue(jsonResponse.contains("No tienes los permisos necesarios"));
    }
}
