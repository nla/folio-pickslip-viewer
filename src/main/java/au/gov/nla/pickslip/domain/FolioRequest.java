package au.gov.nla.pickslip.domain;

import java.time.LocalDateTime;
import java.util.List;

public record FolioRequest(
    String id,
    LocalDateTime requestDate,
    String patronComments,
    String itemId,
    String instanceId,
    String requesterId,
    String status,
    String position,
    Instance instance,
    Item item,
    Requester requester,
    List<String> tagList) {

  public Requester getRequester() {
    return this.requester;
  }

  public Instance getInstance() {
    return this.instance;
  }

  public record Requester(String barcode, String patronGroupGroup) {}
  ;

  public record Item(String barcode, String callNumber, Location location) {
    public record Location(String name, String code) {}
    ;
  }

  public record Instance(String title) {}
  ;
}
