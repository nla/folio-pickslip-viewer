package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.StackLocations;
import au.gov.nla.pickslip.domain.*;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class ScheduledRequestRetrieverService {

  // ISO 8601 Duration i.e. PT10S
  @Value("${schedule.retriever.interval}")
  String runInterval;

  @Value("${schedule.retriever.enabled}")
  Boolean enabled;

  @Autowired StackLocations stackLocations;

  @Autowired FolioService folioService;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private LocalDateTime lastStarted, lastCompleted, lastFailed;

  @PostConstruct
  private void init() {

    this.log.info(
        String.format(
            "Scheduled FOLIO request retriever running every %s..", Duration.parse(runInterval)));
  }

  public LocalDateTime getLastStarted() {
    return lastStarted;
  }

  public LocalDateTime getLastCompleted() {
    return lastCompleted;
  }

  public LocalDateTime getLastFailed() {
    return lastFailed;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  private final PickslipQueues pickslipQueues = new PickslipQueues();

  @Bean
  public PickslipQueues getPickslipQueues() {
    synchronized (this.pickslipQueues) {
      return this.pickslipQueues;
    }
  }

  @Scheduled(fixedDelayString = "${schedule.retriever.interval}")
  public void fetch() throws IOException {

    try {
      this.lastStarted = LocalDateTime.now();

      log.debug("Scheduled run..");

      if (!isEnabled()) {
        log.info("Scheduled retriever not enabled - quitting.");
        return;
      }

      List<FolioLocation> locations = folioService.getFolioLocations();
      List<FolioServicePoint> servicePoints = folioService.getFolioServicePoints();
      Map<FolioServicePoint, List<FolioPickslip>> pickslipsForServicePoints =
          folioService.getPickslipsForServicePoints(servicePoints);
      List<FolioRequest> notFilled = folioService.getFolioRequests();

      synchronized (this.pickslipQueues) {
        pickslipQueues.update(
            stackLocations, notFilled, servicePoints, locations, pickslipsForServicePoints);
      }

      this.lastCompleted = LocalDateTime.now();

      log.debug(".. Scheduled run complete.");

    } catch (RuntimeException re) {
      log.error("Scheduled run failed (rethrowing): " + re.getMessage());
      this.lastFailed = LocalDateTime.now();
      throw re;
    }
  }
}
