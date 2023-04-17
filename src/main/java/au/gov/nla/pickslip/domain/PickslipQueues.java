package au.gov.nla.pickslip.domain;

import au.gov.nla.pickslip.StackLocations;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Representation of request queues in FOLIO. This is the core data model for this application. This structure is
   built up from the following four sources:

     - data in the configuration file (stack codes and display names)
     - 'pickslip' data from FOLIO (api call)
     - 'service point' data from FOLIO (api call, linked by stack code in configuration file)
     - 'request' data from FOLIO (api call)

   When a request associated with a pickslip changes from "Open - Not yet filled" to "Open - In transit" - it no
   longer appears in pickslip data (from the pickslip api).  For this reason, certain information (such as "title")
   is no longer available to this application, and so can't be displayed.  (It's a requirement that these 'In transit"
   requests are shown alongside information about actual pickslips, differentiated only by status.)

   Also of note - the model has a concept of "visiting" pickslips.  A pickslip can be a visitor to another stack if it's
   associated request has a tag which is equal to the stack's code.  This is to support workflow whereby requests can be
   temporarily 'sent' to another stack for filtering or modification  (e.g. Pictures and Manuscripts.)
 */
public class PickslipQueues {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private LocalDateTime lastUpdated;

  // backing source data - FOLIO and config file data.

  // all not filled requests
  private List<FolioRequest> sourceNotFilledRequests;
  private List<FolioServicePoint> sourceServicePoints;

  // service point code -> pickslips
  private Map<String, List<FolioPickslip>> sourcePickSlips;

  // derived data model for this app.
  private Map<ServicePoint, List<Pickslip>> servicePointPickslips;

  // domain model of a stack service point - driven by config file
  public record ServicePoint(
      String id, // FOLIO UUID
      String code, // config file and FOLIO
      String label // config file
      ) {}
  ;

  // domain model of a pickslip, constructed from FOLIO request and pickslip data.
  public record Pickslip(
      boolean
          visiting, // contains at least one tag which corresponds to another Service Point / Stack
                    // - flagged for attention by another stack area.
      Request request,
      Item item,
      Instance instance) {
    public record Request(
        String id,
        Requester requester,
        LocalDateTime requestDate,
        String patronComments,
        String patronGroup,
        String status,
        String position,
        List<String> tagList) {

      public record Requester(String id, String barcode, String firstName, String lastName) {}
      ;

      public enum Status {
        // FOLIO code values of interest
        OPEN_NOT_YET_FILLED("Open - Not yet filled"),
        OPEN_IN_TRANSIT("Open - In transit");

        private final String code;

        public String getCode() {
          return this.code;
        }

        Status(String code) {
          this.code = code;
        }
      }

      public boolean isNotYetFilled() {
        return Status.OPEN_NOT_YET_FILLED.getCode().equalsIgnoreCase(status);
      }
    }
    ;

    public record Item(
        String id, // request
        String title, // pickslip
        String primaryContributor, // pickslip
        String allContributors, // pickslip
        String descriptionOfPieces, // pickslip
        String barcode, // pickslip
        String callNumber, // pickslip,
        String chronology,
        String enumeration,
        String effectiveLocationSpecific) {}
    ;

    public record Instance(String id, String title) {}
    ;

    // return true if String 's' is in the 'list', unless it's equal to the 'except' value.
    private static boolean inList(String s, String except, List<String> list) {
      return !s.equalsIgnoreCase(except) && list.stream().anyMatch(s::equalsIgnoreCase);
    }

    static Pickslip fromFolioPickSlipAndRequest(
        List<String> servicePointCodes, String except, FolioPickslip fps, FolioRequest fr) {

      String UNAVAILABLE = "(Unavailable)";

      Request request =
          new Request(
              fr.id(),
              new Request.Requester(
                  fr.requesterId(),
                  fr.getRequester().barcode(),
                  fps == null ? UNAVAILABLE : fps.requester().firstName(),
                  fps == null ? UNAVAILABLE : fps.requester().lastName()),
              fr.requestDate(),
              fr.patronComments(),
              fr.requester().patronGroupGroup(),
              fr.status(),
              fr.position(),
              fr.tagList());

      Item item;

      if (fps == null) {
        item =
            new Item(
                fr.itemId(),
                UNAVAILABLE, // title
                UNAVAILABLE, // primaryContributor
                UNAVAILABLE, // allContributors
                UNAVAILABLE, // descriptionOfPieces
                fr.item().barcode(),
                fr.item().callNumber(),
                UNAVAILABLE, // chronology
                UNAVAILABLE, // enumeration
                UNAVAILABLE // effectiveLocationSpecific
                );
      } else {
        item =
            new Item(
                fr.itemId(),
                fps.item().title(),
                fps.item().primaryContributor(),
                fps.item().allContributors(),
                fps.item().descriptionOfPieces(),
                fps.item().barcode(),
                fps.item().callNumber(),
                fps.item().chronology(),
                fps.item().enumeration(),
                fps.item().effectiveLocationSpecific());
      }

      Instance instance = new Instance(fr.instanceId(), fr.instance().title());

      boolean visiting = false;
      var tagList = fr.tagList();
      if (tagList != null) {
        if (tagList.stream().anyMatch(s -> inList(s, except, servicePointCodes))) {
          visiting = true;
        }
      }

      return new Pickslip(visiting, request, item, instance);
    }
  }
  ;

