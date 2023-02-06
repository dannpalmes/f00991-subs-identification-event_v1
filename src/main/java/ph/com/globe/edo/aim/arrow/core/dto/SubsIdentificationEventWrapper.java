package ph.com.globe.edo.aim.arrow.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubsIdentificationEventWrapper {
    @JsonProperty("event_id")
    String eventId;

    @JsonProperty("offer_event_id")
    String offerEventId;

    @JsonProperty("subs_id")
    String subsId;

    String msisdn;
    String cd;
    String msg;
    String state;

    @JsonProperty("campaign_group")
    String campaignGroup;

    @JsonProperty("attributes")
    SubsIdentificationEventAttributes subsIdentificationEventAttributes;

    String platform;

    @JsonProperty("processed_dttm")
    String processedDttm;

}
