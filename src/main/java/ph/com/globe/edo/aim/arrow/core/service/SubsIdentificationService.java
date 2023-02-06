package ph.com.globe.edo.aim.arrow.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ph.com.globe.edo.aim.arrow.aerospike.service.AerospikeService;
import ph.com.globe.edo.aim.arrow.component.SimRegistrationComponent;
import ph.com.globe.edo.aim.arrow.core.avro.SubsIdentification;
import ph.com.globe.edo.aim.arrow.kafka.consumer.config.KafkaConsumerProperty;
import ph.com.globe.edo.aim.arrow.kafka.consumer.handler.AvroObjectKafkaConsumerMessageProcessor;
import ph.com.globe.edo.aim.arrow.kafka.consumer.handler.MessageType;
import ph.com.globe.edo.aim.arrow.kafka.producer.client.KafkaProducerClient;
import ph.com.globe.edo.aim.steps.component.utility.StepsStringUtil;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static ph.com.globe.edo.aim.arrow.core.constants.SubsIdentificationConstants.*;
import static ph.com.globe.edo.aim.arrow.kafka.consumer.handler.MessageType.JSON;

@Slf4j
@Dependent
public class SubsIdentificationService extends AvroObjectKafkaConsumerMessageProcessor<SubsIdentification, Map<String, Object>> {

	private static final String[] SPC_BINS = { MSISDN_BIN,IMSI_BIN };

	@ConfigProperty(name = "is.process-event", defaultValue = "true")
	boolean isProcessEvent;

	@ConfigProperty(name = "is.subs-identification.enabled", defaultValue = "true")
	boolean isSendToSimRegistration;

	@ConfigProperty(name = "is.test-mode.enabled", defaultValue = "true")
	boolean isTestModeEnabled;

	@ConfigProperty(name = "is.send-to-kafka", defaultValue = "true")
	boolean isSendToKafka;

	@ConfigProperty(name = "test.data")
	List<String> testData;

	@ConfigProperty(name = "consumer.threads")
	int threads;

	@ConfigProperty(name = "consumer.kafka.topic")
	String consumerTopic;

	@ConfigProperty(name = "producer.kafka.topic")
	String producerTopic;

	@ConfigProperty(name = "subsIdentification.columns", defaultValue = "msisdn,imsi")
	String[] columns;

	@ConfigProperty(name = "aerospike.namespace")
	String namespace;

	@ConfigProperty(name = "aerospike.spc.set", defaultValue = "uup_cache")
	String spcSet;

	@ConfigProperty(name = "aerospike.rc.set", defaultValue = "f0091_subsIdentification_RC")
	String rcSet;

//	@ConfigProperty(name = "aerospike.barring-retry.set", defaultValue = "f0091_barring_retry")
//	String barringRetrySet;

	@ConfigProperty(name = "aerospike.ttl", defaultValue = "86400")
	int ttl;

	@ConfigProperty(name = "recurrence.ttl", defaultValue = "432000")
	int rcTtl;

	@ConfigProperty(name = "druid.cd", defaultValue = "00000")
	String cd;

	@ConfigProperty(name = "druid.msg", defaultValue = "Success: Registered")
	String msg;

	@ConfigProperty(name = "druid.state", defaultValue = "REGISTERED")
	String state;

	@ConfigProperty(name = "campaign.group", defaultValue = "SI")
	String campaignGroup;

	@ConfigProperty(name = "campaign.name", defaultValue = "c00091")
	String campaignName;

	@ConfigProperty(name = "subs-identification.channel-id", defaultValue = "simReg")
	String subsIdentificationChannelId;

	@ConfigProperty(name = "subs-identification.mode", defaultValue = "1")
	Integer subsIdentificationMode;

	@ConfigProperty(name = "subs-identification.channel", defaultValue = "ARR")
	String subsIdentificationChannel;

//	@ConfigProperty(name = "subs-registration.sim-type", defaultValue = "sim")
//	String simRegistrationSimType;
	
//	@ConfigProperty(name = "camel.success-code", defaultValue = "200")
//	String camelSuccessCode;

