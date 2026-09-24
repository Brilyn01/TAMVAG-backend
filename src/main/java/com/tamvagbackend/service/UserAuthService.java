package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.TamvaUser;
import com.tamvagbackend.domain.entity.RefreshTokenSession;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.RefreshTokenSessionRepository;
import com.tamvagbackend.domain.repository.TamvaUserRepository;
import com.tamvagbackend.dto.UserAuthDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class UserAuthService {

    private final TamvaUserRepository tamvaUserRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    private final long userAccessTokenTtlSeconds;
    private final long refreshTokenTtlDays;

    public UserAuthService(
            TamvaUserRepository tamvaUserRepository,
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${tamva.security.user-access-token-ttl-seconds}")
            long userAccessTokenTtlSeconds,
            @Value("${tamva.security.refresh-token-ttl-days}")
            long refreshTokenTtlDays
    ) {
        this.tamvaUserRepository = tamvaUserRepository;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.userAccessTokenTtlSeconds = userAccessTokenTtlSeconds;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (tamvaUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists"
            );
        }

        Customer customer = new Customer(
                UUID.randomUUID(),
                normalizedEmail,
                "INDIVIDUAL",
                "ACTIVE"
        );

        Customer savedCustomer = customerRepository.save(customer);

        TamvaUser user = new TamvaUser();

        /*
         Link the authenticated user to the customer profile
         created during registration.
        */
        user.setCustomer(savedCustomer);

        user.setEmail(normalizedEmail);

        
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());

        user.setPhoneNumber(
                request.phoneNumber() != null
                        && !request.phoneNumber().isBlank()
                        ? request.phoneNumber().trim()
                        : null
        );

        user.setRole("CUSTOMER");

        // Verification behavior remains unchanged for now.
        user.setStatus("PENDING_VERIFICATION");

        TamvaUser savedUser = tamvaUserRepository.save(user);

        return new SignUpResponse(
                savedUser.getUserId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getStatus(),
                savedUser.getCustomerId(),
                "Account created successfully"
        );
    }

    @Transactional
    public SignInResponse signIn(SignInRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        TamvaUser user = tamvaUserRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Verification behavior remains unchanged for now.
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())
                || "CLOSED".equalsIgnoreCase(user.getStatus())) {

            throw new BadCredentialsException(
                    "Account is inactive or suspended"
            );
        }

        String accessToken = generateAccessToken(user);

        String rawRefreshToken = generateOpaqueToken();

        createRefreshTokenSession(user, rawRefreshToken);

        return new SignInResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                userAccessTokenTtlSeconds,
                new UserInfo(
                        user.getUserId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getCustomerId(),
                        List.of(user.getRole())
                )
        );
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String hashedToken = hashToken(
                request.refreshToken().trim()
        );

        RefreshTokenSession session =
                refreshTokenSessionRepository
                        .findByTokenHash(hashedToken)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid refresh token"
                                )
                        );

        if (session.isRevoked()
                || session.getExpiresAt().isBefore(Instant.now())) {

            throw new BadCredentialsException(
                    "Refresh token is expired or revoked"
            );
        }

        session.setRevoked(true);
        refreshTokenSessionRepository.save(session);

        TamvaUser user = session.getUser();

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())
                || "CLOSED".equalsIgnoreCase(user.getStatus())) {

            throw new BadCredentialsException(
                    "Account is inactive or suspended"
            );
        }

        String newAccessToken = generateAccessToken(user);

        String newRawRefreshToken = generateOpaqueToken();

        createRefreshTokenSession(user, newRawRefreshToken);

        return new RefreshResponse(
                newAccessToken,
                newRawRefreshToken,
                "Bearer",
                userAccessTokenTtlSeconds
        );
    }

    @Transactional
    public LogoutResponse logout(
            String rawRefreshToken,
            UUID authenticatedUserId
    ) {
        // Existing logout behavior remains unchanged for now.
        if (rawRefreshToken != null
                && !rawRefreshToken.isBlank()) {

            String hashedToken = hashToken(
                    rawRefreshToken.trim()
            );

            refreshTokenSessionRepository
                    .findByTokenHash(hashedToken)
                    .ifPresent(session -> {
                        session.setRevoked(true);
                        refreshTokenSessionRepository.save(session);
                    });
        }

        if (authenticatedUserId != null) {
            tamvaUserRepository
                    .findById(authenticatedUserId)
                    .ifPresent(user -> {

                        List<RefreshTokenSession> sessions =
                                refreshTokenSessionRepository
                                        .findByUser(user);

                        for (RefreshTokenSession session : sessions) {
                            session.setRevoked(true);
                        }

                        refreshTokenSessionRepository.saveAll(sessions);
                    });
        }

        return new LogoutResponse("Logout successful");
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        TamvaUser user = tamvaUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return mapToUserMeResponse(user);
    }

    @Transactional
    public UserMeResponse updateMe(
            UUID userId,
            UpdateMeRequest request
    ) {
        TamvaUser user = tamvaUserRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (request.firstName() != null
                && !request.firstName().isBlank()) {

            user.setFirstName(
                    request.firstName().trim()
            );
        }

        if (request.lastName() != null
                && !request.lastName().isBlank()) {

            user.setLastName(
                    request.lastName().trim()
            );
        }

        if (request.phoneNumber() != null) {
            user.setPhoneNumber(
                    request.phoneNumber().trim()
            );
        }

        user.setUpdatedAt(Instant.now());

        TamvaUser savedUser =
                tamvaUserRepository.save(user);

        return mapToUserMeResponse(savedUser);
    }

    private UserMeResponse mapToUserMeResponse(
            TamvaUser user
    ) {
        return new UserMeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.getCustomerId(),
                List.of(user.getRole())
        );
    }

    private String generateAccessToken(
            TamvaUser user
    ) {
        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plusSeconds(
                userAccessTokenTtlSeconds
        );

        JwtClaimsSet.Builder claimsBuilder =
                JwtClaimsSet.builder()
                        .issuer("tamva")
                        .issuedAt(issuedAt)
                        .expiresAt(expiresAt)
                        .subject(user.getUserId().toString())
                        .claim(
                                "user_id",
                                user.getUserId().toString()
                        )
                        .claim(
                                "roles",
                                List.of(user.getRole())
                        )
                        .claim(
                                "scope",
                                scopesFor(user)
                        )
                        .claim(
                                "token_type",
                                "user"
                        );

        if (user.getCustomerId() != null) {
            claimsBuilder.claim(
                    "customer_id",
                    user.getCustomerId().toString()
            );
        }

        JwtClaimsSet claims = claimsBuilder.build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();
    }

    /**
         * Returns scopes based on the user's assigned application role.
         *
         * Do not derive authorization from client-supplied values.
         *
         * CUSTOMER allowed:
         * - profile:read
         * - profile:write
         * - risk:evaluate
         
         *
         * Other roles allowed:
         * - profile:read
    */
    private String scopesFor(TamvaUser user) {
        String role = user.getRole() == null
                ? ""
                : user.getRole().trim().toUpperCase();


                return switch (role) {
                        case "CUSTOMER" ->
                                "profile:read profile:write risk:evaluate";

                        default ->
                                "profile:read";
                };
    }

    private void createRefreshTokenSession(
            TamvaUser user,
            String rawRefreshToken
    ) {
        RefreshTokenSession session =
                new RefreshTokenSession(
                        user,
                        hashToken(rawRefreshToken),
                        Instant.now().plus(
                                refreshTokenTtlDays,
                                ChronoUnit.DAYS
                        )
                );

        refreshTokenSessionRepository.save(session);
    }

    private String generateOpaqueToken() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm not available",
                    e
            );
        }
    }
}