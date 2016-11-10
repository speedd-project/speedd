# SPEEDD ML Execution and Configuration

## Quick-Notes for running a local instance of the module

To start a local instance of the PostgreSQL database on the background type:
```bash
su - postgres
/usr/local/pgsql/bin/pg_ctl -D /usr/local/pgsql/data start
```

## SPEEDD ML Module Configuration

All configuration files of the SPEEDD ML module are located in the `path/to/speedd-ml-<version>/etc`. Depending on your
installation of PostgreSQL database you may need to change url, user or password parameters of the `speedd-ml-<version>/etc/application.conf` file.
It is not recommended to change anything else unless you are aware of the consequences.


## SPEEDD ML Executable Scripts

* Data loading
    * `speedd-dataloader` : loads data from CSV files into database
* Data visualization
    * `cnrs-plot-collected` : plots data loaded from the database (collected CNRS dataset)
    * `cnrs-plot-simulated` : plots data loaded from the database (simulated CNRS datasets)
* Machine learning
    * `speedd-wlearn` : performs weight learning
    * `speedd-slearn` : performs structure learning
* Inference
    * `speedd-inference` : performs inference

Example parameters for loading raw input data in the database:
```bash
$ speedd-dataloader \
    -d /path/to/CSV/ \  # path to directory containing the CSV files
    -t task \           # the loading task transformation for the raw input data
    -i suffx:csv.gz \   # load files that match to *.csv.gz (the files can be gzipped)
```

Example parameters for visualizing raw input data:
```bash
$ cnrs-plot-collected \
    -d occupancy,vehicles \     # comma separated columns
    -i 1396300080,1396400080 \  # the temporal interval for loading input and annotation data
    -loc 10314363961353708 \    # the location id of interest
    -lane Fast \                # the lane type of interest
    -r 0.0,110.0 \              # the range of the y-axis
```

Example parameters for weight learning:
```bash
$ speedd-wlearn \
    -t cnrs.collected \               # perform learning for the collected CNRS dataset
    -f2sql /path/to/f2sql.map         # a set of mappings from FOL functions to SQL
    -in /path/to/cnrs.mln \           # path to the input MLN file
    -out /path/to/cnrs-learned.mln \  # path to the resulting MLN file
    -i 1396300080,1396400080 \        # the temporal interval for input and annotation data used for learning
    -exclude 1396310080,1396350080    # excluded temporal interval (may be used for testing)
    -bs 100                           # online learning for 100 time-points duration micro-batches
```

Example parameters for structure learning:
```bash
$ speedd-slearn \
    -t cnrs.collected \               # perform learning for the collected CNRS dataset
    -m /path/to/declarations.modes    # mode declaration file
    -f2sql /path/to/f2sql.map         # a set of mappings from FOL functions to SQL
    -in /path/to/cnrs.mln \           # path to the input MLN file
    -out /path/to/cnrs-learned.mln \  # path to the resulting MLN file
    -i 1396300080,1396400080 \        # the temporal interval for input and annotation data used for learning
    -exclude 1396310080,1396350080    # excluded temporal interval (may be used for testing)
    -bs 100                           # online learning for 100 time-points duration micro-batches
    -maxLength 8                      # maximum length of literals for each rule
    -threshold 1                      # evaluation threshold
```