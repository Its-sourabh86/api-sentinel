package com.apisentinel.service;
import com.apisentinel.exception.InvalidRequestException;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.entity.Monitor;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.utils.JsonSchemaChecker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchemaService {

    private static final Logger log = LoggerFactory.getLogger(SchemaService.class);
    private static final int MAX_SCHEMA_LENGTH = 20_000;

    private final MonitorRepository monitorRepository;
    private final JsonSchemaChecker schemaChecker;
    private final ObjectMapper objectMapper;

    public SchemaService(MonitorRepository monitorRepository,
                         JsonSchemaChecker schemaChecker,
                         ObjectMapper objectMapper) {
        this.monitorRepository = monitorRepository;
        this.schemaChecker = schemaChecker;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public JsonNode set(Long monitorId, JsonNode schema) {
        Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);
        validateSchema(schema);

        monitor.setResponseSchema(schema.toString());
        monitorRepository.save(monitor);
        log.info("Response schema set monitorId={}", monitorId);
        return schema;
    }

    @Transactional(readOnly = true)
    public JsonNode get(Long monitorId) {
        Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);
        if (monitor.getResponseSchema() == null) {
            throw new ResourceNotFoundException("Schema for monitor", monitorId);
        }
        try {
            return objectMapper.readTree(monitor.getResponseSchema());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored schema is corrupted for monitor " + monitorId, e);
        }
    }

    @Transactional
    public void remove(Long monitorId) {
        Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);
        monitor.setResponseSchema(null);
        monitorRepository.save(monitor);
        log.info("Response schema removed monitorId={}", monitorId);
    }

    private void validateSchema(JsonNode schema) {
        if (schema == null || !(schema.isObject() || schema.isBoolean())) {
            throw new InvalidRequestException("schema must be a JSON object");
        }
        if (schema.toString().length() > MAX_SCHEMA_LENGTH) {
            throw new InvalidRequestException("schema must be at most " + MAX_SCHEMA_LENGTH + " characters");
        }
        // SSRF guard: sirf local refs ("#/...") allowed, remote URL se schema fetch nahi hoga
        for (JsonNode ref : schema.findValues("$ref")) {
            if (!ref.isTextual() || !ref.asText().startsWith("#")) {
                throw new InvalidRequestException("only local $ref (starting with '#') is allowed");
            }
        }
        try {
            schemaChecker.assertSchemaIsValid(schema);
        } catch (RuntimeException e) {
            throw new InvalidRequestException("schema is not a valid JSON Schema");
        }
    }
}
