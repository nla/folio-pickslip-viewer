package au.gov.nla.pickslip.service;

import au.gov.nla.folio.api.*;
import au.gov.nla.folio.api.credentials.FOLIOAPICredentials;
import au.gov.nla.folio.util.FOLIOAPIUtils;
import au.gov.nla.pickslip.domain.*;
import au.gov.nla.pickslip.service.async.FolioAsyncService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FolioService {

  @Value("${folio.requests.limit}")
  private int folioRequestsLimit;

  @Value("${folio.pickslips.limit}")
  private int folioPickslipsLimit;

  @Value("#{${folioConfigMap}}")
  private Map<String, String> folioOkapiCredentialsMap;

  // delegate all async
  @Autowired FolioAsyncService folioAsyncService;

  private FOLIOAPICredentials folioOkapiCredentials;

  private Logger log = LoggerFactory.getLogger(this.getClass());

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

    JsonNode n = new FOLIOServicePointRetrieverAPI(folioOkapiCredentials).getServicePoints();

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
                      r.at("/id").asText(null),
                      r.at("/code").asText(null),
                      r.at("/name").asText(null),
                      r.at("/discoveryDisplayName").asText(null));

              result.add(sp);
            });

    return result;
  }

  public List<FolioLocation> getFolioLocations() throws IOException {
    JsonNode n = new FOLIOLocationsRetrieverAPI(folioOkapiCredentials).getLocations();

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
                      r.at("/id").asText(null),
                      r.at("/code").asText(null),
                      r.at("/name").asText(null),
                      r.at("/primaryServicePoint").asText(null));
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

    JsonNode n =
        new FOLIOPickslipsRetrieverAPI(folioOkapiCredentials)
            .getPickslipsForServicePoint(id, folioPickslipsLimit);

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
                      r.at("/requestId").asText(),
                      new FolioPickslip.Item(
                          r.at("/item/title").asText(null),
                          r.at("/item/primaryContributor").asText(null),
                          r.at("/item/allContributors").asText(null),
                          r.at("/item/barcode").asText(null),
                          r.at("/item/descriptionOfPieces").asText(null),
                          r.at("/item/callNumber").asText(null),
                          r.at("/item/chronology").asText(null),
                          r.at("/item/enumeration").asText(null),
                          r.at("/item/effectiveLocationSpecific").asText(null),
                          r.at("/item/yearCaption").asText(null),
                          r.at("/item/copy").asText(null)),
                      new FolioPickslip.Requester(
                          r.at("/requester/firstName").asText(null),
                          r.at("/requester/lastName").asText(null),
                          r.at("/requester/barcode").asText(null)));

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

    JsonNode n =
        new FOLIORequestsRetrieverAPI(folioOkapiCredentials)
            .getRequestsByStatus(
                List.of(
                    PickslipQueues.Pickslip.Request.Status.OPEN_NOT_YET_FILLED.getCode(),
                    PickslipQueues.Pickslip.Request.Status.OPEN_IN_TRANSIT.getCode()),
                folioRequestsLimit);

    ArrayList<FolioRequest> result = new ArrayList<>();

    if (n != null) {
      n.at("/requests")
          .forEach(
              r -> {
                var requestDateNode = r.at("/requestDate");
                LocalDateTime requestDate =
                    requestDateNode.isNull()
                        ? null
                        : LocalDateTime.parse(
                            requestDateNode.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

                FolioRequest req =
                    new FolioRequest(
                        r.at("/id").asText(null),
                        requestDate,
                        r.at("/patronComments").asText(null),
                        r.at("/itemId").asText(null),
                        r.at("/instanceId").asText(null),
                        r.at("/requesterId").asText(null),
                        r.at("/status").asText(null),
                        r.at("/position").asText(null),
                        new FolioRequest.Instance(r.at("/instance/title").asText(null)),
                        new FolioRequest.Item(
                            r.at("/item/barcode").asText(null),
                            r.at("/item/callNumber").asText(null),
                            new FolioRequest.Item.Location(
                                r.at("/item/location/name").asText(null),
                                r.at("/item/location/code").asText(null))),
                        new FolioRequest.Requester(
                            r.at("/requester/barcode").asText(null),
                            r.at("/requester/patronGroupGroup").asText(null)),
                        toList(r.at("/tagList").elements()));

                result.add(req);
              });
    }

    return result;
  }
}
