Project speedd-runtime contains the code for the runtime platform for SPEEDD. It depends on a couple of other projects:


1. storm-kafka-0.8-plus - clone this repository: [https://github.com/kofman-alex/storm-kafka-0.8-plus.git](https://github.com/kofman-alex/storm-kafka-0.8-plus.git)
2. ProtonOnStorm - implementation of Proton CEP engine on storm platform. Download [proton-on-storm.zip](https://github.com/kofman-alex/speedd/blob/master/proton-on-storm.zip "proton-on-storm.zip") archive and extract it into your local maven repository under com/ibm directory, so that the resulting directory layout will be as follows:
<pre>.m2\repository\com\ibm\hrl
├───eep
│   └───EEP
│       └───0.1.0
├───json
│   └───JSON
│       └───0.1.0
└───proton
    ├───Proton
    │   └───0.1.0
    └───ProtonOnStorm
        └───1.0-SNAPSHOT
 </pre>