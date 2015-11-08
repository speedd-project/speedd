# SPEEDD ML execution and configuration

## Quick-Notes for running a local instance of the module

To start a local instance of the Cassandra DB:
```bash
$ /path/to/cassandra/bin/cassandra -f
```

To stop cassandra, press `CTRL+C`.

To start a local Spark instance, in standalone mode:
```bash
$ cd /path/to/spark/
$ ./sbin/start-all.sh
```

To stop Spark local instance:
```bash
$ cd /path/to/spark
$ ./sbin/stop-all.sh
```


## SPEEDD ML module configuration

All configuration files of the SPEEDD ML module are located in the `path/to/speedd-ml-v0.1/etc`. Depending on your 
installation and OS configuration you may need to adjust the parameters of `speedd-ml-0.1/etc/spark-defaults.conf` file.
The `spark-defaults.conf` file is a standard Spark Configuration file (see 
[Available Properties](http://spark.apache.org/docs/1.5.1/configuration.html#viewing-spark-properties) from Spark Documentation)

For example, you may need to change the spark master URL:
```
spark.master            spark://localhost:7077
```


## SPEEDD ML executable script files 

* Data loading
    * `cnrs-loader.sh` : loads data from CSV files into the Cassandra DB (Traffic Management use case)
* Machine learning
    * `speedd-wlearn.sh` : performs weight leaning
    
Example parameters for loading raw input data in:
```bash
$ cnrs-loader.sh \
    -d /path/to/CSV/ \  # path to directory containing the CSV files
    -t input \          # load and transform for raw input data
    -i suffx:csv.gz \   # load files that match to *.csv.gz (the files can be gzipped)
```

Example parameters for weight learning:
```bash
$ speedd-wlearn.sh \
    -t CNRS \                   # perform learning for the traffic management use case
    -in /path/to/cnrs.mln \     # path to the initial MLN file
    -out /tmp/cnrs-out.mln \    # path to the resulting MLN file
    -i 1397041487,1397042487 \  # the temporal interval read input and annotation data from the DB
    -bs 20                      # perform online learning for micro-batched with 20 time-points duration 
```