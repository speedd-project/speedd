[SPEEDD](www.speedd-project.eu) project Machine Learning module.

Copyright (c) [Complex Event Recognition Group](cer.iit.demokritos.gr). NCSR Demokritos, Institute of Informatics and Telecommunications, Software and Knowledge Engineering Laboratory

## Licence
SPEEDD Machine Learning module comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; See the [GNU Lesser General Public License v3 for more details](http://www.gnu.org/licenses/lgpl-3.0.html).

## Instructions to build from source

### Requirements
In order to build SPEEDD Machine Learning Module from source, you need to have Java SE Development Kit (e.g., OpenJDK) version 7 or higher and [SBT](http://www.scala-sbt.org) (v0.13.x) installed in your system. 

All library dependencies are defined inside the `build.sbt` file. The module requires the following projects to be locally build and published:

**1.** Clone and publish locally the auxlib project:

```bash
$ git clone -b v0.1 --depth 1 https://github.com/anskarl/auxlib.git
$ cd auxlib
$ sbt ++2.11.7 publishLocal
```

**2.** Clone and publish locally the Optimus project (further instructions can be found [here](https://github.com/vagm/Optimus)).
```bash
$ git clone -v v1.2.1 --depth 1 https://github.com/vagm/Optimus.git
$ cd Optimus
$ sbt publishLocal
```

**3.** Clone and publish locally the LoMRF project (further instructions can be found [here](https://github.com/anskarl/LoMRF)).

```bash
$ git clone -v v0.4.2 --depth 1 https://github.com/anskarl/LoMRF.git
$ cd LoMRF
$ sbt publishLocal
```
Once you have successfully build and published `auxlib`, `Optimus` and `LoMRF` projects, you can either build a standalone version or a "cluster" version. 

### Build a standalone version
To build the SPEEDD Machine Learning Module, give the following command:
```bash
$ cd path/to/speedd-ml
$ sbt dist
```

After a successful compilation, the SPEEDD Machine Learning Module is located inside the ./target/universal/speedd-ml-<version>.zip file. You can extract this file and add the path/to/speedd-ml-<version>/bin in your PATH, in order to execute the SPEEDD Machine Learning Module scripts from terminal.

### Build a cluster version

To build the SPEEDD Machine Learning Module for Spark cluster (e.g., Apache Mesos), give the following command:
```bash
$ cd path/to/speedd-ml
$ sbt assembly
```

