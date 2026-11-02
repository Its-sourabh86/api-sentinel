package com.apisentinel.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSchemaCheckerTest {

    private static final String SCHEMA = """
            {"type":"object","required":["data"],
             "properties":{"data":{"type":"object","required":["stock","price"],
               "properties":{"stock":{"type":"integer","minimum":0},
                             "price":{"type":"number"}}}}}
            """;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonSchemaChecker checker = new JsonSchemaChecker();

    private List<String> check(String json) throws Exception {
        return checker.check(mapper.readTree(json), SCHEMA);
    }

    @Test
    @DisplayName("sahi shape: koi error nahi")
    void valid() throws Exception {
        assertThat(check("{\"data\":{\"stock\":5,\"price\":9.5}}")).isEmpty();
    }

    @Test
    @DisplayName("required field gayab: error aaye")
    void missingRequiredField() throws Exception {
        assertThat(check("{\"data\":{\"stock\":5}}")).isNotEmpty();
    }

    @Test
    @DisplayName("type drift: stock number ki jagah string")
    void wrongType() throws Exception {
        List<String> errors = check("{\"data\":{\"stock\":\"5\",\"price\":9.5}}");

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("stock");
    }

    @Test
    @DisplayName("minimum rule: negative stock fail")
    void minimum() throws Exception {
        assertThat(check("{\"data\":{\"stock\":-1,\"price\":9.5}}")).isNotEmpty();
    }
}
