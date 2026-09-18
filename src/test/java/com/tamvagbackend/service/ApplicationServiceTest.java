package com.tamvagbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Application;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.repository.ApplicationRepository;
import com.tamvagbackend.domain.repository.InstitutionRepository;
import com.tamvagbackend.dto.ApplicationDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private ObjectMapper objectMapper;

    private PasswordEncoder passwordEncoder;
    private ApplicationService applicationService;

    private UUID institutionId;
    private UUID applicationId;
    private Institution institution;

    private Application savedApplication;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        institutionId = UUID.randomUUID();
        applicationId = UUID.randomUUID();

        institution = new Institution();
        institution.setInstitutionId(institutionId);

        applicationService = new ApplicationService(
                applicationRepository,
                institutionRepository,
                passwordEncoder,
                new ObjectMapper()
        );
    }

    @Test
    void createGeneratesClientCredentialsAndStoresOnlyHash() {
        ApplicationDtos.CreateApplicationRequest request =
                new ApplicationDtos.CreateApplicationRequest(
                        institutionId,
                        "Test Partner",
                        List.of("risk:evaluate", "profile:read")
                );

        when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.of(institution));

        when(applicationRepository.findByClientId(anyString()))
                .thenReturn(Optional.empty());

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> {
                Application application = invocation.getArgument(0);
                application.setApplicationId(applicationId);
                savedApplication = application;
                return application;
                });

        ApplicationDtos.CreateApplicationResponse response =
                applicationService.create(request);

        assertNotNull(response);
        assertNotNull(response.clientSecret());
        assertFalse(response.clientSecret().isBlank());

        assertNotNull(response.application());
        assertEquals(applicationId, response.application().applicationId());
        assertEquals(institutionId, response.application().institutionId());
        assertEquals("Test Partner", response.application().name());
        assertEquals("ACTIVE", response.application().status());

        verify(applicationRepository).save(any(Application.class));

        assertNotNull(savedApplication);

        assertNotNull(savedApplication.getClientSecretHash());
        assertNotEquals(
                response.clientSecret(),
                savedApplication.getClientSecretHash()
        );

        assertTrue(
                passwordEncoder.matches(
                        response.clientSecret(),
                        savedApplication.getClientSecretHash()
                )
        );
    }

    @Test
    void createRejectsUnknownInstitution() {
        when(institutionRepository.findById(institutionId))
                .thenReturn(Optional.empty());

        ApplicationDtos.CreateApplicationRequest request =
                new ApplicationDtos.CreateApplicationRequest(
                        institutionId,
                        "Test Partner",
                        List.of("risk:evaluate")
                );

        assertThrows(
                Exception.class,
                () -> applicationService.create(request)
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateStatusAcceptsActiveAndInactive() {
        Application application = existingApplication();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationDtos.ApplicationResponse response =
                applicationService.updateStatus(
                        applicationId,
                        new ApplicationDtos.UpdateStatusRequest("INACTIVE")
                );

        assertEquals("INACTIVE", response.status());

        response = applicationService.updateStatus(
                applicationId,
                new ApplicationDtos.UpdateStatusRequest("ACTIVE")
        );

        assertEquals("ACTIVE", response.status());
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        Application application = existingApplication();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        assertThrows(
                Exception.class,
                () -> applicationService.updateStatus(
                        applicationId,
                        new ApplicationDtos.UpdateStatusRequest("DELETED")
                )
        );

        verify(applicationRepository, never())
                .save(any(Application.class));
    }

    @Test
    void updateScopesNormalizesAndPersistsScopes() throws Exception {
        Application application = existingApplication();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationDtos.ApplicationResponse response =
                applicationService.updateScopes(
                        applicationId,
                        new ApplicationDtos.UpdateScopesRequest(
                                List.of(
                                        "risk:evaluate",
                                        " risk:evaluate ",
                                        "profile:read"
                                )
                        )
                );

        assertEquals(
                List.of("risk:evaluate", "profile:read"),
                response.scopes()
        );

        assertEquals(
                List.of("risk:evaluate", "profile:read"),
                new ObjectMapper().readValue(
                        application.getScopes(),
                        List.class
                )
        );
    }

    @Test
    void rotateSecretReplacesStoredHash() {
        Application application = existingApplication();

        String oldSecret = "old-secret";
        application.setClientSecretHash(
                passwordEncoder.encode(oldSecret)
        );

        String oldHash = application.getClientSecretHash();

        when(applicationRepository.findById(applicationId))
                .thenReturn(Optional.of(application));

        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApplicationDtos.RotateSecretResponse response =
                applicationService.rotateSecret(applicationId);

        assertNotNull(response.clientSecret());
        assertFalse(response.clientSecret().isBlank());

        assertNotEquals(
                oldHash,
                application.getClientSecretHash()
        );

        assertFalse(
                passwordEncoder.matches(
                        oldSecret,
                        application.getClientSecretHash()
                )
        );

        assertTrue(
                passwordEncoder.matches(
                        response.clientSecret(),
                        application.getClientSecretHash()
                )
        );
    }

    private Application existingApplication() {
        Application application = new Application();

        application.setApplicationId(applicationId);
        application.setInstitution(institution);
        application.setClientId("tamva_test");
        application.setName("Existing Application");
        application.setStatus("ACTIVE");
        application.setScopes(
                "[\"risk:evaluate\", \"profile:read\"]"
        );

        return application;
    }
}