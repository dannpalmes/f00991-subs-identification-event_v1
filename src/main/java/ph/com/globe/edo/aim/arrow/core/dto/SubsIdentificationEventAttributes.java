package ph.com.globe.edo.aim.arrow.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubsIdentificationEventAttributes {
    String msisdn;
    String imsi;
    String subsId;
    String brand;
    String fName;
    String lName;
    String lastCity;
    long balance;
}
