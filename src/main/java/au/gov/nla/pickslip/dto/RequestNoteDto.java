package au.gov.nla.pickslip.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestNoteDto {
  private String requestId;
  private String cancellationAdditionalInformation;
}
