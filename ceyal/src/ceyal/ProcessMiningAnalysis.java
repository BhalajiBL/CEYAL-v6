package ceyal;

import javafx.collections.ObservableList;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ProcessMiningAnalysis {
    public Map<String, Integer> processDiscovery(ObservableList<EventLog> logData) {
        Map<String, Integer> processMap = new HashMap<>();
        for (EventLog log : logData) {
            processMap.put(log.getEvent(), processMap.getOrDefault(log.getEvent(), 0) + 1);
        }
        return processMap;
    }


    // Method for calculating average event duration
    public double calculateAverageEventDuration(ObservableList<EventLog> logs) {
        if (logs.size() < 2) return 0.0;  // Need at least two logs to calculate duration

        Duration totalDuration = Duration.ZERO;
        for (int i = 0; i < logs.size() - 1; i++) {
            Duration duration = Duration.between(logs.get(i).getEventTime(), logs.get(i + 1).getEventTime());
            totalDuration = totalDuration.plus(duration);
        }

        return totalDuration.toMillis() / (double) (logs.size() - 1) / 1000; // average in seconds
    }

    // Implement additional conformance checking method if needed
    public boolean conformanceCheck(ObservableList<EventLog> logs, String expectedProcess) {
        // Placeholder: Implement your conformance checking logic here
        return true; // Assume conformance for now
    }
}
