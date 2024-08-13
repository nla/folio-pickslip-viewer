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

  private static final String NLA_CIRC_RETRIEVALS_GENERAL_PERMISSIONS_UUID = "55254dc9-4e4d-445a-bd8d-48f869d4b780";

  public FolioRequest getRequestById(final String requestId) throws IOException {
    return folioService.folioApiGetRequestById(requestId);
  }

  public boolean userCanEditRequest(final String username) throws IOException {
    List<String> permissions = folioService.getFolioPermissionsForUser(username);
    return permissions.stream().anyMatch(NLA_CIRC_RETRIEVALS_GENERAL_PERMISSIONS_UUID::equals);
  }
}