import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class SpatialHotspot {

    // ─── MAPPER ───────────────────────────────────────────────────────────────
    public static class SpatialMapper
            extends Mapper<Object, Text, Text, Text> {

        private Text outKey   = new Text();
        private Text outValue = new Text();

        @Override
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

            String line = value.toString().trim();

            // Skip header row
            if (line.startsWith("entity_id")) return;

            // Parse: entity_id, timestamp, latitude, longitude
            String[] parts = line.split(",");
            if (parts.length < 4) return;

            String entityId   = parts[0].trim();
            String timestamp  = parts[1].trim();
            String latStr     = parts[2].trim();
            String lonStr     = parts[3].trim();

            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);

                // Spatial bucketing: floor to 3 decimal places (~111m grid)
                double cellLat = Math.floor(lat * 1000) / 1000.0;
                double cellLon = Math.floor(lon * 1000) / 1000.0;

                // Time bucketing: keep only YYYY-MM-DD HH:MM (1-minute window)
                String timeBucket = timestamp.length() >= 16
                        ? timestamp.substring(0, 16)
                        : timestamp;

                // Key = gridCell|timeBucket
                String compositeKey = String.format("%.3f_%.3f|%s",
                        cellLat, cellLon, timeBucket);

                outKey.set(compositeKey);
                outValue.set(entityId);
                context.write(outKey, outValue);

            } catch (NumberFormatException e) {
                // Skip malformed records silently
            }
        }
    }

    // ─── REDUCER ──────────────────────────────────────────────────────────────
    public static class SpatialReducer
            extends Reducer<Text, Text, Text, Text> {

        private Text result = new Text();

        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            Set<String> entities = new HashSet<>();

            for (Text val : values) {
                entities.add(val.toString());
            }

            int count = entities.size();

            // Only output buckets with 2+ entities (actual co-occurrence)
            if (count < 2) return;

            // Build entity list string
            StringBuilder sb = new StringBuilder();
            for (String e : entities) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(e);
            }

            // Output: gridCell|timeBucket → count | entity list
            result.set(count + "|" + sb.toString());
            context.write(key, result);
        }
    }

    // ─── DRIVER ───────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: SpatialHotspot <input path> <output path>");
            System.exit(1);
        }

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Spatial Hotspot Detection");

        job.setJarByClass(SpatialHotspot.class);
        job.setMapperClass(SpatialMapper.class);
        job.setReducerClass(SpatialReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Set number of reducers for better performance
        job.setNumReduceTasks(4);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
