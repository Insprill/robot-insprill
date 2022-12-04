package me.Insprill.RI.utils;

import java.time.Duration;
import java.time.Instant;

public class StopWatch {

    Instant startTime, endTime;
    Duration duration;
    boolean isRunning = false;

    public void start() {
        if (isRunning)
            throw new RuntimeException("Stopwatch is already running.");
        isRunning = true;
        startTime = Instant.now();
    }

    public void stop() {
        endTime = Instant.now();
        if (!isRunning)
            throw new RuntimeException("Stopwatch has not been started yet");
        isRunning = false;
        Duration result = Duration.between(startTime, endTime);
        if (duration == null)
            duration = result;
        else
            duration = duration.plus(result);
    }

    public Duration getElapsedTime() {
        return duration;
    }
}