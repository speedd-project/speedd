package org.speedd.dm;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.speedd.data.Event;
import org.speedd.data.EventFactory;
import org.speedd.data.impl.SpeeddEventFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

class onrampStruct {
	public Boolean active; // option to turn off ramp metering completely
	public Double maxFlow; // user-defined upper limit for ramp metering rate
	public Double minFlow; // user-defined lower limit for ramp metering rate
	public Double flow; // mainline flow estimate
	public long flowTimestamp; // timestamp of the latest mainline flow measurement
	public Double density; // mainline density estimate
	public long actionTimestamp; // timestamp of the latest mainline density estimate
	
	public onrampStruct() {
		// initialize data
		this.active = false;
		this.flow = Double.NaN;
		this.flowTimestamp = -1;
		this.density = Double.NaN;
		this.actionTimestamp = -1;
		this.maxFlow = 100.0;
		this.minFlow = .0;
	}
}

/**
 * DM traffic use case V1.1:
 * Implementation of distributed ramp metering strategy ALINEA
 * 
 * @author mschmitt, kofman
 *
 */
public class TrafficDecisionMakerBolt extends BaseRichBolt {
	private static final long serialVersionUID = 1L;
	
	private OutputCollector collector;
	private static final EventFactory eventFactory = SpeeddEventFactory.getInstance();
	Logger logger = LoggerFactory.getLogger(TrafficDecisionMakerBolt.class);
	
	Map<String, onrampStruct> onrampData = new HashMap<String, onrampStruct>();

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		Event event = (Event)input.getValueByField("message");
		
		// read event
		String eventName = event.getEventName();
		long timestamp = event.getTimestamp();
		Map<String, Object> attributes = event.getAttributes();
		// attribute "location" needs to be present, since partitioning is done according to it
		String location = (String) attributes.get("location");
		
		if (location != null)
		{
			onrampStruct data = onrampData.get(location); // read onramp data
			// create variables for onramp if not yet created
			if (data == null) data = new onrampStruct();
			
			if (eventName.equals("PredictedCongestion") || eventName.equals("Congestion"))
			{
				// turn on ramp metering
				data.active = true;
				// TODO: Use certainty attribute to activate adjacent ramps as well, if necessary (coordinated ramp metering)
			}
			else if (eventName.equals("ClearCongestion"))
			{
				// turn off ramp metering
				data.active = false;
				// reset data (except for the limits)
				data.flow = Double.NaN;
				data.flowTimestamp = -1;
				data.density = Double.NaN;
				data.actionTimestamp = -1;
			}
			else if (eventName.equals("setMeteringRateLimits"))
			{
				// set limits
				Double minFlow = (Double)attributes.get("lowerLimit");
				if (minFlow != null) 
					{
					if (minFlow >= 0) data.minFlow = minFlow;
					else data.minFlow = .0; // disable lower limit
					}
				Double maxFlow = (Double)attributes.get("upperLimit");
				if (maxFlow != null) 
					{
					if (maxFlow >= 0) data.maxFlow = maxFlow;
					else data.maxFlow = Double.POSITIVE_INFINITY; // disable upper limit
					}
			}
			else if (eventName.equals("OnRampFlow"))
			{
				Double inflow = (Double) attributes.get("average_flow"); //dummy;
				data.flowTimestamp = timestamp;
				data.flow = inflow; // TODO: filter data
			}
			else if (eventName.equals("2minsAverageDensityAndSpeedPerLocation"))
			{
				if (data.active && (data.flowTimestamp >= 0) && (data.flowTimestamp > data.actionTimestamp))
				{
					// need estimate of the onramp flow. Also estimate must be more recent than the last action.
					Double density = (Double)attributes.get("average_density");
					
					// create action event
					Map<String, Object> outAttrs = new HashMap<String, Object>();
					outAttrs.put("newMeteringRate", computeNewRate(data,density,location)); // compute action
					outAttrs.put("location", location);
					outAttrs.put("density", density); // may serve as a justification for the decision
					outAttrs.put("lane", "onramp"); // to distinguish from variable speed limits on the mainline?
					
					Event outEvent = eventFactory.createEvent("UpdateMeteringRateAction", timestamp, outAttrs);
					
					// Use sensor labels for partitioning by kafka
					collector.emit(new Values(location, outEvent));
					data.actionTimestamp = timestamp;
				}
			}
			// Events "AverageDensityAndSpeedPerLocation" are not used, we use the 2minsAverage... instead.
			
			onrampData.put(location, data); // saveback onramp data
		}
		else
		{
			logger.warn("location is null for tuple " + input);
			// Discard event and do nothing. Could throw an exception, report an error etc.
		}
				
	}

	private Double computeNewRate(onrampStruct data, double density, String location) {	
		double criticalDensity = get_criticalDensity(location); // Recognize cell ID:
		double KI = 3.6*70/criticalDensity; // Integral gain
		Double new_rate = data.flow + KI * (criticalDensity-density); // Update ramp metering rate: ALINEA
		return Math.min(data.maxFlow, Math.max(data.minFlow, new_rate)); // saturate/ anti-wind-up
	}
	
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("key", "message"));
	}
	
	// A set of private function that essentially just implement hardcoded lookup tables.
	// Those could be replaced by database queries or ...
	
	private int get_cellId(String location) {
		Map<String,Integer> cellId = new HashMap<String,Integer>();
		// Hardcoded for now.
		cellId.put("0024a4dc00003356", -1);
		cellId.put("0024a4dc00003354", 2);
		cellId.put("0024a4dc0000343c", -1);
		cellId.put("0024a4dc0000343b", 4);
		cellId.put("0024a4dc00003445", 5);
		cellId.put("0024a4dc00001b67", 6);
		cellId.put("0024a4dc00003357", -1);
		cellId.put("0024a4dc00000ddd", -1);
		cellId.put("0024a4dc00003355", 9);
		cellId.put("0024a4dc000021d1", -1);
		cellId.put("0024a4dc0000343f", 11);
		cellId.put("0024a4dc00001b5c", -1);
		cellId.put("0024a4dc000025eb", 13);
		cellId.put("0024a4dc000025ea", -1);
		cellId.put("0024a4dc00001c99", -1);
		cellId.put("0024a4dc000013c6", 16);
		cellId.put("0024a4dc00003444", -1);
		cellId.put("0024a4dc000025ec", 18);
		cellId.put("0024a4dc0000343e", -1);
		Integer id = cellId.get(location);
		if (id != null) return id;
		else return -1;
	}

	private double get_criticalDensity(String location) {
		// need to save onramp flow if it is does not come in matched pairs with the density.
		final double[] criticalDensities = {62, 64, 65, 47, 41, 60, 61, 57, 60, 62,
				59, 50, 54, 60, 0, 63, 65, 73, Double.POSITIVE_INFINITY}; // nineteen cells according to sensor locations
		return criticalDensities[get_cellId(location)-1];
		}
	

}
