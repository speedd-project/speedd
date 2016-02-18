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
import com.ibm.hrl.proton.utilities.containers.Pair;

public class CCFTopology extends BaseSpeeddTopology {

	@Override
	public StormTopology buildTopology() {
		TopologyBuilder builder = new TopologyBuilder();

		BaseRichSpout inEventReaderSpout = createKafkaReaderSpout(
				brokerHosts,
				speeddConfig.inEventScheme, 
				speeddConfig.topicInEvents,
				IN_EVENT_READER);

		builder.setSpout(
				IN_EVENT_READER,
				inEventReaderSpout,
				speeddConfig.inEventReaderParallelismHint
		).setNumTasks(speeddConfig.inEventReaderTaskNum);

		KafkaBolt<String, Event> eventWriterBolt = new KafkaBolt<String, Event>()
				.withTopicSelector(	new DefaultTopicSelector(speeddConfig.topicOutEvents))
				.withTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper());

		ProtonOutputConsumerBolt protonOutputConsumerBolt = new ProtonOutputConsumerBolt();

		ProtonTopologyBuilder protonTopologyBuilder = new ProtonTopologyBuilder();

		try {
			Pair<String, String> boltRoutingInformation = protonTopologyBuilder
					.buildProtonTopology(
							builder,
							IN_EVENT_READER,
							speeddConfig.epnPath,
							speeddConfig.cepParallelismHint);
			builder.setBolt(
					CEP_EVENT_CONSUMER,
					protonOutputConsumerBolt,
					speeddConfig.cepConsumerParallelismHint)
					.shuffleGrouping(boltRoutingInformation.getFirstValue(), boltRoutingInformation.getSecondValue())
					.setNumTasks(speeddConfig.cepConsumerTaskNum);
		} catch (ParsingException e) {
			throw new RuntimeException(
					"Building Proton topology failed, reason: ", e);
		}

		builder.setBolt(
				OUT_EVENT_WRITER, 
				eventWriterBolt, 
				speeddConfig.outEventWriterParallelismHint)
				.shuffleGrouping(CEP_EVENT_CONSUMER)
				.setNumTasks(speeddConfig.outEventWriterTaskNum);

		return builder.createTopology();
	}

}
