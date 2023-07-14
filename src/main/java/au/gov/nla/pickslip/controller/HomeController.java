package au.gov.nla.pickslip.controller;

import au.gov.nla.pickslip.StackLocations;
import au.gov.nla.pickslip.domain.PickslipQueues;
import au.gov.nla.pickslip.service.FolioService;
import au.gov.nla.pickslip.service.PdfResponderService;
import au.gov.nla.pickslip.service.ScheduledRequestRetrieverService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

  @Autowired PickslipQueues pickslipQueues;

  @Autowired FolioService folioService;

  @Autowired PdfResponderService pdfResponderService;

  @Autowired ScheduledRequestRetrieverService scheduledRequestRetrieverService;

  @Autowired StackLocations stackLocations;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @GetMapping("/export/{id}")
  public void export(@PathVariable String id, HttpServletResponse response) throws IOException {

    ServletOutputStream sos = response.getOutputStream();
    response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=print.pdf");

    PickslipQueues.Pickslip pickslip = pickslipQueues.getPickslipByRequestId(id);

    pdfResponderService.generate(sos, Collections.singletonList(pickslip));
    response.flushBuffer();
  }

  // Extract numeric components relevant to sorting from call number.  Just enough to be helpful wrt
  // shelf order when printing in bulk. e.g.:
  // "NL 582.1309945 G148" -> 582.1309945
  // "N 2019-1537" -> 2019.1537
  // "ORAL TRC 3800" -> 0
  // "Ephemera (Trade catalogues)" -> 0
  // "mfm X 650/reels 5,704-5,790." -> 0
  double getDeweyish(String callNumber) {

    Pattern pattern = Pattern.compile("^\\D*(\\d+)\\D?(\\d+)?");
    Matcher matcher = pattern.matcher(callNumber);

    double result = 0;
    try {
      if (matcher.find()) {
        result =
            Double.parseDouble(
                ((matcher.groupCount() > 0) ? matcher.group(1) : "0")
                    + "."
                    + ((matcher.groupCount() > 1) ? matcher.group(2) : "0"));
      }
    } catch (NumberFormatException nfe) {
      result = 0;
    }

    return result;
  }

  /*
   * Bulk print slips in "call slip order".  If visitors is set, the visitors to a stack location
   * are printed. Otherwise, the requests in a stack locatino are printed, and visitors are skipped.
   * Slips are ordered by callslip number when they look like they're based on a Dewey number.
   */
  @GetMapping("/bulkprint/{stack}")
  public void bulkExport(
      @PathVariable String stack,
      @RequestParam String upToId,
      @RequestParam(required = false) boolean visitors,
      HttpServletResponse response)
      throws IOException {

    ServletOutputStream sos = response.getOutputStream();
    response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=print.pdf");

    var uptoPickslip = this.pickslipQueues.getPickslipByRequestId(upToId);
    var stackPickslips =
        visitors
            ? this.pickslipQueues.getVisitorsForStack(stack)
            : this.pickslipQueues.getPickslipsForStack(stack);
    if (uptoPickslip == null || stackPickslips == null) {
      return;
    }

    // filter out visitors unless we're printing visitors, and anything that's not
    // open-not-yet-filled. Sort by something that looks like a dewey number (or running number),
    // then alphabetically.
    var sorted =
        stackPickslips.stream()
            .filter(
                p ->
                    (p.request().requestDate().isAfter(uptoPickslip.request().requestDate()))
                        || p.request().requestDate().isEqual(uptoPickslip.request().requestDate()))
            .filter(
                p ->
                    p.visiting() == visitors
                        && PickslipQueues.Pickslip.Request.Status.OPEN_NOT_YET_FILLED
                            .getCode()
                            .equalsIgnoreCase(p.request().status()))
            .sorted(
                (Comparator.comparingDouble(
                        (PickslipQueues.Pickslip p) -> getDeweyish(p.item().callNumber()))
                    .thenComparing(p -> p.item().callNumber())))
            .toList();

    pdfResponderService.generate(sos, sorted);
    response.flushBuffer();
  }

  @GetMapping({"/location/{stack}"})
  public String stack(
      @PathVariable(value = "stack") String stackCode,
      @RequestParam(required = false) String[] showOnly,
      Model model) {

    model.addAttribute("lastSuccess", scheduledRequestRetrieverService.getLastCompleted());
    model.addAttribute("showOnly", showOnly);
    model.addAttribute("stacks", filterStackLocations(showOnly));
    model.addAttribute("stack", stackLocations.getStackForCode(stackCode));
    model.addAttribute("queue", pickslipQueues.getPickslipsForStack(stackCode));
    model.addAttribute("visitors", pickslipQueues.getVisitorsForStack(stackCode));

    return "stack";
  }

  @GetMapping({"", "/", "/home"})
  public String index(@RequestParam(required = false) String[] showOnly, Model model) {

    model.addAttribute("lastSuccess", scheduledRequestRetrieverService.getLastCompleted());
    model.addAttribute("showOnly", showOnly);
    model.addAttribute("stacks", filterStackLocations(showOnly));
    model.addAttribute("queues", pickslipQueues);

    return "index";
  }

  private List<StackLocations.Location> filterStackLocations(String[] stackCodes) {

    List<StackLocations.Location> stackList = null;

    // use specified in given order if present
    if (stackCodes != null) {
      stackList = new ArrayList<>();
      for (String code : stackCodes) {
        var stack = stackLocations.getStackForCode(code);
        if (stack != null) {
          stackList.add(stack);
        }
      }
    }
    return (stackList != null && stackList.size() > 0) ? stackList : stackLocations.getStacks();
  }
}
