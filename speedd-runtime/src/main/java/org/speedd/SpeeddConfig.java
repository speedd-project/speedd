package org.speedd;

public class SpeeddConfig {
	public String epnPath;
	public String enricherPath;
	public String enricherClass;
	public String zkConnect;
	public String inEventScheme;
	public String dmClass;
	public String topicInEvents;
	public String topicOutEvents;
	public String topicActions;
	public String topicAdmin;
	public String topicActionsConfirmed;
	public int cepParallelismHint;
	public int inEventReaderParallelismHint;
	public int inEventReaderTaskNum;
	public int outEventWriterParallelismHint;
	public int outEventWriterTaskNum;
	public int cepConsumerParallelismHint;
	public int cepConsumerTaskNum;
	
	public SpeeddConfig() {
		cepParallelismHint = 1;
		inEventReaderParallelismHint = 1;
		inEventReaderTaskNum = 1;
		outEventWriterParallelismHint = 1;
		outEventWriterTaskNum = 1;
		cepConsumerParallelismHint = 1;
		cepConsumerTaskNum = 1;
	}
}
