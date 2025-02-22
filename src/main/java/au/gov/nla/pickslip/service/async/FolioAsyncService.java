package au.gov.nla.pickslip.service.async;

import au.gov.nla.folio.api.FOLIOAlternativeTitleTypesAPI;
import au.gov.nla.folio.api.FOLIOInstanceNoteTypesAPI;
import au.gov.nla.folio.api.FOLIOInventoryAPI;
import au.gov.nla.folio.api.credentials.FOLIOAPICredentials;
import au.gov.nla.folio.util.FOLIOAPIUtils;
import au.gov.nla.pickslip.config.FolioConfiguration;
import au.gov.nla.pickslip.domain.FolioInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FolioAsyncService {

  @Value("${folio.note-type.access-conditions}")
  private String folioAccessConditionsNoteType;

  @Value("${folio.note-type.terms-of-use}")
  private String folioTermsOfUseNoteType;

  @Value("${folio.note-type.spine-label}")
  private String folioSpineLabelNoteType;

  @Value("${folio.alt-title-type.variant-title}")
  private String folioVariantTitleAltTitleType;

  @Autowired
  FolioConfiguration folioConfiguration;

  String folioAccessConditionsUuid;
  String folioTermsOfUseNoteUuid;
  String folioSpineLabelNoteTypeUuid;
  String folioVariantTitleAltTitleTypeUuid;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private ObjectMapper mapper = new ObjectMapper();

  private FOLIOAPICredentials folioOkapiCredentials;

  @PostConstruct
  public void init() throws IOException {

    folioOkapiCredentials = FOLIOAPIUtils.toFOLIOAPICredentials(Map.of("FOLIO_TENANT", folioConfiguration.getTenant(),
        "FOLIO_PASSWORD", folioConfiguration.getPassword(), "FOLIO_OKAPI_URL",
        folioConfiguration.getOkapiUrl(), "FOLIO_USERNAME", folioConfiguration.getUsername()));

    FOLIOInstanceNoteTypesAPI folioInstanceNoteTypesAPI =
        new FOLIOInstanceNoteTypesAPI(folioOkapiCredentials);

    this.folioAccessConditionsUuid =
        folioInstanceNoteTypesAPI.resolveNoteType(folioAccessConditionsNoteType);
    this.folioTermsOfUseNoteUuid =
        folioInstanceNoteTypesAPI.resolveNoteType(folioTermsOfUseNoteType);
    this.folioSpineLabelNoteTypeUuid =
        folioInstanceNoteTypesAPI.resolveNoteType(folioSpineLabelNoteType);

    FOLIOAlternativeTitleTypesAPI folioAlternativeTitleTypesAPI =
        new FOLIOAlternativeTitleTypesAPI(folioOkapiCredentials);

    this.folioVariantTitleAltTitleTypeUuid =
        folioAlternativeTitleTypesAPI.resolveAlternativeTitleType(folioVariantTitleAltTitleType);

    if (folioAccessConditionsUuid == null || folioAccessConditionsUuid.isBlank()) {
      throw new IllegalStateException(
          "Can't initialize: can't resolve instance note type - access conditions");
    }
    if (folioTermsOfUseNoteUuid == null || folioTermsOfUseNoteUuid.isBlank()) {
      throw new IllegalStateException(
          "Can't initialize: can't resolve instance note types - terms of use");
    }
    if (folioSpineLabelNoteTypeUuid == null || folioSpineLabelNoteTypeUuid.isBlank()) {
      throw new IllegalStateException(
          "Can't initialize: can't resolve instance note types - spine label");
    }
    if (folioVariantTitleAltTitleTypeUuid == null || folioVariantTitleAltTitleTypeUuid.isBlank()) {
      throw new IllegalStateException(
          "Can't initialize: can't resolve instance alternative title types - variant title");
    }
  }

  // ptrArray points to an array of objects or strings, ptrValue is the json name from which to
  // extract the value, from the object, or null if the array contains only strings.  Values are
  // joined by commas and returned.
  private String flatten(JsonNode node, String ptrArray, String ptrValue) {
    ArrayList<String> result = new ArrayList<>();
    JsonNode arrayNode = node.at(ptrArray);
    if (arrayNode != null && arrayNode.isArray()) {

      for (JsonNode jsonNode : arrayNode) {
        String value = ptrValue == null ? jsonNode.textValue() : jsonNode.at(ptrValue).textValue();
        if (value != null && !value.isEmpty()) {
          result.add(value);
        }
      }
    }
    return result.isEmpty() ? null : String.join(", ", result);
  }

  private String extractNote(JsonNode node, String noteId) {
    JsonNode arrayNode = node.at("/notes");
    if (arrayNode != null && arrayNode.isArray()) {
      for (JsonNode jsonNode : arrayNode) {
        if (noteId.equals(jsonNode.at("/instanceNoteTypeId").textValue())) {
          return jsonNode.at("/note").textValue();
        }
      }
    }
    return null;
  }

  private List<String> extractAltTitles(JsonNode node, String alternativeTitleId) {

    ArrayList<String> altTitles = new ArrayList<>();

    JsonNode arrayNode = node.at("/alternativeTitles");
    if (arrayNode != null && arrayNode.isArray()) {
      for (JsonNode jsonNode : arrayNode) {
        if (alternativeTitleId.equals(jsonNode.at("/alternativeTitleTypeId").textValue())) {
          String s = jsonNode.at("/alternativeTitle").textValue();
          if (s != null) {
            altTitles.add(
                Normalizer.normalize(s, Normalizer.Form.NFC)); // renderer don't do decomposed..
          }
        }
      }
    }
    return altTitles;
  }

  @Async
  public CompletableFuture<FolioInstance> getFolioInstance(String instanceId)
      throws JsonProcessingException {
    String instance = new FOLIOInventoryAPI(folioOkapiCredentials).getInstance(instanceId);
    JsonNode n = mapper.readTree(instance);

    FolioInstance folioInstance =
        new FolioInstance(
            n.at("/id").asText(null),
            n.at("/hrid").asText(null),
            flatten(n, "/publication", "/place"),
            flatten(n, "/publication", "/dateOfPublication"),
            flatten(n, "/physicalDescriptions", null),
            flatten(n, "/editions", null),
            flatten(n, "/series", "/value"),
            extractAltTitles(n, this.folioVariantTitleAltTitleTypeUuid),
            extractNote(n, this.folioAccessConditionsUuid),
            extractNote(n, this.folioTermsOfUseNoteUuid),
            extractNote(n, this.folioSpineLabelNoteTypeUuid));
    return CompletableFuture.completedFuture(folioInstance);
  }
}
