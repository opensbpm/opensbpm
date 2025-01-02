package org.opensbpm.engine.e2e;

import java.time.Duration;
import java.time.LocalDateTime;

public class Statistics {

private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Duration duration;
    private final long count;

    public Statistics(LocalDateTime startTime, LocalDateTime endTime, Duration duration, long count) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.count = count;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s",
                getStartTime(), getEndTime(), getDuration(), getCount()
        );
    }
}
