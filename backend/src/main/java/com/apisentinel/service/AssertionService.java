package com.apisentinel.service;

import com.apisentinel.dto.request.CreateAssertionRequest;
import com.apisentinel.dto.response.AssertionResponse;
import com.apisentinel.entity.Monitor;
import com.apisentinel.entity.MonitorAssertion;
import com.apisentinel.enums.AssertionOperator;
import com.apisentinel.enums.AssertionType;

import com.apisentinel.exception.InvalidRequestException;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.MonitorAssertionRepository;
import com.apisentinel.repository.MonitorRepository;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AssertionService {

    private final MonitorRepository monitorRepository;
    private final MonitorAssertionRepository assertionRepository;

    public AssertionService(MonitorRepository monitorRepository,
                            MonitorAssertionRepository assertionRepository) {
        this.monitorRepository = monitorRepository;
        this.assertionRepository = assertionRepository;
    }

    @Transactional
    public AssertionResponse add(Long monitorId, CreateAssertionRequest request) {
        Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);
        validate(request);

        MonitorAssertion assertion = new MonitorAssertion(
                monitor, AssertionType.JSON_PATH, request.path().trim(),
                request.operator(), normalize(request.expectedValue()));

        return AssertionResponse.from(assertionRepository.save(assertion));
    }

    @Transactional(readOnly = true)
    public List<AssertionResponse> list(Long monitorId) {
        monitorRepository.getByIdOrThrow(monitorId); // monitor na ho toh 404
        return assertionRepository.findByMonitorIdOrderByIdAsc(monitorId)
                .stream()
                .map(AssertionResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long monitorId, Long assertionId) {
        MonitorAssertion assertion = assertionRepository.findByIdAndMonitorId(assertionId, monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Assertion", assertionId));
        assertionRepository.delete(assertion);
    }

    private void validate(CreateAssertionRequest request) {
        String path = request.path().trim();
        if (!path.startsWith("$")) {
            throw new InvalidRequestException("path must start with '$'");
        }
        try {
            JsonPath.compile(path);
        } catch (InvalidPathException e) {
            throw new InvalidRequestException("path is not a valid JSONPath expression");
        }

        AssertionOperator op = request.operator();
        String expected = normalize(request.expectedValue());

        if (op.requiresExpectedValue() && expected == null) {
            throw new InvalidRequestException("expectedValue is required for operator " + op);
        }
        if (op.isNumeric() && !isNumber(expected)) {
            throw new InvalidRequestException("expectedValue must be a number for operator " + op);
        }
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private boolean isNumber(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
