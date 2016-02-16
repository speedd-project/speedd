package org.speedd.fraud;

import org.speedd.BaseSpeeddTopology;
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

		BaseRichSpout inEventReaderSpout = createKafkaReaderSpout(
				brokerHosts,
				speeddConfig.inEventScheme,
				speeddConfig.topicInEvents,
				IN_EVENT_READER);

		builder.setSpout(IN_EVENT_READER, inEventReaderSpout, 2).setNumTasks(2);

		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>().withTopicSelector(
				new DefaultTopicSelector(speeddConfig.topicOutEvents))
				.withTupleToKafkaMapper(
						new FieldNameBasedTupleToKafkaMapper());
		
		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		try {
			protonTopologyBuilder.buildProtonTopology(
					builder, 
					IN_EVENT_READER,
					protonOutputConsumerBolt,
					CEP_EVENT_CONSUMER,
					speeddConfig.epnPath);
		} catch (ParsingException e) {
			throw new RuntimeException("Building Proton topology failed, reason: ", e);
		}

		builder.setBolt(OUT_EVENT_WRITER, eventWriterBolt, 4).shuffleGrouping(CEP_EVENT_CONSUMER).setNumTasks(8);

		return builder.createTopology();	
	}

}
