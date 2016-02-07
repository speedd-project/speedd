package org.speedd.fraud;

import org.speedd.BaseSpeeddTopology;
import org.speedd.EventJsonScheme;
import org.speedd.cep.ProtonOutputConsumerBolt;
import org.speedd.data.Event;

import storm.kafka.bolt.KafkaBolt;
import storm.kafka.bolt.mapper.FieldNameBasedTupleToKafkaMapper;
import storm.kafka.bolt.selector.DefaultTopicSelector;
import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichSpout;

import com.ibm.hrl.proton.ProtonTopologyBuilder;
import com.ibm.hrl.proton.metadata.parser.ParsingException;

public class CCFTopology extends BaseSpeeddTopology {

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
				.withTupleToKafkaMapper(
						new FieldNameBasedTupleToKafkaMapper());
		
		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		try {
			
			protonTopologyBuilder.buildProtonTopology(builder, IN_EVENT_READER,
					protonOutputConsumerBolt, CEP_EVENT_CONSUMER,
					speeddConfig.epnPath);
		} catch (ParsingException e) {
			throw new RuntimeException("Building Proton topology failed, reason: ", e);
		}

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt).shuffleGrouping(
				CEP_EVENT_CONSUMER);

		return builder.createTopology();	
	}

}
