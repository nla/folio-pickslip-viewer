package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.domain.FolioRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestEditService {

  private final FolioService folioService;

  private static final String NLA_CIRC_RETRIEVALS_EDIT_REQUESTS_ROLE_UUID = "f985adab-ede9-4ebf-bcf3-5262f1ac6969";

  public FolioRequest getRequestById(final String requestId) throws IOException {
    return folioService.folioApiGetRequestById(requestId);
  }

  public boolean userCanEditRequest(final String username) throws IOException {
    List<String> roles = folioService.getFolioRolesForUser(username);
    return roles.stream().anyMatch(NLA_CIRC_RETRIEVALS_EDIT_REQUESTS_ROLE_UUID::equals);
  }
}