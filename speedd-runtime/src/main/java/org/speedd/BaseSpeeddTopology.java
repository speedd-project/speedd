package org.speedd;

import org.slf4j.Logger;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.base.BaseRichSpout;

public abstract class BaseSpeeddTopology implements ISpeeddTopology {
	protected SpeeddConfig speeddConfig;
	protected BrokerHosts brokerHosts;
	
	protected Logger logger;

	public static final String ADMIN_COMMAND_READER = "admin-command-reader";

	public static final String IN_EVENT_READER = "in-event-reader";
	
	public static final String ACTION_READER = "action-reader";

	public static final String OUT_EVENT_WRITER = "out-event-writer";

	public static final String CEP_EVENT_CONSUMER = "cep-event-consumer";

	@Override
	public void configure(SpeeddConfig configuration) {
		this.speeddConfig = configuration;
		brokerHosts = new ZkHosts(speeddConfig.zkConnect);
	}
	
	protected BaseRichSpout createKafkaReaderSpout(
			BrokerHosts brokerHosts, 
			String schemeClassName,
			String topic, 
			String spoutId
	) {
		SpoutConfig kafkaConfig = new SpoutConfig(
				brokerHosts,
				topic, 
				"",
				spoutId);
		try {
			Class<? extends Scheme> clazz = (Class<Scheme>) this.getClass().getClassLoader().loadClass(schemeClassName);
			kafkaConfig.scheme = new SchemeAsMultiScheme(clazz.newInstance());
		} catch (Exception e) {
			throw new RuntimeException("Creating a scheme instance failed", e);
		}

		return new KafkaSpout(kafkaConfig);
	}

}
