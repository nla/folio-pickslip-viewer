package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.config.FolioConfiguration;
import au.gov.nla.pickslip.domain.FolioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestEditService {

  private final FolioService folioService;
  private final FolioConfiguration folioConfiguration;

  public FolioRequest getRequestById(final String requestId) throws IOException {
    return folioService.folioApiGetRequestById(requestId);
  }

  public boolean userCanEditRequest(final String username) throws IOException {
    List<String> roles = folioService.getFolioRolesForUser(username);
    return roles.stream().anyMatch(folioConfiguration.getRequestEditRoleUuid()::equals);
  }
}