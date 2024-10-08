package ceyal;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventLog {
    private final StringProperty event;
    private final StringProperty timestamp;
    private LocalDateTime eventTime;

    public EventLog(String event, String timestamp) {
        this.event = new SimpleStringProperty(event);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.eventTime = parseTimestamp(timestamp);
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(timestamp, formatter);
    }

    public StringProperty eventProperty() {
        return event;
    }

    public StringProperty timestampProperty() {
        return timestamp;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }
}
