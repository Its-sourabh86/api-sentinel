package com.apisentinel.security;

import com.apisentinel.enums.Role;

public record AuthenticatedUser(Long id, Role role) {
}