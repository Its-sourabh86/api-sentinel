package com.apisentinel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.apisentinel.config.CorsProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPropertiesTest {

    @Test
    @DisplayName("sahi origins accept")
    void validOrigins() {
        CorsProperties props = new CorsProperties(
                List.of("http://localhost:5173", "https://app.example.com"));

        assertThat(props.allowedOrigins()).hasSize(2);
    }

    @Test
    @DisplayName("wildcard origin par app start hi na ho")
    void wildcardRejected() {
        assertThatThrownBy(() -> new CorsProperties(List.of("*")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("https://*.example.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("path ya galat scheme wala origin reject")
    void malformedRejected() {
        assertThatThrownBy(() -> new CorsProperties(List.of("http://localhost:5173/app")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorsProperties(List.of("ftp://example.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("khaali ya null list matlab koi cross-origin allowed nahi")
    void emptyMeansNone() {
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
        assertThat(new CorsProperties(List.of()).allowedOrigins()).isEmpty();
    }
}
