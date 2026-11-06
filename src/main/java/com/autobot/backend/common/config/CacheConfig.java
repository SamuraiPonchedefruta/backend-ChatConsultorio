package com.autobot.backend.common.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    /** Fichas públicas de modelos, por id. */
    public static final String FICHAS = "fichas";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager gestor = new CaffeineCacheManager(FICHAS);
        gestor.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                // Red de seguridad: aunque falle una invalidación, la ficha vieja no vive para siempre.
                .expireAfterWrite(Duration.ofMinutes(10)));
        // Las escrituras y borrados de caché esperan al commit de la transacción. Sin esto, la
        // caché se invalidaría antes de guardar el cambio y otra consulta podría volver a cargar el dato viejo.
        return new TransactionAwareCacheManagerProxy(gestor);
    }
}
