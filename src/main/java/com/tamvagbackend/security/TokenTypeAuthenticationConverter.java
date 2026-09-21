package com.tamvagbackend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

public class TokenTypeAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private static final String INSTITUTION_TOKEN = "institution";
    private static final String USER_TOKEN = "user";
    private static final String ADMIN_TOKEN = "admin";

    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public TokenTypeAuthenticationConverter() {
        this.authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        this.authoritiesConverter.setAuthoritiesClaimName("scope");
        this.authoritiesConverter.setAuthorityPrefix("SCOPE_");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String tokenType = jwt.getClaimAsString(
                TOKEN_TYPE_CLAIM
        );

        validateTokenType(jwt, tokenType);

        Collection<GrantedAuthority> authorities =
                authoritiesConverter.convert(jwt);

        if (authorities == null) {
            throw new BadCredentialsException(
                    "Token contains no valid authorities"
            );
        }

        return new JwtAuthenticationToken(
                jwt,
                authorities
        );
    }

    private void validateTokenType(
            Jwt jwt,
            String tokenType
    ) {
        if (tokenType == null || tokenType.isBlank()) {
            throw new BadCredentialsException(
                    "Token type is required"
            );
        }

        switch (tokenType) {
            case INSTITUTION_TOKEN ->
                    validateInstitutionToken(jwt);

            case USER_TOKEN ->
                    validateUserToken(jwt);

            case ADMIN_TOKEN ->
                    throw new BadCredentialsException(
                            "Admin authentication is not implemented"
                    );

            default ->
                    throw new BadCredentialsException(
                            "Unsupported token type"
                    );
        }
    }

    private void validateInstitutionToken(Jwt jwt) {

        requireClaim(
                jwt,
                "institution_id",
                "Institution token requires institution_id"
        );

        requireClaim(
                jwt,
                "application_id",
                "Institution token requires application_id"
        );
    }

    private void validateUserToken(Jwt jwt) {

        requireClaim(
                jwt,
                "user_id",
                "User token requires user_id"
        );
    }

    private void requireClaim(
            Jwt jwt,
            String claimName,
            String errorMessage
    ) {
        String claimValue = jwt.getClaimAsString(claimName);

        if (claimValue == null || claimValue.isBlank()) {
            throw new BadCredentialsException(errorMessage);
        }
    }
}