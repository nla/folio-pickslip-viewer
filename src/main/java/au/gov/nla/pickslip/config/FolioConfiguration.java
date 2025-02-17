package au.gov.nla.pickslip.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "folio")
@Getter
@Setter
@Slf4j
public class FolioConfiguration {
  private String okapiUrl;
  private String tenant;
  private String username;
  private String password;
}
