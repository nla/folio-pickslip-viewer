package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.domain.PickslipQueues;
import com.lowagie.text.pdf.BaseFont;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

  @Autowired SpringTemplateEngine templateEngine;

  @Autowired PickslipQueues pickslipQueues;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private ITextRenderer renderer;

  @PostConstruct
  private void init() throws IOException {
    this.renderer = new ITextRenderer();
    ResourceLoaderUserAgent resourceLoaderUA =
        new ResourceLoaderUserAgent(renderer.getOutputDevice());
    resourceLoaderUA.setSharedContext(renderer.getSharedContext());
    renderer.getSharedContext().setUserAgentCallback(resourceLoaderUA);
    renderer
        .getFontResolver()
        .addFont("pdf/fonts/LibreBarcode128Text-Regular.ttf", BaseFont.IDENTITY_H, true);
  }

  public synchronized void generate(OutputStream os, String id) {

    PickslipQueues.Pickslip pickslip = pickslipQueues.getPickslipByRequestId(id);

    Context ctx = new Context(LocaleContextHolder.getLocale());

    ctx.setVariable("pickslip", pickslip);
    String htmlContent = this.templateEngine.process("pdf/print_pdf", ctx);

    renderer.setDocumentFromString(htmlContent);
    renderer.layout();
    renderer.createPDF(os);
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
