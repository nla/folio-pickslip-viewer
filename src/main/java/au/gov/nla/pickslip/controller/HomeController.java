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
import java.util.List;

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

    pdfResponderService.generate(sos, id);
    response.flushBuffer();
  }

  @GetMapping({"/location/{stack}"})
  public String stack(@PathVariable(value = "stack") String stackCode,
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
  public String index(Model model,
                      @RequestParam(required = false) String[] showOnly) {

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
      for (String code: stackCodes) {
        var stack = stackLocations.getStackForCode(code);
        if (stack != null) {
          stackList.add(stack);
        }
      }
    }
    return (stackList != null && stackList.size() > 0)
            ? stackList
            : stackLocations.getStacks();
  }

}
