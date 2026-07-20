package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.UnicodeFontMap;
import au.gov.nla.pickslip.domain.FolioInstance;
import au.gov.nla.pickslip.domain.PickslipQueues;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openpdf.text.pdf.PdfReader;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@ExtendWith(MockitoExtension.class)
public class PdfResponderServiceTests {

  @Test
  void generatePreservesOneMergedPagePerPickslipWhenBarcodesAreMissing() throws Exception {
    PdfResponderService pdfResponderService = new PdfResponderService();
    FolioService folioService = Mockito.mock(FolioService.class);

    pdfResponderService.folioService = folioService;
    pdfResponderService.templateEngine = templateEngine();
    pdfResponderService.unicodeFontMap = unicodeFontMap();
    ReflectionTestUtils.invokeMethod(pdfResponderService, "init");

    var firstPickslip = pickslip("instance-1", null, "", "Call Number 1");
    var secondPickslip = pickslip("instance-2", "321654987", "123456789", "Call Number 2");

    Mockito.when(folioService.getFolioInstances(List.of("instance-1", "instance-2")))
        .thenReturn(
            Map.of(
                "instance-1", folioInstance("instance-1", "HRID-1"),
                "instance-2", folioInstance("instance-2", "HRID-2")));

    byte[] pdfBytes;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      pdfResponderService.generate(
          outputStream, List.of(firstPickslip, secondPickslip), "pickslips.pdf");
      pdfBytes = outputStream.toByteArray();
    }

    Assertions.assertTrue(pdfBytes.length > 0, "Expected generated PDF bytes");

    try (PdfReader reader = new PdfReader(pdfBytes)) {
      Assertions.assertEquals(2, reader.getNumberOfPages());
    }
  }

  private static SpringTemplateEngine templateEngine() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }

  private static UnicodeFontMap unicodeFontMap() {
    UnicodeFontMap unicodeFontMap = new UnicodeFontMap();
    unicodeFontMap.setFontRanges(List.of());
    return unicodeFontMap;
  }

  private static PickslipQueues.Pickslip pickslip(
      String instanceId, String itemBarcode, String patronBarcode, String callNumber) {
    return new PickslipQueues.Pickslip(
        false,
        false,
        new PickslipQueues.Pickslip.Request(
            "request-" + instanceId,
            new PickslipQueues.Pickslip.Request.Requester(
                "requester-" + instanceId, patronBarcode, "Jane", "Reader"),
            ZonedDateTime.of(2026, 7, 21, 10, 30, 0, 0, ZoneId.systemDefault()),
            "Please retrieve",
            "Reader Group",
            PickslipQueues.Pickslip.Request.Status.OPEN_NOT_YET_FILLED.getCode(),
            "1",
            List.of()),
        new PickslipQueues.Pickslip.Item(
            "item-" + instanceId,
            "Title for " + instanceId,
            "Primary Contributor",
            "All Contributors",
            "Description of pieces",
            itemBarcode,
            callNumber,
            "Chronology",
            "Enumeration",
            "MITCHELL WAREHOUSE [Mitchell]",
            "2026",
            "1"),
        new PickslipQueues.Pickslip.Instance(instanceId, "Instance title"));
  }

  private static FolioInstance folioInstance(String id, String hrid) {
    return new FolioInstance(
        id,
        hrid,
        "Canberra",
        "2026",
        "123 pages",
        "First edition",
        "Series title",
        null,
        "Access conditions",
        "Terms of use",
        "Spine label");
  }
}
