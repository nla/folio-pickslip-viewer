package au.gov.nla.pickslip.config;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@AllArgsConstructor
@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt,
    AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt source) {
    return new JwtAuthenticationToken(source,
        Stream.concat(new JwtGrantedAuthoritiesConverter().convert(source)
                .stream(), extractResourceRoles(source).stream())
            .collect(toSet()));
  }

  private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
    if (jwt.hasClaim("realm_access")) {
      List<String> roleList =
          new HashMap<String, List<String>>(jwt.getClaim("realm_access")).get("roles");
      return roleList.isEmpty() ? Collections.emptySet() : roleList.stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
          .collect(toSet());
    }
    return Collections.emptySet();
  }
}