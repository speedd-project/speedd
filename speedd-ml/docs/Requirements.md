# Runtime Requirements

SPEEDD-ML module requires Apache Cassandra 2.1.x and Apache Spark 1.5.1 (for Scala 2.11) install in your system.

## Apache Cassandra
To install Apache Cassandra follow the instructions [here](http://wiki.apache.org/cassandra/GettingStarted)

## Apache Spark for Scala 2.11
By default Apache Spark 1.5.1 is provided only for Scala 2.10. In order to compile Apache Spark 1.5.1 for Scala 2.11, 
download Spark 1.5.1 source file [spark-1.5.1.tgz](http://spark.apache.org/downloads.html) and give the following commands:
```bash
$ tar xf spark-1.5.1.tgz
$ cd spark
$ ./dev/change-scala-version.sh 2.11
$ ./make-distribution.sh --name scala_2.11 --tgz --skip-java-test -Phadoop-2.4 -Pyarn -Dscala-2.11
```
The resulting Spark distribution will be packed in file `spark-1.5.1-bin-scala_2.11.tgz`.
