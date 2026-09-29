package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncRequest;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncResponse;
import com.tamvagbackend.dto.AuditDtos.ConnectionResponse;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.security.CallerContext;
import com.tamvagbackend.service.ConnectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectorController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class ConnectorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectorService connectorService;

    @Test
    void adminCallerWithoutInstitutionIdCanListConnections() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        when(connectorService.getConnections(any(CallerContext.AdminCaller.class)))
                .thenReturn(List.of(
                        new ConnectionResponse(
                                connectionId,
                                customerId,
                                institutionId,
                                "ACTIVE",
                                "GCB_REF_1",
                                Instant.now(),
                                Instant.now()
                        )
                ));

        mockMvc.perform(
                get("/v1/connectors")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "SUPER_ADMIN")
                                        .claim("operational_role", "SECURITY_ADMIN")
                                )
                                .authorities(() -> "SCOPE_connector:read")
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].connection_id").value(connectionId.toString()))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void adminCallerWithoutInstitutionIdCanSyncConnector() throws Exception {
        UUID adminUserId = UUID.randomUUID();
        UUID connectionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();

        when(connectorService.sync(eq(connectionId), any(ConnectorSyncRequest.class), any(CallerContext.AdminCaller.class)))
                .thenReturn(new ConnectorSyncResponse(
                        "sync_admin_001",
                        "COMPLETED",
                        10,
                        10,
                        0,
                        Instant.now()
                ));

        mockMvc.perform(
                post("/v1/connectors/" + connectionId + "/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customer_id": "%s",
                                  "institution_id": "%s",
                                  "sync_mode": "FULL"
                                }
                                """.formatted(customerId, institutionId))
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "SUPER_ADMIN")
                                        .claim("operational_role", "SECURITY_ADMIN")
                                )
                                .authorities(() -> "SCOPE_connector:sync")
                        )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sync_id").value("sync_admin_001"))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void adminCallerWithoutRequiredScopeIsForbidden() throws Exception {
        UUID adminUserId = UUID.randomUUID();

        mockMvc.perform(
                get("/v1/connectors")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject(adminUserId.toString())
                                        .claim("token_type", "admin")
                                        .claim("admin_user_id", adminUserId.toString())
                                        .claim("email", "admin@tamva.com")
                                        .claim("role", "ADMIN")
                                        .claim("operational_role", "AUDITOR")
                                )
                                .authorities(() -> "SCOPE_other:read")
                        )
        )
        .andExpect(status().isForbidden());
    }

    @Test
    void institutionCallerCanListConnections() throws Exception {
        UUID institutionId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(connectorService.getConnections(any(CallerContext.InstitutionCaller.class)))
                .thenReturn(List.of());

        mockMvc.perform(
                get("/v1/connectors")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("institution-app")
                                        .claim("token_type", "institution")
                                        .claim("institution_id", institutionId.toString())
                                        .claim("application_id", applicationId.toString())
                                )
                                .authorities(() -> "SCOPE_connector:read")
                        )
        )
        .andExpect(status().isOk());
    }
}
