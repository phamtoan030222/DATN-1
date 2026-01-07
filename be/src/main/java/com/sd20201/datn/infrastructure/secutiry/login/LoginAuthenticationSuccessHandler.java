package com.sd20201.datn.infrastructure.secutiry.login;

import com.sd20201.datn.infrastructure.secutiry.response.TokenUriResponse;
import com.sd20201.datn.infrastructure.secutiry.service.RefreshTokenService;
import com.sd20201.datn.infrastructure.secutiry.service.TokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class LoginAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Setter(onMethod_ = @Autowired)
    private TokenProvider tokenProvider;

    @Setter(onMethod_ = @Autowired)
    private RefreshTokenService refreshTokenService;

    @Value("${frontend.url}")
    private String DEFAULT_TARGET_URL_TUTOR_CLIENT;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("run on LoginAuthenticationSuccessHandler success");

        handleResponse(request, response, authentication);
        if (response.isCommitted()) {
            log.warn("Response has already been committed. Unable to redirect to target URL");
            return;
        }

        clearAuthenticationAttributes(request);
    }

    protected void handleResponse(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Authentication authentication
    ) {
        try {

            String token = tokenProvider.createTokenByLogin(authentication);
            String refreshToken = refreshTokenService.createRefreshToken(authentication).getRefreshToken();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("""
                        {
                          "state": "%s"
                        }
                    """.formatted(TokenUriResponse.getState(token, refreshToken)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
