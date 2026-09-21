
package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Application;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.repository.ApplicationRepository;
import com.tamvagbackend.domain.repository.InstitutionRepository;
import com.tamvagbackend.dto.ApplicationDtos;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Set<String> FORBIDDEN_APPLICATION_SCOPES = Set.of(
            "cases:read",
            "cases:write",
            "cases:create"
    );

    private final ApplicationRepository applicationRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.applicationRepository = applicationRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    public ApplicationDtos.CreateApplicationResponse create(
            ApplicationDtos.CreateApplicationRequest request,
            UUID callerInstitutionId
    ) {
        if (!callerInstitutionId.equals(request.institutionId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot create an application for another institution"
            );
        }

        Institution institution = institutionRepository
                .findById(request.institutionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Institution not found: " + request.institutionId()
                ));

        Application application = new Application();

        application.setInstitution(institution);
        application.setName(request.name().trim());
        application.setStatus("ACTIVE");

        String clientId = generateUniqueClientId();
        String clientSecret = generateClientSecret();

        application.setClientId(clientId);
        application.setClientSecretHash(
                passwordEncoder.encode(clientSecret)
        );

        application.setScopes(
                serializeScopes(
                        validateApplicationScopes(request.scopes())
                )
        );

        Application saved = applicationRepository.save(application);

        return new ApplicationDtos.CreateApplicationResponse(
                toResponse(saved),
                clientSecret
        );
    }

    @Transactional(readOnly = true)
    public ApplicationDtos.ApplicationResponse get(
            UUID applicationId,
            UUID callerInstitutionId
    ) {
        Application application = findApplication(
                applicationId,
                callerInstitutionId
        );

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationDtos.ApplicationResponse> list(
            UUID callerInstitutionId
    ) {
        return applicationRepository
                .findByInstitution_InstitutionId(callerInstitutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApplicationDtos.ApplicationResponse updateStatus(
            UUID applicationId,
            UUID callerInstitutionId,
            ApplicationDtos.UpdateStatusRequest request
    ) {
        Application application = findApplication(
                applicationId,
                callerInstitutionId
        );

        String status = request.status()
                .trim()
                .toUpperCase();

        if (!List.of("ACTIVE", "INACTIVE").contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be ACTIVE or INACTIVE"
            );
        }

        application.setStatus(status);

        return toResponse(
                applicationRepository.save(application)
        );
    }

    public ApplicationDtos.ApplicationResponse updateScopes(
            UUID applicationId,
            UUID callerInstitutionId,
            ApplicationDtos.UpdateScopesRequest request
    ) {
        Application application = findApplication(
                applicationId,
                callerInstitutionId
        );

        List<String> scopes = validateApplicationScopes(
                request.scopes()
        );

        application.setScopes(
                serializeScopes(scopes)
        );

        return toResponse(
                applicationRepository.save(application)
        );
    }

    public ApplicationDtos.RotateSecretResponse rotateSecret(
            UUID applicationId,
            UUID callerInstitutionId
    ) {
        Application application = findApplication(
                applicationId,
                callerInstitutionId
        );

        String clientSecret = generateClientSecret();

        application.setClientSecretHash(
                passwordEncoder.encode(clientSecret)
        );

        applicationRepository.save(application);

        return new ApplicationDtos.RotateSecretResponse(
                application.getApplicationId(),
                application.getClientId(),
                clientSecret
        );
    }

    private Application findApplication(
            UUID applicationId,
            UUID callerInstitutionId
    ) {
        /*
         * Return 404 rather than 403 on a cross-institution ID
         * so callers cannot use this endpoint to enumerate
         * whether an application ID exists under another institution.
         */
        return applicationRepository
                .findByApplicationIdAndInstitution_InstitutionId(
                        applicationId,
                        callerInstitutionId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Application not found: " + applicationId
                ));
    }

    private ApplicationDtos.ApplicationResponse toResponse(
            Application application
    ) {
        return new ApplicationDtos.ApplicationResponse(
                application.getApplicationId(),
                application.getInstitution().getInstitutionId(),
                application.getClientId(),
                application.getName(),
                application.getStatus(),
                parseScopes(application.getScopes()),
                application.getCreatedAt().toString()
        );
    }

    private List<String> validateApplicationScopes(
            List<String> scopes
    ) {
        List<String> normalizedScopes = normalizeScopes(scopes);

        List<String> forbiddenScopes = normalizedScopes.stream()
                .filter(FORBIDDEN_APPLICATION_SCOPES::contains)
                .toList();

        if (!forbiddenScopes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Case scopes cannot be assigned to institution applications"
            );
        }

        return normalizedScopes;
    }

    private List<String> normalizeScopes(
            List<String> scopes
    ) {
        return scopes.stream()
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .distinct()
                .toList();
    }

    private String serializeScopes(
            List<String> scopes
    ) {
        try {
            return objectMapper.writeValueAsString(
                    normalizeScopes(scopes)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Unable to serialize application scopes",
                    e
            );
        }
    }

    private List<String> parseScopes(
            String scopes
    ) {
        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    scopes,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    String.class
                            )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Invalid stored application scopes",
                    e
            );
        }
    }

    private String generateUniqueClientId() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String clientId = "tamva_" +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "");

            if (applicationRepository
                    .findByClientId(clientId)
                    .isEmpty()) {
                return clientId;
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique client ID"
        );
    }

    private String generateClientSecret() {
        byte[] secret = new byte[32];

        SECURE_RANDOM.nextBytes(secret);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(secret);
    }
}