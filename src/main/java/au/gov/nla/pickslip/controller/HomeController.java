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
  // shelf order when
  // printing in bulk. e.g.:
  // "NL 582.1309945 G148" -> 582.1309945
  // "N 2019-1537" -> 2019.1537
  // "ORAL TRC 3800" -> 0
  // "Ephemera (Trade catalogues)" -> 0
  // "mfm X 650/reels 5,704-5,790." -> 0
  private float getDeweyish(String callNumber) {

    Pattern pattern = Pattern.compile("^\\D*(\\d+)\\D?(\\d+)?");
    Matcher matcher = pattern.matcher(callNumber);

    float result = 0;
    try {
      if (matcher.find()) {
        result =
            Float.parseFloat(
                ((matcher.groupCount() > 0) ? matcher.group(1) : "0")
                    + "."
                    + ((matcher.groupCount() > 1) ? matcher.group(2) : "0"));
      }
    } catch (NumberFormatException nfe) {
      result = 0;
    }

    return result;
  }

  @GetMapping("/bulkprint/{stack}")
  public void bulkExport(
      @PathVariable String stack, @RequestParam String upToId, HttpServletResponse response)
      throws IOException {

    ServletOutputStream sos = response.getOutputStream();
    response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=print.pdf");

    List<PickslipQueues.Pickslip> sortedPickslips = null;

    var uptoPickslip = this.pickslipQueues.getPickslipByRequestId(upToId);
    var stackPickslips = this.pickslipQueues.getPickslipsForStack(stack);
    if (uptoPickslip == null || stackPickslips == null) {
      return;
    }

    // sort by something that looks like a dewey number (or running number), then alphabetically
    var sorted =
        stackPickslips.stream()
            .filter(
                p ->
                    (p.request().requestDate().isAfter(uptoPickslip.request().requestDate()))
                        || p.request().requestDate().isEqual(uptoPickslip.request().requestDate()))
            .sorted(
                (Comparator.comparingDouble(
                        (PickslipQueues.Pickslip p) -> getDeweyish(p.item().callNumber()))
                    .thenComparing(p -> p.item().callNumber())))
            .toList();

    /*
    // sort: status; then by request date.  Head of list is most recent; "In transit" status at end.
    for (var pickslips : servicePointPickslips.values()) {
      pickslips.sort(
              Comparator.comparing((PickslipQueues.Pickslip a) -> a.request.status)
                      .thenComparing(a -> a.request.requestDate)
                      .reversed());
    }



    Comparator<PickslipQueues.Pickslip> comparator = new Comparator<PickslipQueues.Pickslip>() {

      //  	NL 582.1309945 G148 	-> 582.1309945
      //   	N 2019-1537 -> 2019.1537
      // ORAL TRC 3800 -> 0
      // Ephemera (Trade catalogues) -> 0
      // mfm X 650/reels 5,704-5,790. -> 0
      private float extractDewy(String s) {
        return 0;
      }
      @Override
      public int compare(PickslipQueues.Pickslip p1, PickslipQueues.Pickslip p2) {
        float c1 = extractDewy(p1.item().callNumber());
        float c2 = extractDewy(p2.item().callNumber());
        return Float.compare(c1, c2);
      }
    };


    // pickslip.request().requestDate()

    var sorted = stackPickslips.stream().filter(p -> p.request().requestDate().isBefore(uptoPickslip.request().requestDate())).sorted(
      comparator
    ).toList();

    */

    pdfResponderService.generate(sos, sorted);
    response.flushBuffer();
  }

  @GetMapping({"/location/{stack}"})
  public String stack(
      @PathVariable(value = "stack") String stackCode,
      @RequestParam(required = false) String[] showOnly,
      Model model) {

    model.addAttribute("showOnly", showOnly);
    model.addAttribute("stacks", filterStackLocations(showOnly));
    model.addAttribute("stack", stackLocations.getStackForCode(stackCode));
    model.addAttribute("queue", pickslipQueues.getPickslipsForStack(stackCode));
    model.addAttribute("visitors", pickslipQueues.getVisitorsForStack(stackCode));

    return "stack";
  }

  @GetMapping({"", "/", "/home"})
  public String index(@RequestParam(required = false) String[] showOnly, Model model) {

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

  // label printing POC - move elsewhere..
  @GetMapping(path = "printLabel")
  public String printLabel(@RequestParam("data") String data, Model model) throws Exception {
    model.addAttribute("data", data);
    return "label";
  }
}
