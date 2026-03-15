# Spatial-Temporal Hotspot Detection Using Hadoop MapReduce
**University of Ruhuna — Faculty of Engineering**  
**Module:** Cloud Computing (EE7222/EC7204) | **Semester 7, February 2026**

---

## 1. Approach

Large-scale GPS trajectory datasets present significant challenges for traditional
processing tools due to their volume and complexity. This project implements a
custom Hadoop MapReduce job to process the Microsoft Geolife GPS Trajectory
Dataset — a real-world dataset containing 24,876,978 GPS records collected from
182 users across multiple years.

The core challenge addressed is that raw GPS coordinates cannot be directly
compared due to measurement precision differences. Two readings from the same
physical location may differ slightly (e.g., 39.9871 vs 39.9873). To solve this,
the system applies spatial bucketing — converting raw coordinates into grid cells
using floor discretization to 3 decimal places, giving approximately 111-meter
resolution per cell. Similarly, timestamps are bucketed into 1-minute windows to
group users present in the same area during the same period.

The MapReduce pipeline operates as follows. The Mapper reads each GPS record,
converts coordinates into a grid cell and timestamp into a time bucket, and emits
a composite key (grid_cell|time_bucket) with the user ID as the value. Hadoop's
Shuffle phase automatically groups all user IDs sharing the same spatial-temporal
key. The Reducer then deduplicates the user list, counts co-located entities, and
outputs only buckets where two or more users were detected simultaneously.

The job was implemented in Java and executed on Apache Hadoop 3.4.3 in
single-node pseudo-distributed mode, processing the full 1.2 GB dataset across
10 map tasks and 4 reduce tasks.

---

## 2. Results and Interpretation

The MapReduce job completed successfully in approximately 75 seconds, processing
all 24,876,978 records and producing 360,378 spatial-temporal co-occurrence
records.

**Key findings:**

- 360,378 location-time buckets contained two or more users simultaneously
- 31,395 buckets had three or more users present at the same time
- 710 high-density hotspots were detected with five or more users
- The peak co-occurrence detected was 7 users at coordinates 39.987, 116.432
  (Haidian District, Beijing) on 2008-05-30 at 12:32 — sustained across
  multiple consecutive grid cells indicating coordinated group movement

The spatial hotspots with the highest user counts are concentrated in the
Haidian and Chaoyang districts of Beijing, which are known high-activity urban
areas containing universities, commercial centres, and transportation hubs. The
consistent group of users (user073, user078, user112, user128, user136, user153,
user167) appearing together across 25+ consecutive minutes and multiple grid
cells suggests a shared commute or organized group activity rather than random
co-location.

From a performance perspective, the job processed 24.8 million records in
~75 seconds with zero shuffle errors, demonstrating the effectiveness of
the MapReduce paradigm for large-scale geospatial data. The 4-reducer
configuration produced balanced output across four part files of approximately
equal size (~4.5 MB each).

This system can be expanded by incorporating density-based clustering (DBSCAN)
to merge adjacent hotspot cells, adding a visualization layer using Apache Zeppelin
or a web dashboard, or scaling to a multi-node cluster for real-time trajectory
stream processing using Apache Kafka and Spark Streaming.
