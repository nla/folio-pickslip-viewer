package au.gov.nla.pickslip.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class KeycloakLogoutHandler implements LogoutHandler {

  private final RestClient restClient = RestClient.create();
  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication auth) {
    logoutFromKeycloak((OidcUser) auth.getPrincipal());
  }

  private void logoutFromKeycloak(OidcUser user) {
    String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromUriString(endSessionEndpoint)
            .queryParam("id_token_hint", user.getIdToken().getTokenValue());

    ResponseEntity<String> result = restClient.get()
        .uri(builder.toUriString())
        .retrieve()
        .toEntity(String.class);

    if (result.getStatusCode().is2xxSuccessful()) {
      log.info("Keycloak logout successful: {}", user.getPreferredUsername());
    } else {
      log.warn("Keycloak logout unsuccessful: {}", result.getBody());
    }
  }
}