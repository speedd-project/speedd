# SPEEDD ML execution and configuration

## Quick-Notes for running a local instance of the module

To start a local instance of the PostgreSQL DB on the background:
```bash
su - postgres
/usr/local/pgsql/bin/pg_ctl -D /usr/local/pgsql/data start
```

## SPEEDD ML module configuration

All configuration files of the SPEEDD ML module are located in the `path/to/speedd-ml-v0.2/etc`. Depending on your
installation of PostgreSQL you may need to change url, user or password parameters of `speedd-ml-0.2/etc/application.conf` file.
It is not recommended to change anything else unless you are aware of the consequences.


## SPEEDD ML executable script files

* Data loading
    * `cnrs-loader` : loads data from CSV files into the PostgreSQL DB (Traffic Management use case)
* Data visualization
    * `cnrs-plot` : plots data loaded from PostgreSQL DB (Traffic Management use case)
* Machine learning
    * `speedd-wlearn` : performs weight leaning
* Inference
    * `speedd-inference` : performs inference

Example parameters for loading raw input data in:
```bash
$ cnrs-loader \
    -d /path/to/CSV/ \  # path to directory containing the CSV files
    -t input \          # load and transform for raw input data
    -i suffx:csv.gz \   # load files that match to *.csv.gz (the files can be gzipped)
```

Example parameters for weight learning:
```bash
$ speedd-wlearn \
    -t CNRS \                   # perform learning for the traffic management use case
    -in /path/to/cnrs.mln \     # path to the initial MLN file
    -out /tmp/cnrs-out.mln \    # path to the resulting MLN file
    -i 1396300080,1396400080 \  # the temporal interval read input and annotation data from the DB
    -bs 100                      # perform online learning for micro-batched with 100 time-points duration
```