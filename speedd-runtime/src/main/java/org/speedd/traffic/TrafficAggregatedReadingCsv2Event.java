package org.speedd.traffic;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

import org.speedd.EventParser;
import org.speedd.ParsingError;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;

public class TrafficAggregatedReadingCsv2Event implements EventParser, Constants {
	private static final String ATTR_LOCATION = "location";
	private static final String ATTR_LANE = "lane";
	private static final String ATTR_SPEED_HISTOGRAM = "speed_histogram";
	private static final String ATTR_OCCUPANCY = "occupancy";
	private static final String ATTR_VEHICLES = "vehicles";
	private static final String ATTR_LENGTH_HISTOGRAM = "length_histogram";
	private static final String ATTR_TIMESTAMP = "timestamp";
	private static final String ATTR_AVG_SPEED = "average_speed";
	private static final String ATTR_MEDIAN_SPEED = "median_speed";

	
	private static final int ATTR_DATE_INDEX = 0;
	private static final int ATTR_TIME_INDEX = 1;
	private static final int ATTR_LOCATION_INDEX = 2;
	private static final int ATTR_LANE_INDEX = 3;
	private static final int ATTR_OCCUPANCY_INDEX = 4;
	private static final int ATTR_VEHICLES_INDEX = 5;
	private static final int ATTR_MEDIAN_SPEED_INDEX = 6;
	private static final int ATTR_AVG_SPEED_INDEX = 7;
	private static final int ATTR_SPEED_INDEX = 8;
	private static final int ATTR_LENGTH_INDEX = 28;
	
	private static final int SPEED_HISTOGRAM_BINCOUNT = 20;
	private static final int LENGTH_HISTOGRAM_BINCOUNT = 100;
	
	//total expected number of fields in a csv line. Assuming here that the length histogram is the ending part of csv
	private static final int NUM_FIELDS = ATTR_LENGTH_INDEX + SPEED_HISTOGRAM_BINCOUNT + LENGTH_HISTOGRAM_BINCOUNT;
	
	private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss"); 
	
	private EventFactory eventFactory;

	public TrafficAggregatedReadingCsv2Event(EventFactory eventFactory) {
		this.eventFactory = eventFactory;
	}

	public Event fromBytes(byte[] bytes) throws ParsingError {
		String name = TRAFFIC_SENSOR_READING_AGGREGATED;

		try {
			
			String[] tuple = new String(bytes).split(",");
			
			int tupleLength = tuple.length;
			
			//extend to expected length padding the rest with nulls
			if(tupleLength < NUM_FIELDS){
				String[] padded = Arrays.copyOf(tuple, NUM_FIELDS);
				
				Arrays.fill(padded, tupleLength, NUM_FIELDS-1, "0");
				
				tuple = padded;
			}

			String dateTimeStr = String.format("%s,%s", tuple[ATTR_DATE_INDEX], tuple[ATTR_TIME_INDEX]);

			long timestamp;

			timestamp = dateTimeFormat.parse(dateTimeStr).getTime();

			HashMap<String, Object> attrMap = new HashMap<String, Object>();
			
			attrMap.put(ATTR_TIMESTAMP, Long.valueOf(timestamp));
			attrMap.put(ATTR_LOCATION, tuple[ATTR_LOCATION_INDEX]);
			attrMap.put(ATTR_LANE, tuple[ATTR_LANE_INDEX]);
			attrMap.put(ATTR_OCCUPANCY, Double.parseDouble(tuple[ATTR_OCCUPANCY_INDEX]));
			attrMap.put(ATTR_VEHICLES, Long.parseLong(tuple[ATTR_VEHICLES_INDEX]));
			attrMap.put(ATTR_MEDIAN_SPEED, getNumericValue(tuple[ATTR_MEDIAN_SPEED_INDEX]));
			attrMap.put(ATTR_AVG_SPEED, getNumericValue(tuple[ATTR_AVG_SPEED_INDEX]));
			attrMap.put(ATTR_SPEED_HISTOGRAM, buildHistogram(tuple, ATTR_SPEED_INDEX, SPEED_HISTOGRAM_BINCOUNT));
			attrMap.put(ATTR_LENGTH_HISTOGRAM, buildHistogram(tuple, ATTR_LENGTH_INDEX, LENGTH_HISTOGRAM_BINCOUNT));

			return eventFactory.createEvent(name, timestamp, attrMap);
		} catch (Exception e) {
			throw new ParsingError(
					"Error parsing CSV for aggregated traffic reading", e);
		}
	}

	private Long[] buildHistogram(String[] tuple, int startIndex, int nBins) {
		Long[] histogram = new Long[nBins];
		
		for(int i=startIndex,binIndex=0; binIndex<nBins; ++i,++binIndex){
			histogram[binIndex] = Long.parseLong(tuple[i]);
		}
		
		return histogram;
	}
	
	private Double getNumericValue(String strVal){
		try {
			return Double.parseDouble(strVal);
		}
		catch (NumberFormatException e){
			//if cannot parse - the value is not available, - return null
			return null;
		}
	}

}
