package au.gov.nla.pickslip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PickslipViewerApplication {

  public static void main(String[] args) {
    SpringApplication.run(PickslipViewerApplication.class, args);
  }
}
