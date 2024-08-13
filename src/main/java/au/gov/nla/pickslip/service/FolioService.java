package au.gov.nla.pickslip.service;

import au.gov.nla.folio.api.FOLIOLocationsRetrieverAPI;
import au.gov.nla.folio.api.FOLIOPatronPermissionsAPI;
import au.gov.nla.folio.api.FOLIOPatronRetrieverAPI;
import au.gov.nla.folio.api.FOLIOPickslipsRetrieverAPI;
import au.gov.nla.folio.api.FOLIORequestsRetrieverAPI;
import au.gov.nla.folio.api.FOLIOServicePointRetrieverAPI;
import au.gov.nla.folio.api.credentials.FOLIOAPICredentials;
import au.gov.nla.folio.util.FOLIOAPIUtils;
import au.gov.nla.pickslip.domain.FolioInstance;
import au.gov.nla.pickslip.domain.FolioLocation;
import au.gov.nla.pickslip.domain.FolioPickslip;
import au.gov.nla.pickslip.domain.FolioRequest;
import au.gov.nla.pickslip.domain.FolioServicePoint;
import au.gov.nla.pickslip.domain.PickslipQueues;
import au.gov.nla.pickslip.dto.RequestNoteDto;
import au.gov.nla.pickslip.service.async.FolioAsyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class FolioService {

  @Value("${folio.requests.limit}")
  private int folioRequestsLimit;

  @Value("${folio.pickslips.limit}")
  private int folioPickslipsLimit;

  @Value("#{${folioConfigMap}}")
  private Map<String, String> folioOkapiCredentialsMap;

  // delegate all async
  @Autowired
  FolioAsyncService folioAsyncService;

  private FOLIOAPICredentials folioOkapiCredentials;

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @PostConstruct
  public void init() {
    folioOkapiCredentials = FOLIOAPIUtils.toFOLIOAPICredentials(this.folioOkapiCredentialsMap);
  }

  public Map<String, FolioInstance> getFolioInstances(List<String> instanceIds)
      throws JsonProcessingException {

    List<CompletableFuture<FolioInstance>> futures = new ArrayList<>();
    for (String id : instanceIds) {
      futures.add(folioAsyncService.getFolioInstance(id)); // timeout exists..
    }

    // need to not put same in twice (key repeat) - merge
    return futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toMap(FolioInstance::id, v -> v, (k1, k2) -> k1));
  }

  public List<FolioServicePoint> getFolioServicePoints() throws IOException {

    JsonNode n = folioApiGetServicePoints();

    if (n == null) {
      log.debug("No service points found.");
      return null;
    }

    ArrayList<FolioServicePoint> result = new ArrayList<>();

    n.at("/servicepoints")
        .forEach(
            r -> {
              FolioServicePoint sp =
                  new FolioServicePoint(
                      r.at("/id")
                          .asText(null),
                      r.at("/code")
                          .asText(null),
                      r.at("/name")
                          .asText(null),
                      r.at("/discoveryDisplayName")
                          .asText(null));

              result.add(sp);
            });

    return result;
  }

  public List<FolioLocation> getFolioLocations() throws IOException {

    JsonNode n = folioApiGetFolioLocations();

    if (n == null) {
      log.debug("No locations found.");
      return null;
    }

    ArrayList<FolioLocation> result = new ArrayList<>();

    n.at("/locations")
        .forEach(
            r -> {
              FolioLocation loc =
                  new FolioLocation(
                      r.at("/id")
                          .asText(null),
                      r.at("/code")
                          .asText(null),
                      r.at("/name")
                          .asText(null),
                      r.at("/primaryServicePoint")
                          .asText(null));
              result.add(loc);
            });

    return result;
  }

  public Map<FolioServicePoint, List<FolioPickslip>> getPickslipsForServicePoints(
      List<FolioServicePoint> servicePoints) throws IOException {

    Map<FolioServicePoint, List<FolioPickslip>> result = new HashMap<>();
    for (var sp : servicePoints) {
      var pickslips = getPickslipsForServicePoint(sp.id());

      log.debug(
          "{} pickslips retrieved for {}", pickslips == null ? 0 : pickslips.size(), sp.code());

      result.put(sp, pickslips);
    }

    return result;
  }

  public List<FolioPickslip> getPickslipsForServicePoint(String id) throws IOException {

    JsonNode n = folioApiGetPickslipsForServicePoint(id, folioPickslipsLimit);

    if (n == null) {
      log.debug("No pickslips for {}", id);
      return null;
    }

    ArrayList<FolioPickslip> result = new ArrayList<>();

    n.at("/pickslips")
        .forEach(
            r -> {
              FolioPickslip pick =
                  new FolioPickslip(
                      r.at("/requestId")
                          .asText(),
                      new FolioPickslip.Item(
                          r.at("/item/title")
                              .asText(null),
                          r.at("/item/primaryContributor")
                              .asText(null),
                          r.at("/item/allContributors")
                              .asText(null),
                          r.at("/item/barcode")
                              .asText(null),
                          r.at("/item/descriptionOfPieces")
                              .asText(null),
                          r.at("/item/callNumber")
                              .asText(null),
                          r.at("/item/chronology")
                              .asText(null),
                          r.at("/item/enumeration")
                              .asText(null),
                          r.at("/item/effectiveLocationSpecific")
                              .asText(null),
                          r.at("/item/yearCaption")
                              .asText(null),
                          r.at("/item/copy")
                              .asText(null)),
                      new FolioPickslip.Requester(
                          r.at("/requester/firstName")
                              .asText(null),
                          r.at("/requester/lastName")
                              .asText(null),
                          r.at("/requester/barcode")
                              .asText(null)));

              result.add(pick);
            });

    return result;
  }

  private List<String> toList(Iterator<JsonNode> n) {
    ArrayList<String> result = new ArrayList<>();
    n.forEachRemaining(s -> result.add(s.asText()));
    return result;
  }

  public List<FolioRequest> getFolioRequests() throws IOException {

    JsonNode n = folioApiGetRequests();

    ArrayList<FolioRequest> result = new ArrayList<>();

    if (n != null) {
      n.at("/requests")
          .forEach(
              r -> {
                var requestDateNode = r.at("/requestDate");

                ZonedDateTime requestDate =
                    requestDateNode.isNull()
                        ? null
                        : ZonedDateTime.parse(
                        requestDateNode.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                ZonedDateTime localRequestDate =
                    requestDate.withZoneSameInstant(ZoneId.systemDefault());

                FolioRequest req =
                    new FolioRequest(
                        r.at("/id")
                            .asText(null),
                        localRequestDate,
                        r.at("/patronComments")
                            .asText(null),
                        r.at("/itemId")
                            .asText(null),
                        r.at("/instanceId")
                            .asText(null),
                        r.at("/requesterId")
                            .asText(null),
                        r.at("/status")
                            .asText(null),
                        r.at("/cancellationAdditionalInformation")
                            .asText(null),
                        r.at("/position")
                            .asText(null),
                        new FolioRequest.Instance(r.at("/instance/title")
                            .asText(null)),
                        new FolioRequest.Item(
                            r.at("/item/barcode")
                                .asText(null),
                            r.at("/item/callNumber")
                                .asText(null),
                            new FolioRequest.Item.Location(
                                r.at("/item/location/name")
                                    .asText(null),
                                r.at("/item/location/code")
                                    .asText(null))),
                        new FolioRequest.Requester(
                            r.at("/requester/barcode")
                                .asText(null),
                            r.at("/requester/patronGroupGroup")
                                .asText(null)),
                        toList(r.at("/tagList")
                            .elements()));

                result.add(req);
              });
    }

    return result;
  }
  // for spy / mock accessibility:

  protected JsonNode folioApiGetServicePoints() throws IOException {
    return new FOLIOServicePointRetrieverAPI(folioOkapiCredentials).getServicePoints();
  }

  protected JsonNode folioApiGetFolioLocations() throws IOException {
    return new FOLIOLocationsRetrieverAPI(folioOkapiCredentials).getLocations();
  }

  protected JsonNode folioApiGetPickslipsForServicePoint(String id, int folioPickslipsLimit)
      throws IOException {
    return new FOLIOPickslipsRetrieverAPI(folioOkapiCredentials)
        .getPickslipsForServicePoint(id, folioPickslipsLimit);
  }

  protected JsonNode folioApiGetRequests() throws IOException {
    return new FOLIORequestsRetrieverAPI(folioOkapiCredentials)
        .getRequestsByStatus(
            List.of(
                PickslipQueues.Pickslip.Request.Status.OPEN_NOT_YET_FILLED.getCode(),
                PickslipQueues.Pickslip.Request.Status.OPEN_IN_TRANSIT.getCode()),
            folioRequestsLimit);
  }

  public FolioRequest folioApiGetRequestById(final String requestId) throws IOException {
    JsonNode folioRequestJson =
        new FOLIORequestsRetrieverAPI(folioOkapiCredentials).getRequestById(requestId);

    JsonNode requestDateNode = folioRequestJson.at("/requestDate");
    ZonedDateTime requestDate =
        requestDateNode.isNull()
            ? null
            : ZonedDateTime.parse(
            requestDateNode.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    assert requestDate != null;
    ZonedDateTime localRequestDate =
        requestDate.withZoneSameInstant(ZoneId.systemDefault());

    return
        new FolioRequest(
            folioRequestJson.at("/id")
                .asText(null),
            localRequestDate,
            folioRequestJson.at("/patronComments")
                .asText(null),
            folioRequestJson.at("/itemId")
                .asText(null),
            folioRequestJson.at("/instanceId")
                .asText(null),
            folioRequestJson.at("/requesterId")
                .asText(null),
            folioRequestJson.at("/status")
                .asText(null),
            folioRequestJson.at("/cancellationAdditionalInformation")
                .asText(null),
            folioRequestJson.at("/position")
                .asText(null),
            new FolioRequest.Instance(folioRequestJson.at("/instance/title")
                .asText(null)),
            new FolioRequest.Item(
                folioRequestJson.at("/item/barcode")
                    .asText(null),
                folioRequestJson.at("/item/callNumber")
                    .asText(null),
                new FolioRequest.Item.Location(
                    folioRequestJson.at("/item/location/name")
                        .asText(null),
                    folioRequestJson.at("/item/location/code")
                        .asText(null))),
            new FolioRequest.Requester(
                folioRequestJson.at("/requester/barcode")
                    .asText(null),
                folioRequestJson.at("/requester/patronGroupGroup")
                    .asText(null)),
            toList(folioRequestJson.at("/tagList")
                .elements()));
  }

  public void updateRequest(final RequestNoteDto requestNoteDto) throws IOException {
    FOLIORequestsRetrieverAPI folioRequestsRetrieverAPI =
        new FOLIORequestsRetrieverAPI(folioOkapiCredentials);
    JsonNode folioRequestJson =
        folioRequestsRetrieverAPI.getRequestById(requestNoteDto.getRequestId());
    if (folioRequestJson != null) {
      ((ObjectNode) folioRequestJson).put("cancellationAdditionalInformation",
          requestNoteDto.getCancellationAdditionalInformation());
      folioRequestsRetrieverAPI.updateRequest(requestNoteDto.getRequestId(), folioRequestJson);
    }
    else {
      log.error("Could not retrieve request with id: {}", requestNoteDto.getRequestId());
    }
  }

  public List<String> getFolioPermissionsForUser(final String username) throws IOException {
    List<String> permissions = new ArrayList<>();

    if (Strings.isBlank(username)) {
      log.error("Username is empty");
      return permissions;
    }

    FOLIOPatronRetrieverAPI folioPatronRetrieverAPI =
        new FOLIOPatronRetrieverAPI(folioOkapiCredentials);
    JsonNode userJsonNode = folioPatronRetrieverAPI.getUserByUsername(username.trim());
    if (userJsonNode == null) {
      log.error("Folio record for user with name: {} not found", username);
      return permissions;
    }

    String userId = userJsonNode.at("/id")
        .asText("");
    if (userId.isEmpty()) {
      log.error("Folio record for user with name: {} has no id", username);
      return permissions;
    }

    FOLIOPatronPermissionsAPI folioPatronPermissionsAPI =
        new FOLIOPatronPermissionsAPI(folioOkapiCredentials);
    JsonNode permissionsJsonNode = folioPatronPermissionsAPI.getPermissionsForUserId(userId);
    if (permissionsJsonNode == null) {
      log.error("Unable to retrieve Folio permissions for user id: {}", userId);
      return permissions;
    }

    JsonNode permissionsArray = permissionsJsonNode.at("/permissionNames");
    if (permissionsArray.isMissingNode() || !permissionsArray.isArray()) {
      log.error("Unable to find permissions list in Folio response for user id: {}", userId);
      return permissions;
    }

    for (JsonNode jsonNode : permissionsArray) {
      permissions.add(jsonNode.asText());
    }

    return permissions;
  }
}
