package org.speedd;

import backtype.storm.generated.StormTopology;

public interface ISpeeddTopology {
	public void configure(SpeeddConfig configuration);
	
	public StormTopology buildTopology();
}
