CREATE KEYSPACE IF NOT EXISTS cnrs WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 1 };

CREATE TABLE IF NOT EXISTS cnrs.raw_input (
loc_id bigint,
lane varchar,
timestamp int,
occupancy decimal,
vehicles decimal,
avg_speed decimal,
hs map<int,decimal>,
hl map<int,decimal>,
PRIMARY KEY ((loc_id, lane), timestamp) ) 
WITH compression = { 'sstable_compression' : 'SnappyCompressor' };

CREATE INDEX IF NOT EXISTS ON cnrs.raw_input (timestamp);


CREATE TABLE IF NOT EXISTS cnrs.location (
loc_id bigint,
lane varchar,
prev_lane varchar,
coord_x decimal,
coord_y decimal,
dist int,
num int,
PRIMARY KEY (loc_id, lane) )
WITH compression = { 'sstable_compression' : 'SnappyCompressor' };



CREATE TABLE IF NOT EXISTS cnrs.annotation (
start_ts int,
end_ts int,
event_num int,
description varchar,
end_loc int,
sens int,
start_loc int,
PRIMARY KEY ((start_ts), end_ts, event_num) )
WITH compression = { 'sstable_compression' : 'SnappyCompressor' };

CREATE INDEX IF NOT EXISTS ON cnrs.annotation (description);