package com.apisentinel.service;

import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.Role;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorAccessTest {

    @Mock
    private MonitorRepository monitorRepository;

    private static final AuthenticatedUser ALICE = new AuthenticatedUser(1L, Role.USER);
    private static final AuthenticatedUser BOB = new AuthenticatedUser(2L, Role.USER);
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(99L, Role.ADMIN);

    private MonitorAccess access() {
        return new MonitorAccess(monitorRepository);
    }

    private void monitorOwnedBy(Long id, Long ownerId) {
        Monitor m = new Monitor("A", "https://example.com", HttpMethod.GET, 60, 3000, 200);
        if (ownerId != null) {
            m.assignOwner(ownerId);
        }
        when(monitorRepository.getByIdOrThrow(id)).thenReturn(m);
    }

    @Test
    @DisplayName("owner apna monitor access kar sake")
    void owner_allowed() {
        monitorOwnedBy(5L, 1L);
        assertThatCode(() -> access().assertAccessible(5L, ALICE)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("dusre user ko 404 (IDOR band)")
    void otherUser_notFound() {
        monitorOwnedBy(5L, 1L);
        assertThatThrownBy(() -> access().assertAccessible(5L, BOB))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("admin kisi ka bhi monitor dekh sake")
    void admin_allowed() {
        monitorOwnedBy(5L, 1L);
        assertThatCode(() -> access().assertAccessible(5L, ADMIN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bina owner wala purana monitor: admin ko haan, user ko nahi")
    void legacyMonitor_adminOnly() {
        monitorOwnedBy(5L, null);
        assertThatCode(() -> access().assertAccessible(5L, ADMIN)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access().assertAccessible(5L, ALICE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("exist hi na kare toh bhi wahi 404 (alag error nahi)")
    void missingMonitor_sameException() {
        when(monitorRepository.getByIdOrThrow(404L))
                .thenThrow(new ResourceNotFoundException("Monitor", 404L));
        assertThatThrownBy(() -> access().assertAccessible(404L, ALICE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}