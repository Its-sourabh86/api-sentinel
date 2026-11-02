package com.apisentinel.dto.request;
import com.apisentinel.enums.HttpMethod;
public record ProbeRequest(String url, HttpMethod method, int timeoutMs) {
}
