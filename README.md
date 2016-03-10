Setting Up
==========
SPEEDD prototype consists of two modules:
- runtime
- user interface (or dashboard)

Before the first use you should build the modules as described below.

Building SPEEDD runtime
-----------------------
Project speedd-runtime contains the code for the runtime platform for SPEEDD.
The speedd-runtime module depends on a bunch of other projects (e.g. storm, kafka, etc.). The dependencies are available on the internet and will be resolved by maven.

Build the speedd-runtime module by performing the following:

1. cd speedd-runtime
2. mvn clean install assembly:assembly

Installing and running dashboard app
------------------------------------
Follow instructions for respective use cases:
- [Traffic Management](https://github.com/speedd-project/speedd/tree/master/speedd_ui_tm_new)
- [Credit Card Fraud Management](https://github.com/speedd-project/speedd/tree/master/speedd_ui_bf_new)

Running demo scenarios
======================
In the following sections we assume that your current working directory is 'speedd-runtime'.

Traffic Management
------------------
1. cd scripts/traffic
2. sudo ./start-speedd-runtime
3. Open a new terminal and start the dashboard app following the [instructions](https://github.com/speedd-project/speedd/tree/master/speedd_ui_tm_new)
4. start sending input events by running ./playevents-traffic  

Credit Card Fraud Detection
---------------------------
1. cd scripts/ccf
2. sudo ./start-speedd-runtime
3. Open a new terminal and start the dashboard app following the [instructions](https://github.com/speedd-project/speedd/tree/master/speedd_ui_bf_new)
4. start sending input events by running ./playevents-fraud

Stopping SPEEDD prototype
-------------------------
1. Kill the topology by running 'sudo scripts/kill-speedd-runtime'
2. Stop the UI by killing the process (or Ctrl-C in the terminal shell where the UI process has been started)

_**Note: It is important to stop the prototype running current demo scenario before running another demo scenario (for example, if the traffic management demo is running, it is important to stop it before running the credit card fraud demo).**_

Seeing events and actions emitted by SPEEDD
-------------------------------------------
For example, for traffic management:
- **For input events**: run 'scripts/traffic/show-in-events'
- **For output events**: run 'scripts/traffic/show-out-events'
- **For actions**: run 'scripts/traffic/show-actions'
