package com.tamvagbackend.controller;

import com.tamvagbackend.config.SecurityConfig;
import com.tamvagbackend.dto.UserAuthDtos.*;
import com.tamvagbackend.exception.GlobalExceptionHandler;
import com.tamvagbackend.exception.SecurityExceptionHandler;
import com.tamvagbackend.service.UserAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        SecurityExceptionHandler.class,
        GlobalExceptionHandler.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAuthService userAuthService;

    @Test
    void signupReturnsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(userAuthService.signUp(any()))
                .thenReturn(new SignUpResponse(
                        userId,
                        "ama.mensah@example.com",
                        "Ama",
                        "Mensah",
                        "PENDING_VERIFICATION",
                        customerId,
                        "Account created successfully"
                ));

        mockMvc.perform(
                post("/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ama.mensah@example.com",
                                  "password": "StrongPassword!2026",
                                  "firstName": "Ama",
                                  "lastName": "Mensah",
                                  "phoneNumber": "+233240000000"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.email").value("ama.mensah@example.com"))
        .andExpect(jsonPath("$.firstName").value("Ama"))
        .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
        .andExpect(jsonPath("$.message").value("Account created successfully"));
    }

    @Test
    void signupWithInvalidEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(
                post("/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-a-valid-email",
                                  "password": "StrongPassword!2026",
                                  "firstName": "Ama",
                                  "lastName": "Mensah"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void signinReturnsTokensAndUserInfo() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(userAuthService.signIn(any()))
                .thenReturn(new SignInResponse(
                        "access-token-jwt",
                        "refresh-token-opaque",
                        "Bearer",
                        900,
                        new UserInfo(
                                userId,
                                "ama.mensah@example.com",
                                "Ama",
                                "Mensah",
                                customerId,
                                List.of("CUSTOMER")
                        )
                ));

        mockMvc.perform(
                post("/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ama.mensah@example.com",
                                  "password": "StrongPassword!2026"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("access-token-jwt"))
        .andExpect(jsonPath("$.refreshToken").value("refresh-token-opaque"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(900))
        .andExpect(jsonPath("$.user.email").value("ama.mensah@example.com"));
    }

    @Test
    void signinWithWrongPasswordReturnsUnauthorized() throws Exception {
        when(userAuthService.signIn(any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(
                post("/v1/users/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ama.mensah@example.com",
                                  "password": "WrongPassword"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReturnsNewTokens() throws Exception {
        when(userAuthService.refresh(any()))
                .thenReturn(new RefreshResponse(
                        "new-access-token",
                        "new-refresh-token",
                        "Bearer",
                        900
                ));

        mockMvc.perform(
                post("/v1/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "existing-refresh-token"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void logoutReturnsOk() throws Exception {
        when(userAuthService.logout(any(), any()))
                .thenReturn(new LogoutResponse("Logout successful"));

        mockMvc.perform(
                post("/v1/users/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "some-token"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void meUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meAuthenticatedReturnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        when(userAuthService.getMe(userId))
                .thenReturn(new UserMeResponse(
                        userId,
                        "ama.mensah@example.com",
                        "Ama",
                        "Mensah",
                        "+233240000000",
                        "ACTIVE",
                        customerId,
                        List.of("CUSTOMER")
                ));

        mockMvc.perform(
                get("/v1/users/me")
                        .with(jwt().jwt(builder -> builder
                                .subject(userId.toString())
                                .claim("scope", "profile:read")
                        ))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("ama.mensah@example.com"))
        .andExpect(jsonPath("$.firstName").value("Ama"))
        .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }
}
