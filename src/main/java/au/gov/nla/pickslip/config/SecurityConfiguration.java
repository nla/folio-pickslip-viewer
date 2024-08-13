package au.gov.nla.pickslip.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfiguration {
  static String[] ANONYMOUS_PATHS = {"/export/**", "/bulkprint/**", "/location/**", "/", "/home", "/user/**", "/webjars/**","/css/**"};

  private final KeycloakLogoutHandler keycloakLogoutHandler;


  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorizeHttpRequests ->
                authorizeHttpRequests
                    .requestMatchers(Arrays.stream(ANONYMOUS_PATHS)
                        .map(AntPathRequestMatcher::antMatcher)
                        .toArray(AntPathRequestMatcher[]::new))
                    .permitAll().anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults())
        .logout(logout -> logout.addLogoutHandler(keycloakLogoutHandler).logoutSuccessUrl("/"));
    return http.build();
  }
}
