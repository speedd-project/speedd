package org.speedd.traffic;

import org.speedd.BaseSpeeddTopology;
import org.speedd.EventJsonScheme;
import org.speedd.JoinBolt;
import org.speedd.cep.ProtonOutputConsumerBolt;
import org.speedd.data.Event;
import org.speedd.dm.TrafficDecisionMakerBolt;

import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.topology.base.BaseRichSpout;

import com.ibm.hrl.proton.ProtonTopologyBuilder;
import com.ibm.hrl.proton.metadata.parser.ParsingException;

public class TrafficManagementTopology extends BaseSpeeddTopology {
	private static final String CEP_INPUT = "cep-input";

	private static final String DECISION_WRITER = "decision-writer";
	
	public static final String DECISION_MAKER = "dm";
	
	public static final String ENRICHER = "enricher";

	@Override
	public StormTopology buildTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		BaseRichSpout trafficReaderSpout = createKafkaReaderSpout(
				brokerHosts,
				speeddConfig.inEventScheme,
				speeddConfig.topicInEvents,
				IN_EVENT_READER);

		BaseRichSpout adminSpout = createKafkaReaderSpout(
				brokerHosts,
				EventJsonScheme.class.getName(), 
				speeddConfig.topicAdmin,
				ADMIN_COMMAND_READER);
		
		builder.setSpout(IN_EVENT_READER, trafficReaderSpout);

		BaseRichSpout actionsSpout = createKafkaReaderSpout(
				brokerHosts,
				EventJsonScheme.class.getName(), 
				speeddConfig.topicActions,
				ACTION_READER);
		
		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>().withTopicSelector(
				new DefaultTopicSelector(speeddConfig.topicOutEvents))
				.withTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper());
		
		BaseBasicBolt enricherBolt = new TrafficEnricherBolt(speeddConfig.enricherPath);
		
		// @FIXME distribute output events according to the use-case specific
		// grouping strategy
		builder.setBolt(ENRICHER, enricherBolt).shuffleGrouping(IN_EVENT_READER);

		JoinBolt joinBolt = new JoinBolt();
		
		builder.setSpout(ACTION_READER, actionsSpout);
		
		builder.setBolt(CEP_INPUT, joinBolt).shuffleGrouping(ENRICHER).shuffleGrouping(ACTION_READER);

		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		try {
			
			protonTopologyBuilder.buildProtonTopology(builder, CEP_INPUT,
					protonOutputConsumerBolt, CEP_EVENT_CONSUMER,
					speeddConfig.epnPath);
		} catch (ParsingException e) {
			throw new RuntimeException("Building Proton topology failed, reason: ", e);
		}

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt).shuffleGrouping(
				CEP_EVENT_CONSUMER);

		builder.setSpout(ADMIN_COMMAND_READER, adminSpout)
				.setMaxTaskParallelism(1);

		IRichBolt dmBolt = new TrafficDecisionMakerBolt();
		
		// @FIXME distribute output events according to the use-case specific
		// grouping strategy
		builder.setBolt(DECISION_MAKER, dmBolt)
				.shuffleGrouping(CEP_EVENT_CONSUMER)
				.allGrouping(ADMIN_COMMAND_READER);

		builder.setBolt(DECISION_WRITER,
				new KafkaBolt<String, Event>().withTopicSelector(
						new DefaultTopicSelector(speeddConfig.topicActions))
						.withTupleToKafkaMapper(
								new FieldNameBasedTupleToKafkaMapper()))
				.shuffleGrouping(DECISION_MAKER);

		return builder.createTopology();
	}

}
