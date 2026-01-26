package com.smlikelion.webfounder.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smlikelion.webfounder.global.dto.response.BaseResponse;
import com.smlikelion.webfounder.global.dto.response.ErrorCode;
import com.smlikelion.webfounder.security.ExceptionHandleFilter;
import com.smlikelion.webfounder.security.JwtAuthenticationFilter;
import com.smlikelion.webfounder.security.JwtTokenProvider;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.PrintWriter;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfig {

    private final JwtTokenProvider tokenProvider; //JwtToken 생성 및 검증
    private final ExceptionHandleFilter exceptionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .httpBasic().disable()
                .csrf().disable() //CSRF 보호 비활성화
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests(authorize ->
                        authorize
                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS
                                .antMatchers("/actuator/**").permitAll() // Actuator 허용
                                .anyRequest().authenticated()
                )
                .addFilterAfter(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(exceptionFilter, JwtAuthenticationFilter.class)
                .exceptionHandling((exceptionConfig) -> {
                    exceptionConfig
                            .authenticationEntryPoint(unauthorizedEntryPoint)
                            .accessDeniedHandler(accessDeniedHandler);
                })
                .cors();

        return httpSecurity.build();
    }


    // 프론트 CORS 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 허용할 프론트 Origin
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "https://localhost:5173",
                "https://likelion-smwu.com",
                "https://admin-client-v3-seven.vercel.app"

        ));

        // 허용 메서드
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용 헤더 (Authorization 포함)
        config.setAllowedHeaders(List.of("*"));

        // 프론트에서 읽을 수 있게 노출할 헤더(필요시)
        config.setExposedHeaders(List.of("Authorization"));

        // 쿠키/세션 기반이면 true 필요, 헤더 JWT여도 프론트가 withCredentials 쓰면 true 필요
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private final AuthenticationEntryPoint unauthorizedEntryPoint =
            (request, response, authException) -> {
                BaseResponse fail = new BaseResponse(ErrorCode.UNAUTHORIZED_ERROR);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                String json = new ObjectMapper().writeValueAsString(fail);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                PrintWriter writer = response.getWriter();
                writer.write(json);
                writer.flush();
            };

    private final AccessDeniedHandler accessDeniedHandler =
            (request, response, accessDeniedException) -> {
                BaseResponse fail = new BaseResponse(ErrorCode.FORBIDDEN_ERROR);
                response.setStatus(HttpStatus.FORBIDDEN.value());
                String json = new ObjectMapper().writeValueAsString(fail);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                PrintWriter writer = response.getWriter();
                System.out.println(json);
                writer.write(json);
                writer.flush();
            };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**",
                "/swagger/**",
                "/api/admin/check-token-validation",
                "/api/admin/signin",
                "/api/admin/signup",
                "/api/project/**",
                "/api/recruit/**",
                "/actuator/**"
        );
    }
}
