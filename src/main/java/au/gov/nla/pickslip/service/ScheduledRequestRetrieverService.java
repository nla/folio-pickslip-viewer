package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.StackLocations;
import au.gov.nla.pickslip.domain.*;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledRequestRetrieverService {

  @Value("${schedule.retriever.cron}")
  String runCron;

  @Value("${schedule.retriever.enabled}")
  Boolean enabled;

  @Autowired StackLocations stackLocations;

  @Autowired FolioService folioService;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  private LocalDateTime lastStarted, lastCompleted, lastFailed;

  @PostConstruct
  private void init() {
    this.log.info(String.format("Scheduled FOLIO request retriever cron spec: %s..", runCron));
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

  // event listener and scheduler will be on different threads
  @Scheduled(cron = "${schedule.retriever.cron}")
  @EventListener(ContextRefreshedEvent.class)
  public synchronized void fetch() throws IOException {

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
