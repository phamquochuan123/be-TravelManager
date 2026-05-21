package com.example.travelManager.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.travelManager.filter.JwtRequestFilter;
import com.example.travelManager.service.AppUserDetailsService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // Cấu hình trong application.properties: app.cors.allowed-origins=http://localhost:5173,http://localhost:5174
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:5174}")
    private String allowedOrigins;

    private final AppUserDetailsService appUserDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SecurityConfig(AppUserDetailsService appUserDetailsService,
            JwtRequestFilter jwtRequestFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) {
        this.appUserDetailsService = appUserDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/send-reset-otp", "/reset-password",
                                "/logout", "/admin/setup", "/verify-otp", "/send-otp")
                        .permitAll()
                        // VNPay IPN callback — không cần auth (VNPay server gọi)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/payment/ipn", "/payment/result").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/payment/create").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/payment/my").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/admin/payments").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/admin/users/*/lock", "/admin/users/*/unlock").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("ADMIN", "STAFF")
                        // Hotel & Tour: GET public, booking authenticated, POST/PUT/DELETE chỉ ADMIN hoặc STAFF
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/hotels", "/hotels/**", "/rooms", "/rooms/**", "/room/**",
                                "/bookings/confirmation/**",
                                "/tours", "/tours/**",
                                "/restaurants", "/restaurants/**",
                                "/destinations", "/destinations/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/hotels/*/rooms/*/bookings").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/restaurants/*/bookings").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/tour-bookings/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/hotels/**", "/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/hotels/**", "/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/hotels/**", "/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/hotels/**", "/rooms/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/restaurants/**", "/destinations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                "/restaurants/**", "/destinations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                "/restaurants/**", "/destinations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                "/restaurants/**", "/destinations/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/tour-bookings/my",
                                "/restaurants/bookings/my").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/tour-bookings/all",
                                "/restaurants/bookings/all", "/bookings/all").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/tour-bookings/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/restaurants/bookings/**").authenticated()
                        .requestMatchers("/bookings/**").authenticated()
                        // Tour reviews: GET public, POST authenticated, admin actions ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/tours/*/reviews").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/tours/*/reviews/*").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/tours/*/reviews/*/reply",
                                "/tours/*/reviews/*/hide", "/tours/*/reviews/*/unhide").hasRole("ADMIN")
                        // Hotel reviews
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/hotels/*/reviews", "/hotels/*/reviews/all").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/hotels/*/reviews").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/hotels/*/reviews/*/reply",
                                "/hotels/*/reviews/*/hide", "/hotels/*/reviews/*/unhide").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/hotels/*/reviews/*").hasRole("ADMIN")
                        // Restaurant reviews
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/restaurants/*/reviews", "/restaurants/*/reviews/all").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/restaurants/*/reviews").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/restaurants/*/reviews/*/reply",
                                "/restaurants/*/reviews/*/hide", "/restaurants/*/reviews/*/unhide").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/restaurants/*/reviews/*").hasRole("ADMIN")
                        // Tour seasonal prices: GET public, CRUD for admin/staff
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/tours/*/seasonal-prices").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/tours/*/seasonal-prices").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/tours/*/seasonal-prices/*").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/tours/*/seasonal-prices/*").hasAnyRole("ADMIN", "STAFF")
                        // Assign staff to departure
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/tours/*/departures/*/assign-staff").hasAnyRole("ADMIN")
                        // Tour coupons: validate authenticated, CRUD for admin/staff
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/tour-coupons/validate").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/tour-coupons/**").hasAnyRole("ADMIN", "STAFF")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/tour-coupons/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/tour-coupons/**").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/tour-coupons/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .logout(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