  // return matching FolioRequest from list
  private FolioPickslip findFolioPickslipByRequestId(
      Map<FolioServicePoint, List<FolioPickslip>> folioPickslipsByServicePoint, String requestId) {
    for (var fpsList : folioPickslipsByServicePoint.values()) {
      if (fpsList != null) {
        for (var fps : fpsList) {
          if (requestId.equals(fps.requestId())) {
            return fps;
          }
        }
      }
    }
    return null;
  }

  // update model with new data
  public void update(
      StackLocations stackLocations, // config file, unchanging
      List<FolioRequest> folioNotFilledRequests, // all not filled requests from FOLIO
      List<FolioServicePoint> folioServicePoints, // all service point records from FOLIO
      List<FolioLocation>
          folioLocations, // all locations with link to primary service point (locations are in
                          // folio requests)
      Map<FolioServicePoint, List<FolioPickslip>>
          folioPickslipsByServicePoint) { // all pickslips for NOT YET FILLED requests for all
                                          // service points

    // loc->FolioSp
    List<ServicePoint> servicePoints =
        stackLocations.getStacks().stream()
            .map(
                location ->
                    new ServicePoint(
                        servicePointByCode(location.code(), folioServicePoints).id(),
                        location.code(),
                        location.label()))
            .toList();

    List<String> servicePointCodes = servicePoints.stream().map(ServicePoint::code).toList();

    // code - >service point
    Map<String, ServicePoint> locationServicePointMap = new HashMap<>();
    for (var fLoc : folioLocations) {
      for (var xSp : servicePoints) {
        if (xSp.id().equalsIgnoreCase(fLoc.primaryServicePoint())) {
          locationServicePointMap.put(fLoc.code(), xSp);
        }
      }
    }

    Map<ServicePoint, List<Pickslip>> servicePointPickslips = new HashMap<>();

    for (var req : folioNotFilledRequests) {

      FolioPickslip fps = findFolioPickslipByRequestId(folioPickslipsByServicePoint, req.id());
      ServicePoint sp = locationServicePointMap.get(req.item().location().code());
      if (sp == null) {
        throw new IllegalStateException(String.format("Service Point null for request: %s", req));
      }
      Pickslip pickslip =
          Pickslip.fromFolioPickSlipAndRequest(servicePointCodes, sp.code, fps, req);

      // add pickslip
      List<Pickslip> pickslips = servicePointPickslips.get(sp);
      if (pickslips == null) {
        pickslips = new ArrayList<>();
      }
      pickslips.add(pickslip);
      servicePointPickslips.put(sp, pickslips);
    }

    // sort: status; then by request date.  Head of list is most recent; "In transit" status at end.
    for (var pickslips : servicePointPickslips.values()) {
      pickslips.sort(
          Comparator.comparing((Pickslip a) -> a.request.status)
              .thenComparing(a -> a.request.requestDate)
              .reversed());
    }

    this.servicePointPickslips = servicePointPickslips;
    this.lastUpdated = LocalDateTime.now();
  }

  // return matching FolioServicePoint from list.
  private FolioServicePoint servicePointByCode(String code, List<FolioServicePoint> servicePoints) {
    return servicePoints.stream()
        .filter(sp -> code.equals(sp.code()))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Code not present in FOLIO ServicePoints: " + code));
  }

  // get items from other stack locations which are temporarily reassigned to this stack.
  // for all pickslips - pick out the ones with a tag which corresponds to stackCode.
  public List<Pickslip> getVisitorsForStack(String stackCode) {
    List<Pickslip> result = new ArrayList<>();

    for (var servicePoint : servicePointPickslips.keySet()) {
      if (!stackCode.equalsIgnoreCase(servicePoint.code())) {
        var pickslips = servicePointPickslips.get(servicePoint);
        if (pickslips != null) {
          for (var pickslip : pickslips) {
            List<String> tags = pickslip.request.tagList;
            if (tags != null) {
              if (tags.stream().anyMatch(s -> stackCode.equalsIgnoreCase(s))) {
                result.add(pickslip);
              }
            }
          }
        }
      }
    }
    return result;
  }

  public List<Pickslip> getPickslipsForStack(String stackCode) {
    var key =
        this.servicePointPickslips.keySet().stream()
            .filter(k -> stackCode.equals(k.code()))
            .findFirst()
            .orElse(null);
    return key == null ? null : this.servicePointPickslips.get(key);
  }

  // find a Pickslip by RequestId
  public Pickslip getPickslipByRequestId(String requestId) {

    if (servicePointPickslips == null || servicePointPickslips.values() == null) {
      return null;
    }

    for (var pickslips : servicePointPickslips.values()) {
      for (var pickslip : pickslips) {
        if (pickslip != null && requestId.equalsIgnoreCase(pickslip.request().id())) {
          return pickslip;
        }
      }
    }
    return null;
  }
}
