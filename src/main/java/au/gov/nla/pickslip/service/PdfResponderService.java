package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.UnicodeFontMap;
import au.gov.nla.pickslip.domain.PickslipQueues;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.*;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextUserAgent;

@Service
public class PdfResponderService {

  @Autowired FolioService folioService;

  @Autowired SpringTemplateEngine templateEngine;

  @Autowired UnicodeFontMap unicodeFontMap;

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private ITextRenderer renderer;

  @PostConstruct
  private void init() throws IOException {
    this.renderer = new ITextRenderer();
    ResourceLoaderUserAgent resourceLoaderUA =
        new ResourceLoaderUserAgent(renderer.getOutputDevice());
    resourceLoaderUA.setSharedContext(renderer.getSharedContext());
    renderer.getSharedContext().setUserAgentCallback(resourceLoaderUA);

    var resolver = renderer.getFontResolver();
    resolver.addFont("pdf/fonts/NotoSans-Regular.ttf", BaseFont.IDENTITY_H, false);

    resolver.addFont("pdf/fonts/NotoSansKR-VF.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansSC-VF.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansJP-VF.ttf", BaseFont.IDENTITY_H, false);

    resolver.addFont("pdf/fonts/NotoSansLao-Regular.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansKhmer-Regular.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansThai-Regular.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansBalinese-Regular.ttf", BaseFont.IDENTITY_H, false);
    resolver.addFont("pdf/fonts/NotoSansMyanmar-Regular.ttf", BaseFont.IDENTITY_H, false);
  }

  public static String generateCode128BarcodeImage(String barcodeText, int width, int height)
      throws Exception {

    if (barcodeText == null || barcodeText.isBlank()) {
      return null;
    }

    Code128Writer barcodeWriter = new Code128Writer();

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 4);

    BitMatrix bitMatrix =
        barcodeWriter.encode(barcodeText, BarcodeFormat.CODE_128, width, height, hints);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "png", bos);
    return Base64.getEncoder().encodeToString(bos.toByteArray());
  }

  public synchronized void generate(OutputStream os, List<PickslipQueues.Pickslip> pickslipList, String filename) {

    Context ctx = new Context(LocaleContextHolder.getLocale());

    try (Document out = new Document(PageSize.A4)) {

      var instanceIdList = pickslipList.stream().map(p -> p.instance().id()).toList();
      var instances = folioService.getFolioInstances(instanceIdList);

      log.debug("Retrieved instances {}", instances.keySet());

      PdfCopy writer = new PdfCopy(out, os);
      out.open();

      for (PickslipQueues.Pickslip pickslip : pickslipList) {

        String itemBarcode, patronBarcode;
        try {
          itemBarcode = generateCode128BarcodeImage(pickslip.item().barcode(), 150, 50);
          patronBarcode =
              generateCode128BarcodeImage(pickslip.request().requester().barcode(), 80, 25);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        ctx.setVariable(
            "itemBarcode64",
            itemBarcode != null ? "data:image/png;base64," + itemBarcode : null);
        ctx.setVariable(
            "patronBarcode64",
            patronBarcode != null ? "data:image/png;base64," + patronBarcode : null);

        ctx.setVariable("instance", instances.get(pickslip.instance().id()));
        ctx.setVariable("pickslip", pickslip);
        ctx.setVariable("printFilename", filename);
        ctx.setVariable("unicodeFontMap", unicodeFontMap);

        String htmlContent = this.templateEngine.process("pdf/print_pdf", ctx);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(baos);

        byte[] data = baos.toByteArray(); // in the order of 3-5k per page fwiw
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        PdfReader reader = new PdfReader(bais);
        PdfImportedPage importedPage = writer.getImportedPage(reader, 1);

        log.debug("adding page... {} ", importedPage.getPageNumber());
        log.debug("buffer length... {} ", data.length);

        PdfAction action = new PdfAction(PdfAction.PRINTDIALOG);
        writer.setOpenAction(action);

        writer.addPage(importedPage);
        writer.freeReader(reader);
        reader.close();
      }
      writer.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static class ResourceLoaderUserAgent extends ITextUserAgent {
    public ResourceLoaderUserAgent(ITextOutputDevice outputDevice) {
      super(outputDevice);
    }

    protected InputStream resolveAndOpenStream(String uri) {
      return super.resolveAndOpenStream(uri);
    }
  }
}
