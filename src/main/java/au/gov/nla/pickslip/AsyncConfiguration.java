package au.gov.nla.pickslip;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

  @Value("${folio.async.concurrency}")
  private int concurrency;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Override
  public Executor getAsyncExecutor() {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(this.concurrency);
    executor.setMaxPoolSize(this.concurrency);

    executor.initialize();
    return executor;
  }
}
