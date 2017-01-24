package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * DM traffic use case V3.0
 * Implememntation of an event-driven freeway ramp metering algorithm as
 * documented in the respective chapters in D4.3 and D6.7.
 * 
 * @author mschmitt, using contributions from V1.0 and V2.0, partly by cramesh, akofman
 *
 */
public class TrafficDecisionMakerBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	Logger logger = LoggerFactory.getLogger(TrafficDecisionMakerBolt.class);
	
	private Map<Integer, FreewayMeteredOnramp> cell_map = new HashMap<Integer, FreewayMeteredOnramp>();
	
	/*
	 * SPEEDD specific constants
	 */
	
	// turn on/off debugging: creates log files of internal dm-state
	public static boolean DEBUG = false;
	
	// event names
	public static String CONGESTION = "Congestion";
	public static String CLEAR_CONGESTION = "ClearCongestion";
	public static String PREDICTED_CONGESTION = "PredictedCongestion"; // NOTE: Not defined on github.
	public static String MAINLINE_MEASUREMENT = "AverageDensityAndSpeedPerLocationOverInterval";
	public static String ONRAMP_MEASUREMENT = "AverageOnRampValuesOverInterval";
	public static String OFFRAMP_MEASUREMENT = "AverageOffRampValuesOverInterval";
	public static String PREDICTED_OVERFLOW = "PredictedRampOverflow";
	public static String CLEAR_RAMP_OVERFLOW = "ClearRampOverflow";
	public static String INCIDENT = "PossibleIncident";
	public static String METERING_LIMITS = "setMeteringRateLimits"; // NOTE: Not defined on github.
	public static String COORDINATE = "CoordinateRamps"; 
	public static String QUEUE_LENGTH = "AggregatedQueueLength";
	public static String SET_RATES = "SetTrafficLightPhases";
	
	// simulation/ execution constants
	public static double dt = 60./3600.; 									// NOTE: Change accordingly.
	public static double RMIN = 3. * 60.;									// NOTE: One car every 20s. Change accordingly.
	
	// conversion factors
	public static double RATE2GREEN_INTERVAL = dt * 2.;						// ASSUMPTION: One car every 2sec.
	public static double OCCU_2_DENS = 200.; 								// ASSUMPTION: attribute 'density' is sensor coverage ('occupancy', [0..1])
	public static double CARS_2_FLOW = 1. / TrafficDecisionMakerBolt.dt; 	// ASSUMPTION: attribute 'flow' is in [cars / observation_interval]
	

	/**
	 * MS: I do not know what this function does.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	/**
	 * Wrapper function for main routine, executed whenever an event arrives.
	 * @param input: contains the event
	 */
	@Override
	public void execute(Tuple input) {
		Event event = (Event)input.getValueByField("message");
		execute(event); // don't read 
	}
	
	/**
	 * Main routine, separated from execute(Tuple input) to facilitate testing
	 * @param event
	 */
	public Event[] execute(Event event) {	
		if (event == null) {
			logger.warn("Event is null");
			return null; // do nothing
		}
		
		// read event attributes
		Map<String, Object> attributes = event.getAttributes();
		String dmPartition = (String) attributes.get("dmPartition"); // attribute "dm_sensorId" needs to be present
		Integer sensorId;
		try {
			sensorId = Integer.parseInt((String) attributes.get("sensorId")); // attribute "sensorId" ...
		} catch (NumberFormatException e) {
			sensorId = null; // will make the event being ignored
		}
		if (sensorId == null) {
			try {
				sensorId = Integer.parseInt((String) attributes.get("junction_id")); // ... or "junction_id" needs to be present
			} catch (NumberFormatException e) {
				sensorId = null; // will make the event being ignored
			}
		}
		
		if ((dmPartition != null) && (sensorId != null))
		{
			int sensor_id = (int) sensorId;
			
			// check if sensor id is valid
			int k = GrenobleTopology.get_index(sensor_id);
			if (k <= 0) {
				return null; // invalid sensor ID
			}
			
			// check if object already present
			FreewayMeteredOnramp localRamp = cell_map.get((Integer) k);
			if (localRamp == null) {
				FreewayCell_IdTable new_id_table = GrenobleTopology.get_id_table(k);
				if (new_id_table.actu_id > 0) {
					// only if metered onramp
					Ctm new_ctm = GrenobleTopology.getCtm(k);
					double new_ql = GrenobleTopology.getQueue(k);
					localRamp= new FreewayMeteredOnramp(new_id_table, new_ctm, new_ql, TrafficDecisionMakerBolt.dt);
					cell_map.put((Integer) k, localRamp);
				}
			}

			// need second check to exclude cells without metered onramps
			if (localRamp != null) {
				// forward event for processing, receive new events
				Event[] out_events = localRamp.processEvent(event);
				
				if (out_events != null) {
				for (int ii=0; ii<out_events.length; ii++) {
						if (out_events[ii] != null) {
							// emit
			            	logger.info("Emitting event: " + out_events[ii]);
			            	collector.emit(new Values("1", out_events[ii]));	
						}
					}
					return out_events;
				} else {
					return null;
				}
				
			}
			
		} else {
			logger.warn("dmPartition and/or sensorId is null or invalid for tuple " + event);
			System.out.println(dmPartition);
			System.out.println(sensorId);
			System.out.println((String) attributes.get("sensorId"));
			// Discard event and do nothing. Could throw an exception, report an error etc.
		}
		
		return null;		
	}

	/**
	 * MS: I do not know what this function does.
	 */
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}
	
}
