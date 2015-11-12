package org.speedd.perf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaPinger {

	public static class PingCallback implements Callback {
		public AtomicBoolean called = new AtomicBoolean(false);
		
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if(exception != null){
				System.err.println("Error: " + exception.getMessage());
			}
			else {
				System.out.println("Record metadata: " + printRecordMetadata(metadata));
			}
			called.set(true);
		}
		
	}

	private static String printRecordMetadata(RecordMetadata recordMetadata){
		return String.format("topic: %s, partition: %d, offset: %d", recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset());
	}
	
	public static void main(String[] args) {
		Options options = new Options();

		options.addOption("b", "brokers", true, "Comma-separated list of brokers (host1:port1,...)");

		String bootstrapServers = "localhost:9092";
		
		try {
			CommandLine cmd = new BasicParser().parse(options, args);
			if(cmd.hasOption('b')){
				bootstrapServers = cmd.getOptionValue('b');
			}
		}
		catch(ParseException e){
			System.out.println(e.getMessage());
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("kafka-ping [options]",
					options);
			System.exit(1);
		}
		
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("bootstrap.servers", bootstrapServers);
		config.put("request.required.acks", 1);
		config.put("producer.type", "sync");
		config.put("serializer.class", StringSerializer.class);
		config.put("value.serializer", StringSerializer.class);
		config.put("key.serializer", StringSerializer.class);
		
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(config);
		
		PingCallback cb = new PingCallback();
		
		producer.send(new ProducerRecord<String, String>("test", "ping"), cb);
		
		while(!cb.called.get()){
			try {
			Thread.sleep(1000);
			} catch(InterruptedException e){
				
			}
		}

		System.out.println("Completed.");
	}
	

}