	// Previous error codes 204,401,404,500,600,601 | 401 404 deleted due to
	// unauthorized error codes
//	@ConfigProperty(name = "camel.error-codes", defaultValue = "204,500,600,601")
//	List<String> camelErrorCodes;

	@ConfigProperty(name = "subs-identification.success-code", defaultValue = "0")
	String subsIdentificationSuccessCode;

	@ConfigProperty(name = "subs-identification.date", defaultValue = "2022-12-27 00:00:00")
	String subsIdentificationDate;

	@ConfigProperty(name = "subs-identification.format", defaultValue = "yyyy-MM-dd HH:mm:ss")
	String dateTimeFormat;

//	@ConfigProperty(name = "request.type", defaultValue = "barring")
//	String requestType;

	@Inject
	KafkaConsumerProperty kafkaConsumerProperty;

	@Inject
	KafkaProducerClient kafkaProducerClient;

	@Inject
	AerospikeService aerospikeService;

	@Inject
	SimRegistrationComponent simRegistrationComponent;

	@ConfigProperty(name = "kafka.producer.delimeter", defaultValue = "\t")
	String producerDelimeter;

	@Inject
	SpcAerospikeCluster cluster;

	ObjectMapper mapper = new ObjectMapper();

	@Override
	public String messageDelimiter() {
		return EMPTY;
	}

	@Override
	public MessageType messageType() {
		return JSON;
	}

	@Override
	public void processEvent(SubsIdentification data) {
		try {
			if (isProcessEvent) {

				if (isTestModeEnabled) {

					if (testData.contains(data.getMsisdn().toString())) {
						process(data);
					}

				} else {
					process(data);
				}

			}
		} catch (Exception e) {
			log.error(ERROR, "processEvent()", e.getMessage(), Arrays.asList(e.getStackTrace()));
		}
	}

	@Override
	public Map<String, Object> properties() {
		return kafkaConsumerProperty.getConfigs();
	}

	@Override
	public int threads() {
		return threads;
	}

	@Override
	public String topic() {
		return consumerTopic;
	}


	@Override
	public SubsIdentification getSourceObject(Map<String, Object> values) {
		SubsIdentification subsIdentification = new SubsIdentification();
		try {
			if (values.size() >= subsIdentification.getSchema().getFields().size()) {
				subsIdentification.setMsisdn(getValue(values.get(columns[0]).toString()));
				subsIdentification.setImsi(getValue(values.get(columns[1]).toString()));
				subsIdentification.setSubsId(getValue(values.get(columns[2]).toString()));
				subsIdentification.setBrand(getValue(values.get(columns[3]).toString()));
				subsIdentification.setFName(getValue(values.get(columns[4]).toString()));
				subsIdentification.setLastCity(getValue(values.get(columns[5]).toString()));
				subsIdentification.setBalance(StepsStringUtil.parseLong(StepsStringUtil.parseLong(columns[6])));
			} else {
				log.error("Subscriber Identification Event Input Invalid");
			}

		} catch (Exception e) {
			log.error(ERROR, "getSourceObject()", e.getMessage(), Arrays.asList(e.getStackTrace()));
		}
		return subsIdentification;
	}

