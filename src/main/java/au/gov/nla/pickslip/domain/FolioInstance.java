package au.gov.nla.pickslip.domain;

import java.util.List;

// Extra for printing  - /inventory/instances/{instanceId}
public record FolioInstance(
    String id,
    String hrid,
    String publicationPlace, // array flattened
    String publicationDateOfPublication, // array flattened
    String physicalDescriptions, // array flattened
    String editions, // array flattened
    String series, // array flattened
    List<String> variantTitles,
    String accessConditions,
    String termsOfUse,
    String spineLabel) {}
