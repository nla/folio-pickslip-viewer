package au.gov.nla.pickslip.service;

import au.gov.nla.pickslip.StackLocations;
import au.gov.nla.pickslip.domain.FolioLocation;
import au.gov.nla.pickslip.domain.FolioPickslip;
import au.gov.nla.pickslip.domain.FolioRequest;
import au.gov.nla.pickslip.domain.FolioServicePoint;
import au.gov.nla.pickslip.domain.PickslipQueues;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class RequestEditService {

  private final FolioService folioService;

  public FolioRequest getRequestById(final String requestId) throws IOException {
    return folioService.folioApiGetRequestById(requestId);
  }
}