	private String getValue(Object data) {
		return data != null ? String.valueOf(data) : EMPTY;
	}
	private void process(SubsIdentification data) {

		try {
			log.info("data: {}", data);

			String msisdn = data.getMsisdn().toString();
			String imsi = data.getImsi().toString();
			String spcClusterName = cluster.clusterName();

			if (!aerospikeService.exists(spcClusterName, namespace, rcSet, msisdn)) {
				Map<String, Object> spcData = retrieveSpc(msisdn);

				String msisdnRetrieved = spcData.get(MSISDN_BIN) != null ? spcData.get(MSISDN_BIN).toString() : EMPTY;

				if (!StringUtils.isAllBlank(msisdnRetrieved )) {
						SubsIdentification siCache = new SubsIdentification();
						siCache = buildSubsIdentificationWrapper(data);
						sendToKafka(writeAsString(siCache));
						log.info("msisdn: {} - registered.", msisdn);
				} else {
					log.info("BIN is Empty for the MSISDN: {}", msisdn);
				}
			} else {
				log.info("Data not first transaction {}", msisdn);
			}
		} catch (Exception e) {
			log.error(ERROR, "retrieveSpc()", e.getMessage(), Arrays.asList(e.getStackTrace()));
		}

	}
	private Map<String, Object> retrieveSpc(String msisdn) {

		Map<String, Object> data = new HashMap<>();

		try {

			String spcClusterName = cluster.clusterName();
			String formattedMsisdn = msisdn.substring(2);

			data = aerospikeService.retrieve(spcClusterName, namespace, spcSet, formattedMsisdn, SPC_BINS);

			if (data != null) {
				log.info("msisdn: {}, set: {}, data: {}", formattedMsisdn, spcSet, data);
			} else {
				log.info("msisdn: {} - no data retrieved from {}.", formattedMsisdn, spcSet);
			}

		} catch (Exception e) {
			log.error(ERROR, "retrieveSpc()", e.getMessage(), Arrays.asList(e.getStackTrace()));
		}

		return data;

	}

	private SubsIdentification buildSubsIdentificationWrapper(SubsIdentification subsIdentification) {

		SubsIdentification subsIdentificationCache = new SubsIdentification();

		try {
			subsIdentificationCache.setDetectionId(detectionId());
			subsIdentificationCache.setMsisdn(subsIdentification.getMsisdn().toString());
			subsIdentificationCache.setImsi(subsIdentification.getImsi().toString());
			subsIdentificationCache.setBrand(subsIdentification.getBrand().toString());
			subsIdentificationCache.setFName(subsIdentification.getFName().toString());
			subsIdentificationCache.setLName(subsIdentification.getLName().toString());
			subsIdentificationCache.setLastCity(subsIdentification.getLastCity().toString());
			subsIdentificationCache.setBalance(subsIdentification.getBalance());
			subsIdentificationCache.setState(PROCESSED);
		} catch (Exception e) {
			log.error("Error in Subscriber Detection: {}", e.getMessage());
		}
		return subsIdentificationCache;
	}
	public String detectionId(){
		return RandomStringUtils.randomAlphanumeric(4);
	}
	private String writeAsString(SubsIdentification data) {
		StringBuilder sb = new StringBuilder();
		if (data.getMsisdn() != null)
			sb.append(data.getMsisdn());
		sb.append(producerDelimeter);
		if (data.getImsi() != null)
			sb.append(data.getImsi());
		sb.append(producerDelimeter);
		if (data.getSubsId() != null)
			sb.append(data.getSubsId());
		sb.append(producerDelimeter);
		if (data.getBrand() != null)
			sb.append(data.getBrand());
		sb.append(producerDelimeter);
		if (data.getFName() != null)
			sb.append(data.getFName());
		sb.append(producerDelimeter);
		if (data.getLName() != null)
			sb.append(data.getLName());
		sb.append(producerDelimeter);
		if (data.getLastCity() != null)
			sb.append(data.getLastCity());
		sb.append(producerDelimeter);
		if (data.getBalance() != null)
			sb.append(data.getBalance());
		sb.append(producerDelimeter);
		return sb.toString();
	}
	private void sendToKafka(String data) {

		if (isSendToKafka) {
			try {

				kafkaProducerClient.sendMessage(producerTopic, data);
				log.info("Topic Produced: {} - {}", producerTopic, data);

			} catch (Exception e) {
				log.error(ERROR, "sendToKafka():FAILED", e.getMessage(), Arrays.asList(e.getStackTrace()));
			}

		}
	}

}
