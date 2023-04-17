package au.gov.nla.pickslip.domain;

public record FolioPickslip(String requestId, Item item, Requester requester) {

  public record Item(
      String title,
      String primaryContributor,
      String allContributors,
      String barcode,
      String descriptionOfPieces,
      String callNumber,
      String chronology,
      String enumeration,
      String effectiveLocationSpecific) {}

  public record Requester(String firstName, String lastName, String barcode) {}
  ;
}
