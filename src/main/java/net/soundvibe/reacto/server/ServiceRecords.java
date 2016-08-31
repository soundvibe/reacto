package net.soundvibe.reacto.server;

import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.Status;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * @author OZY on 2016.08.26.
 */
public interface ServiceRecords {

    String LAST_UPDATED = "updated";

    static boolean AreEquals(Record existingRecord, Record newRecord) {
        return  Objects.equals(newRecord.getName(), existingRecord.getName()) &&
                Objects.equals(newRecord.getLocation(), existingRecord.getLocation()) &&
                Objects.equals(newRecord.getType(), existingRecord.getType());
    }

    static boolean isDown(Record existingRecord) {
        return existingRecord.getStatus() == Status.DOWN || !isUpdatedRecently(existingRecord);
    }

    static boolean isUpdatedRecently(Record record) {
        final Instant now = Instant.now();
        return Duration.between(record.getMetadata().getInstant(LAST_UPDATED, now),
                now)
                .toMinutes() < 4L;
    }

}
