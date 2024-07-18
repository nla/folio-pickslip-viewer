package au.gov.nla.pickslip.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.ZonedDateTime;
import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FolioRequest(
    String id,
    ZonedDateTime requestDate,
    String patronComments,
    String itemId,
    String instanceId,
    String requesterId,
    String status,
    String cancellationAdditionalInformation,
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

  @JsonInclude(value = JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Requester(String barcode, String patronGroupGroup) {}
  ;

  @JsonInclude(value = JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(String barcode, String callNumber, Location location) {
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(String name, String code) {}
    ;
  }


  @JsonInclude(value = JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Instance(String title) {}
  ;
}
