package org.speedd.traffic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.Fields;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class TrafficEnricherBolt extends BaseRichBolt {

	public static final Logger LOG = LoggerFactory.getLogger(TrafficEnricherBolt.class);
	 
	private OutputCollector _collector;	
	private HashMap<String,EnrichmentInformation> mappingTable = new HashMap<String,EnrichmentInformation>(); 
	
	private class EnrichmentInformation implements Serializable
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String dm_partition;
		private String lane;
		private int queueLength;
		private String location;
		
		private EnrichmentInformation(String dm_partition, String lane,
				int queueLength, String location) {			
			this.dm_partition = dm_partition;
			this.lane = lane;
			this.queueLength = queueLength;
			this.location = location;
		}

		
	}
	
	public TrafficEnricherBolt(String enricherPath)
	{		
		prepareMapping(enricherPath);
	}
	
	

	@Override
	public void execute(Tuple input) {
		//read the attributes from the tuple
		Map<String,Object> attributes = (Map<String,Object>)input.getValueByField(Fields.FIELD_ATTRIBUTES);
		Map<String,Object> updatedAttributes = new HashMap<String,Object>(attributes);
		
		String aimsunId = (String)attributes.get(TrafficAimsunReadingCsv2Event.ATTR_DETECTOR_ID);
		//if this location is irrelevant
		EnrichmentInformation information = mappingTable.get(aimsunId);				
		
		if (information != null)
		{						
			updatedAttributes.put(TrafficAimsunReadingCsv2Event.ATTR_LANE, information.lane);
			updatedAttributes.put(TrafficAimsunReadingCsv2Event.ATTR_DM_PARTITION,information.dm_partition);
			updatedAttributes.put(TrafficAimsunReadingCsv2Event.ATTR_LOCATION,information.location);
			updatedAttributes.put(TrafficAimsunReadingCsv2Event.ATTR_QUEUE_LENGTH,information.queueLength);
		}
		 updatedAttributes = Collections.unmodifiableMap(updatedAttributes);
		 List<Object> tuple = new ArrayList<Object>();
		 tuple.add(input.getValueByField(Fields.FIELD_PROTON_EVENT_NAME));
		 tuple.add(input.getValueByField(Fields.FIELD_TIMESTAMP));
		 tuple.add(updatedAttributes);
		
		_collector.emit(tuple);

	}

	@Override
	public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
		_collector = collector;							
		

	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		backtype.storm.tuple.Fields fields = new backtype.storm.tuple.Fields(Fields.FIELD_PROTON_EVENT_NAME, Fields.FIELD_TIMESTAMP, Fields.FIELD_ATTRIBUTES);
		declarer.declare(fields);

	}
	
	private  void prepareMapping(String translatorTables) {
		BufferedReader reader = null;
		
		//open the input file and retrieve the first and last timestamps,calculate the difference
		try {
			//reading first line
			reader = new BufferedReader(new FileReader(translatorTables));
			
			String line = reader.readLine();
			
			while (line != null){				
				byte[] record = line.getBytes(Charset.forName("UTF-8"));
				String[] tuple = new String(record, Charset.forName("UTF-8")).split(",");				
				
				String aimsunId = tuple[1];
				String lane = tuple[3];
				String dm_partition = tuple[2];
				String location = tuple[0];
				Integer queueLength = Integer.valueOf(tuple[8]);
				
				if (!aimsunId.equals("????")){
										
					EnrichmentInformation enrichmentInfo= new EnrichmentInformation(dm_partition, lane, queueLength, location);
					mappingTable.put(aimsunId, enrichmentInfo);
				}
				line = reader.readLine();
				
			}
		}
		catch(Exception e){
			 LOG.warn("Failer to prepare mapping tables for enrichment", e);
		}finally{
			if (reader!= null)
				try {
					reader.close();
				} catch (IOException e) {
					
				}
		}
		
	}

	

}
