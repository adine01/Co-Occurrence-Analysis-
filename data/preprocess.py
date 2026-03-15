import os
import csv

input_dir = os.path.expanduser("~/geolife")
output_file = os.path.expanduser("~/geolife_clean.csv")

count = 0

with open(output_file, "w", newline="") as csvfile:
    writer = csv.writer(csvfile)
    writer.writerow(["entity_id", "timestamp", "latitude", "longitude"])

    # Walk through each user folder
    for user_id in sorted(os.listdir(input_dir)):
        traj_dir = os.path.join(input_dir, user_id, "Trajectory")
        if not os.path.isdir(traj_dir):
            continue

        for plt_file in os.listdir(traj_dir):
            if not plt_file.endswith(".plt"):
                continue

            filepath = os.path.join(traj_dir, plt_file)
            with open(filepath, "r") as f:
                lines = f.readlines()

            # First 6 lines are headers in .plt format — skip them
            for line in lines[6:]:
                parts = line.strip().split(",")
                if len(parts) < 7:
                    continue

                lat = parts[0]
                lon = parts[1]
                date = parts[5]   # e.g. 2008-10-23
                time = parts[6]   # e.g. 02:53:04

                timestamp = f"{date} {time}"
                writer.writerow([f"user{user_id}", timestamp, lat, lon])
                count += 1

print(f"Done! Total records written: {count}")
