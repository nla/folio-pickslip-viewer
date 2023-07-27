package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.domain.FolioLocation;
import au.gov.nla.pickslip.domain.FolioPickslip;
import au.gov.nla.pickslip.domain.FolioRequest;
import au.gov.nla.pickslip.domain.FolioServicePoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@ExtendWith(MockitoExtension.class)
public class FolioServiceTests {

  @Spy FolioService folioService;

  private ResourceLoader resourceLoader = new DefaultResourceLoader();
  private ObjectMapper mapper = new ObjectMapper();

  private JsonNode loadJson(String filename) throws IOException {
    Resource r = resourceLoader.getResource("classpath:json/" + filename);
    return mapper.readTree(r.getFile());
  }

  @Test
  void getFolioServicePoints() throws IOException, InterruptedException {

    // Mockito.when..
    Mockito.doReturn(loadJson("folioServicePoints.json"))
        .when(folioService)
        .folioApiGetServicePoints();

    List<FolioServicePoint> servicePoints = folioService.getFolioServicePoints();

    // expected..
    var expected =
        Arrays.asList(
            new FolioServicePoint(
                "f2b6aa63-008f-4c02-a6ac-e761a6b51ee1",
                "MITCHELL-SP",
                "Mitchell",
                "Mitchell Warehouse"),
            new FolioServicePoint(
                "325aea8b-f5f2-4160-88ca-63d803856a70",
                "MRR-SP",
                "Main Reading Room",
                "Main Reading Room"));

    Assertions.assertEquals(expected, servicePoints);
  }

  @Test
  void getFolioLocations() throws IOException, InterruptedException {

    // Mockito.when..
    Mockito.doReturn(loadJson("folioLocations.json"))
        .when(folioService)
        .folioApiGetFolioLocations();

    List<FolioLocation> locations = folioService.getFolioLocations();

    // expected..
    var expected =
        Arrays.asList(
            new FolioLocation(
                "18749e74-091a-4d99-8c05-e2d96e6e92c8",
                "AUSLQ",
                "AUSLQ [Lakeside]",
                "b552250a-81ad-4c61-b768-d4188b75f161"),
            new FolioLocation(
                "e08095e7-4f53-465a-9d74-c5376d076f24",
                "RUNPEF",
                "RUNPEF - Aust [LG1]",
                "cf222fba-49cc-4b41-90f7-ed220f4a8228"),
            new FolioLocation(
                "343d85b5-654b-4331-aae3-67c434d57316",
                "MITCHELL",
                "MITCHELL WAREHOUSE [Mitchell]",
                "f2b6aa63-008f-4c02-a6ac-e761a6b51ee1"),
            new FolioLocation(
                "64dfa4ff-c806-4f10-b871-e6a2af0a7e8e",
                "WHOUSE",
                "WHOUSE [Hume]",
                "d3782c73-042e-4c36-98d1-efeb0fc3ac3e"));

    Assertions.assertEquals(expected, locations);
  }

  @Test
  void getPickslipsForServicePoint() throws IOException, InterruptedException {

    // Mockito.when..
    Mockito.doReturn(loadJson("pickslipsForServicePointMitchell.json"))
        .when(folioService)
        .folioApiGetPickslipsForServicePoint(
            ArgumentMatchers.eq("343d85b5-654b-4331-aae3-67c434d57316"), ArgumentMatchers.anyInt());

    List<FolioPickslip> pickslips =
        folioService.getPickslipsForServicePoint("343d85b5-654b-4331-aae3-67c434d57316");

    // expected..
    var expected =
        Arrays.asList(
            new FolioPickslip(
                "a9d7fff3-4211-49ee-855a-1ca59dd27fe5",
                new FolioPickslip.Item(
                    "The miscellaneous works of Thomas Arnold.",
                    "Arnold, Thomas, 1795-1842",
                    "Arnold, Thomas, 1795-1842",
                    "990002052522",
                    null,
                    "320.4 ARN",
                    null,
                    null,
                    "MITCHELL WAREHOUSE [Mitchell]",
                    "",
                    "1"),
                new FolioPickslip.Requester("Michael", "Howard", "a")),
            new FolioPickslip(
                "c62b7535-2d98-43c1-ba5c-906ceb41fcd7",
                new FolioPickslip.Item(
                    "Print : a manual for librarians and students describing in detail the "
                        + " history, methods, and applications of printing and paper making.",
                    "Mann, George",
                    "Mann, George",
                    "990001588920",
                    null,
                    "655 MAN",
                    null,
                    null,
                    "MITCHELL WAREHOUSE [Mitchell]",
                    "",
                    "1"),
                new FolioPickslip.Requester("Alex (SPL)", "Barr", "21708000392674")));

    Assertions.assertEquals(expected, pickslips);
  }

  @Test
  void getRequests() throws IOException, InterruptedException {

    // Mockito.when..
    Mockito.doReturn(loadJson("folioRequests.json")).when(folioService).folioApiGetRequests();

    List<FolioRequest> requests = folioService.getFolioRequests();

    ZonedDateTime requestDate =
        ZonedDateTime.parse(
            "2023-03-26T13:00:00.000+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    ZonedDateTime localRequestDate = requestDate.withZoneSameInstant(ZoneId.systemDefault());

    // expected..
    var expected =
        Arrays.asList(
            new FolioRequest(
                "e688d594-379b-456d-845c-e68c14e613d8",
                localRequestDate,
                "",
                "dd3223ca-a641-521e-9b51-2090d34bebd6",
                "f4e192ba-a995-5f2c-8d7d-c13ec36d258f",
                "5e73e86d-e493-5465-9bbe-a8d62025f475",
                "Open - Not yet filled",
                "1",
                new FolioRequest.Instance(
                    "Land without justice : an autobiography of his youth / Milovan Djilas ; with"
                        + " an introduction and notes by William Jovanovich."),
                new FolioRequest.Item(
                    "990001588920",
                    "655 MAN",
                    new FolioRequest.Item.Location("MITCHELL WAREHOUSE [Mitchell]", "MITCHELL")),
                new FolioRequest.Requester("21708000392647", "SPL"),
                Arrays.asList("mrr-sp", "parked")));

    Assertions.assertEquals(expected, requests);
  }
}
