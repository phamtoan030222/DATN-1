package com.sd20201.datn.infrastructure.secutiry.config;

import com.sd20201.datn.infrastructure.constant.MappingConstants;
import com.sd20201.datn.infrastructure.constant.RoleConstant;
import com.sd20201.datn.infrastructure.secutiry.exception.RestAuthenticationEntryPoint;
import com.sd20201.datn.infrastructure.secutiry.filter.TokenAuthenticationFilter;
import com.sd20201.datn.infrastructure.secutiry.oauth2.CustomOAuth2UserService;
import com.sd20201.datn.infrastructure.secutiry.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sd20201.datn.infrastructure.secutiry.oauth2.OAuth2AuthenticationFailureHandler;
import com.sd20201.datn.infrastructure.secutiry.oauth2.OAuth2AuthenticationSuccessHandler;
import com.sd20201.datn.infrastructure.secutiry.service.CustomUserDetailsService;
import com.sd20201.datn.utils.Helper;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Setter(onMethod = @__({@Autowired}))
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Setter(onMethod = @__({@Autowired}))
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Setter(onMethod = @__({@Autowired}))
    private CustomOAuth2UserService customOAuth2UserService;

    @Setter(onMethod = @__({@Autowired}))
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${frontend.url}")
    private String allowedOrigin;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    public AuthenticationManager authenticationManager(
            PasswordEncoder passwordEncoder,
            CustomUserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return new ProviderManager(daoAuthenticationProvider);
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        source.registerCorsConfiguration("/**", config.applyPermitDefaultValues());
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "*"));
        config.setAllowedOrigins(Collections.singletonList(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS","PATCH"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(c -> c.configurationSource(corsConfigurationSource()));
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.exceptionHandling(e -> e.authenticationEntryPoint(new RestAuthenticationEntryPoint()));

//        http.authorizeHttpRequests(
//                authorizeRequests -> authorizeRequests.requestMatchers(MappingConstants.API_LOGIN).permitAll()
//        );
//
//        http.authorizeHttpRequests(
//                authorizeRequests -> authorizeRequests.requestMatchers(Helper.appendWildcard(MappingConstants.API_TEST)).hasAnyAuthority(RoleConstant.QUAN_LY.name())
//        );
//
//        http.oauth2Login(
//                oauth2 -> oauth2.authorizationEndpoint(a -> a.baseUri("/oauth2/authorize"))
//                        .redirectionEndpoint(r -> r.baseUri("/oauth2/callback/**"))
//                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
//                        .authorizationEndpoint(a -> a.authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
//                        .successHandler(oAuth2AuthenticationSuccessHandler)
//                        .failureHandler(oAuth2AuthenticationFailureHandler)
//        );
//
//        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
