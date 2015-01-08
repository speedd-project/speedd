Setting Up
==========
SPEEDD prototype consists of two modules:
- runtime
- user interface (or dashboard)

Before the first use you should build the modules as described below.

Building SPEEDD runtime
-----------------------
Project speedd-runtime contains the code for the runtime platform for SPEEDD.
The speedd-runtime module depends on a bunch of other projects (e.g. storm, kafka, etc.). Most of the dependencies are available on the internet and will be resolved by maven. There is one projects that is special - it requires your explicit action - as follows:

* storm-kafka-0.8-plus - clone this repository: [https://github.com/kofman-alex/storm-kafka-0.8-plus.git](https://github.com/kofman-alex/storm-kafka-0.8-plus.git).

After cloning, build the storm-kafka-plus by running 'mvn clean install' command from the storm-kafka-plus directory.

Then build the speedd-runtime module by performing the following:

1. cd speedd-runtime
2. mvn clean install assembly:assembly

Installing the Dashboard module
-------------------------------
For install perform the following steps:
1. open terminal
2. run: chmod +x installUI.sh
3. run: ./installUI.sh
4. Follow instructions in the terminal

**Note**: after pulling changes for UI modules have to be updated - see details in [dashboard's README](https://github.com/speedd-project/speedd/tree/master/speedd-ui).

Running demo scenarios
======================
In the following sections we assume that your current working directory is 'speedd-runtime'.

Traffic Management
------------------
1. cd ./traffic
2. sudo ./start-speedd-runtime
3. Run the dashboard by starting ../../speedd-ui/bin/run.sh
4. start sending input events by running ./playevents-traffic  

Credit Card Fraud Detection
---------------------------
1. cd ./ccf
2. sudo ./start-speedd-runtime
3. _TBD - start UI_
3. start sending input events by running ./playevents-fraud

Stopping SPEEDD prototype
-------------------------
1. Kill the topology by running 'sudo scripts/kill-speedd-runtime'
2. Stop the UI by killing the process (or Ctrl-C in the terminal shell where the UI process has been started)

_**Note: It is important to stop the prototype running current demo scenario before running another demo scenario (for example, if the traffic management demo is running, it is important to stop it before running the credit card fraud demo).**_

Seeing events and actions emitted by SPEEDD
-------------------------------------------
- **For input events**: run 'scripts/show-in-events'
- **For output events**: run 'scripts/show-out-events'
- **For actions**: run 'scripts/show-actions'
