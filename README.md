Project speedd-runtime contains the code for the runtime platform for SPEEDD.
To build it and run the runtime do as follows:

1. cd speedd-runtime
2. mvn clean install assembly:assembly
3. ./start-speedd-topology

The speedd-runtime module depends on a bunch of other projects (e.g. storm, kafka, etc.). Most of the dependencies are available on the internet and will be resolved by maven. There is one projects that is special - it requires your explicit action - as follows:

* storm-kafka-0.8-plus - clone this repository: [https://github.com/kofman-alex/storm-kafka-0.8-plus.git](https://github.com/kofman-alex/storm-kafka-0.8-plus.git). After cloning, build the storm-kafka-plus by running 'mvn clean install' command from the storm-kafka-plus directory.
