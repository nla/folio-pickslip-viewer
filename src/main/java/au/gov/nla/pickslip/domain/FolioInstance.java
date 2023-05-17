package au.gov.nla.pickslip.domain;

// Extra for printing  - /inventory/instances/{instanceId}
public record FolioInstance(
    String id,
    String hrid,
    String publicationPlace, // array flattened
    String publicationDateOfPublication, // array flattened
    String physicalDescriptions, // array flattened
    String editions, // array flattened
    String series, // array flattened
    String accessConditions,
    String termsOfUse

    // spine label not mapped yet in FOLIO
    ) {}
