package au.gov.nla.pickslip;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("stacklocations")
public class StackLocations {

  private List<Location> stacks;

  public record Location(String code, String label) {}
  ;

  public void setStacks(List<Location> stacks) {
    this.stacks = stacks;
  }

  public Location getStackForCode(String code) {
    return stacks.stream().filter(s -> code.equalsIgnoreCase(s.code())).findFirst().orElse(null);
  }

  public List<Location> getStacks() {
    return stacks;
  }
}
