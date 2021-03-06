package cc.blynk.server.workers;

import cc.blynk.server.core.model.enums.GraphType;
import cc.blynk.server.core.reporting.average.AggregationKey;
import cc.blynk.server.core.reporting.average.AggregationValue;
import cc.blynk.server.core.reporting.average.AverageAggregator;
import cc.blynk.server.db.DBManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

import static cc.blynk.server.core.dao.ReportingDao.*;

/**
 * Worker that runs once a minute. During run - stores all aggregated reporting data
 * to disk. Also sends all data in batches to RDBMS in case DBManager was initialized.
 *
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 10.08.15.
 */
public class StorageWorker implements Runnable {

    private static final Logger log = LogManager.getLogger(StorageWorker.class);
    private static final Comparator<AggregationKey> AGGREGATION_KEY_COMPARATOR = (o1, o2) -> (int) (o1.ts - o2.ts);

    private final AverageAggregator averageAggregator;
    private final String reportingPath;
    private final DBManager dbManager;

    public StorageWorker(AverageAggregator averageAggregator, String reportingPath, DBManager dbManager) {
        this.averageAggregator = averageAggregator;
        this.reportingPath = reportingPath;
        this.dbManager = dbManager;
    }

    /**
     * Simply writes single reporting entry to disk (16 bytes).
     * Reporting entry is value (double) and timestamp (long)
     *
     * @param reportingPath - path to user specific reporting file
     * @param value - sensor data
     * @param ts - time when entry was created
     * @throws IOException
     */
    public static void write(Path reportingPath, double value, long ts) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                Files.newOutputStream(reportingPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            dos.writeDouble(value);
            dos.writeLong(ts);
            dos.flush();
        }
    }

    @Override
    public void run() {
        Map<AggregationKey, AggregationValue> removedKeys;

        removedKeys = process(averageAggregator.getMinute(), GraphType.MINUTE);
        dbManager.insertReporting(removedKeys, GraphType.MINUTE);

        removedKeys = process(averageAggregator.getHourly(), GraphType.HOURLY);
        dbManager.insertReporting(removedKeys, GraphType.HOURLY);

        removedKeys = process(averageAggregator.getDaily(), GraphType.DAILY);
        dbManager.insertReporting(removedKeys, GraphType.DAILY);

        dbManager.cleanOldReportingRecords(Instant.now());
    }

    /**
     * Iterates over all reporting entries that were created during last minute.
     * And stores all entries one by one to disk.
     *
     * @param map - reporting entires that were created during last minute.
     * @param type - type of reporting. Could be minute, hourly, daily.
     * @return - returns list of reporting entries that were successfully flushed to disk.
     */
    private Map<AggregationKey, AggregationValue>  process(Map<AggregationKey, AggregationValue> map, GraphType type) {
        long nowTruncatedToPeriod = System.currentTimeMillis() / type.period;

        List<AggregationKey> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys, AGGREGATION_KEY_COMPARATOR);

        Map<AggregationKey, AggregationValue> removedKeys = new HashMap<>();

        for (AggregationKey keyToRemove : keys) {
            //if prev hour
            if (keyToRemove.ts < nowTruncatedToPeriod) {
                AggregationValue value = map.get(keyToRemove);

                try {
                    final Path userReportFolder = Paths.get(reportingPath, keyToRemove.username);
                    if (Files.notExists(userReportFolder)) {
                        Files.createDirectories(userReportFolder);
                    }

                    String fileName = generateFilename(keyToRemove.dashId, keyToRemove.pinType, keyToRemove.pin, type);
                    Path filePath = Paths.get(userReportFolder.toString(), fileName);

                    write(filePath, value.calcAverage(), keyToRemove.ts * type.period);

                    final AggregationValue removedValue = map.remove(keyToRemove);
                    removedKeys.put(keyToRemove, removedValue);
                } catch (IOException ioe) {
                    log.error("Error open user data reporting file. Reason : {}", ioe.getMessage());
                }
            }
        }

        return removedKeys;
    }

}
