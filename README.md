# Spatial-Temporal Hotspot Detection Using Hadoop MapReduce

**Module:** Cloud Computing (EE7222/EC7204) — University of Ruhuna  
**Assignment:** Large-Scale Data Analysis Using MapReduce  
**Dataset:** Microsoft Geolife GPS Trajectory Dataset (24.8 million records)

---

## Project Structure
```
geolife-mapreduce/
├── README.md
├── data/
│   ├── preprocess.py        # Converts raw .plt files to CSV
│   └── sample_input.csv     # Sample of 1000 records
├── src/
│   └── SpatialHotspot.java  # MapReduce job
├── build/
│   └── spatialhotspot.jar   # Compiled JAR
├── evidence/
│   ├── hadoop_setup.png     # Hadoop running evidence
│   ├── hdfs_upload.png      # Dataset in HDFS
│   ├── job_running.png      # Job execution evidence
│   └── output_sample.txt    # Top 100 hotspot results
└── report/
    └── report.pdf           # 2-page project report
```

---

## Requirements

- Java JDK 8+
- Apache Hadoop 3.x
- Python 3.x
- The Geolife dataset: https://www.microsoft.com/en-us/research/publication/geolife-gps-trajectory-dataset-user-guide/

---

## Step 1 — Preprocess the Dataset

Convert raw `.plt` trajectory files into a single CSV:
```bash
# Place the Geolife Data/ folder at ~/geolife/
python3 data/preprocess.py
# Output: ~/geolife_clean.csv (24.8 million records)
```

---

## Step 2 — Start Hadoop Services
```bash
$HADOOP_HOME/sbin/start-dfs.sh
$HADOOP_HOME/sbin/start-yarn.sh

# Verify all services are running
jps
# Expected: NameNode, DataNode, SecondaryNameNode, ResourceManager, NodeManager
```

---

## Step 3 — Upload Dataset to HDFS
```bash
hdfs dfs -mkdir -p /geolife/input
hdfs dfs -put ~/geolife_clean.csv /geolife/input/

# Verify
hdfs dfs -du -h /geolife/input/
# Expected: 1.2 G
```

---

## Step 4 — Compile the MapReduce Job
```bash
javac -classpath $(hadoop classpath) -d build/ src/SpatialHotspot.java
jar -cvf build/spatialhotspot.jar -C build/ .
```

---

## Step 5 — Run the Job
```bash
hadoop jar build/spatialhotspot.jar SpatialHotspot \
  /geolife/input/geolife_clean.csv \
  /geolife/output
```

Expected runtime: ~2 minutes on a single-node setup.

---

## Step 6 — View Results
```bash
# List output files
hdfs dfs -ls /geolife/output/

# Top hotspots sorted by entity count
hdfs dfs -cat /geolife/output/part-r-* | \
  awk -F'\t' '{split($2,a,"|"); print a[1]+0"\t"$0}' | \
  sort -rn | head -20

# Count hotspots with 5+ users
hdfs dfs -cat /geolife/output/part-r-* | \
  awk -F'\t' '{split($2,a,"|"); if(a[1]+0 >= 5) print}' | wc -l
```

---

## Output Format

Each output line:
```
grid_cell|time_bucket    count|user1 user2 user3 ...
```

Example:
```
39.987_116.432|2008-05-30 12:32    7|user136 user128 user073 user078 user112 user167 user153
```

---

## Results Summary

| Metric | Value |
|---|---|
| Total GPS records processed | 24,876,978 |
| Total spatial-temporal buckets detected | 360,378 |
| Hotspots with 3+ users | 31,395 |
| Hotspots with 5+ users | 710 |
| Peak co-occurrence | 7 users, same location, same minute |
| Job execution time | ~75 seconds |
