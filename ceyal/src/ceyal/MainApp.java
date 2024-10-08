package ceyal;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainApp extends Application {
    private TableView<EventLog> tableView;
    private ObservableList<EventLog> logData;
    private Pane petriNetPane;
    private Label statsLabel;
    private Scale scaleTransform;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Process Mining Software");

        logData = FXCollections.observableArrayList();
        tableView = new TableView<>(logData);

        // Setting up columns for the table
        TableColumn<EventLog, String> eventColumn = new TableColumn<>("Event");
        eventColumn.setCellValueFactory(cellData -> cellData.getValue().eventProperty());

        TableColumn<EventLog, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cellData -> cellData.getValue().timestampProperty());

        tableView.getColumns().add(eventColumn);
        tableView.getColumns().add(timestampColumn);

        // Filter field
        TextField filterField = new TextField();
        filterField.setPromptText("Filter Events...");
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            tableView.setItems(logData.filtered(log -> log.eventProperty().get().toLowerCase().contains(newValue.toLowerCase())));
        });

        // Buttons
        Button loadButton = new Button("Load Event Log");
        loadButton.setOnAction(e -> loadEventLog(primaryStage));

        Button analyzeButton = new Button("Analyze Event Log");
        analyzeButton.setOnAction(e -> performAnalysis());

        Button visualizeButton = new Button("Visualize Process");
        visualizeButton.setOnAction(e -> visualizePetriNet());

        // Zoom slider
        Slider zoomSlider = new Slider(0.5, 2.0, 1.0);  // Min 0.5x, Max 2.0x, Default 1.0x
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.setMinorTickCount(5);
        zoomSlider.setSnapToTicks(true);
        zoomSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            scaleTransform.setX(newValue.doubleValue());
            scaleTransform.setY(newValue.doubleValue());
        });

        // Statistics label
        statsLabel = new Label("Statistics: ");
        statsLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10;");

        VBox buttonBox = new VBox(loadButton, analyzeButton, visualizeButton, zoomSlider, statsLabel);
        buttonBox.setSpacing(10);

        // Create SplitPane
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.5); // Set the initial position of the divider

        // Left Pane for Event Log
        VBox leftPane = new VBox();
        leftPane.getChildren().addAll(filterField, tableView);
        splitPane.getItems().add(leftPane);

        // Right Pane for Petri net visualization with scroll and zoom capabilities
        petriNetPane = new Pane(); // Create a Pane for Petri net visualization
        ScrollPane scrollPane = new ScrollPane(petriNetPane);  // Add ScrollPane for the Petri net pane
        splitPane.getItems().add(scrollPane); // Add the scrollable Petri net pane to the split pane

        // Add button box at the top of the application
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(buttonBox);
        borderPane.setCenter(splitPane);

        Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Scale transformation for zooming
        scaleTransform = new Scale(1.0, 1.0);
        petriNetPane.getTransforms().add(scaleTransform);

        // Zoom with mouse wheel
        petriNetPane.setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            double scaleFactor = (delta > 0) ? 1.1 : 0.9;
            scaleTransform.setX(scaleTransform.getX() * scaleFactor);
            scaleTransform.setY(scaleTransform.getY() * scaleFactor);
        });
    }

    private void loadEventLog(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Event Log File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            readEventLog(file);
        }
    }

    private void readEventLog(File file) {
        logData.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    logData.add(new EventLog(parts[0], parts[1]));
                }
            }
            updateStatistics();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performAnalysis() {
        ProcessMiningAnalysis analysis = new ProcessMiningAnalysis();

        // Process Discovery
        Map<String, Integer> discoveredProcess = analysis.processDiscovery(logData);
        StringBuilder discoveryResults = new StringBuilder("Process Discovery Results:\n");
        discoveredProcess.forEach((event, count) -> discoveryResults.append(event).append(": ").append(count).append("\n"));

        // Conformance Checking
        boolean isConformant = analysis.conformanceCheck(logData, "Start,Process,End");
        String conformanceMessage = isConformant ? "The process log conforms to the expected model." : "The process log does not conform to the expected model.";

        // Performance Mining
        double averageDuration = analysis.calculateAverageEventDuration(logData);

        // Display Results
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Process Mining Analysis Results");
        alert.setHeaderText(null);
        alert.setContentText(discoveryResults.toString() + conformanceMessage + "\nAverage Event Duration: " + averageDuration);
        alert.showAndWait();
    }

    private void visualizePetriNet() {
        petriNetPane.getChildren().clear(); // Clear previous visualization

        double xOffset = 150; // Starting x position for the first place/transition
        double yOffset = 100; // Starting y position for the first place

        // Draw Places and Transitions based on event logs
        for (int i = 0; i < logData.size(); i++) {
            EventLog log = logData.get(i);
            String eventName = log.eventProperty().get();

            // Create a place (circle) for each event
            Circle place = new Circle(20, Color.LIGHTGREEN);
            place.setCenterX(xOffset);
            place.setCenterY(yOffset);
            place.setOnMouseClicked(e -> showEventDetails(eventName));
            petriNetPane.getChildren().add(place);

            // Create a transition (rectangle) for each event
            Rectangle transition = new Rectangle(xOffset - 30, yOffset + 40, 60, 20);
            transition.setFill(Color.LIGHTBLUE);
            transition.setOnMouseClicked(e -> showEventDetails(eventName));
            petriNetPane.getChildren().add(transition);

            // Create lines (arcs) between places and transitions
            Line arc1 = new Line(xOffset, yOffset + 20, xOffset, yOffset + 40);
            petriNetPane.getChildren().add(arc1);

            // Add event name as text label
            Label eventLabel = new Label(eventName);
            eventLabel.setLayoutX(xOffset - 20);
            eventLabel.setLayoutY(yOffset + 45);
            petriNetPane.getChildren().add(eventLabel);

            // Set next position for the next event
            yOffset += 80; // Adjust spacing for next event (vertically)
        }

        // Connect last place to a final place (optional)
        Circle finalPlace = new Circle(20, Color.LIGHTCORAL);
        finalPlace.setCenterX(xOffset);
        finalPlace.setCenterY(yOffset);
        petriNetPane.getChildren().add(finalPlace);
    }

    private void showEventDetails(String eventName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Event Details");
        alert.setHeaderText(null);
        alert.setContentText("Details for event: " + eventName);
        alert.showAndWait();
    }

    private void updateStatistics() {
        int totalEvents = logData.size();
        long uniqueEvents = logData.stream().map(EventLog::eventProperty).distinct().count();
        statsLabel.setText("Statistics: Total Events: " + totalEvents + ", Unique Events: " + uniqueEvents);
    }

    public static class EventLog {
        private final SimpleStringProperty event;
        private final SimpleStringProperty timestamp;

        public EventLog(String event, String timestamp) {
            this.event = new SimpleStringProperty(event);
            this.timestamp = new SimpleStringProperty(timestamp);
        }

        public SimpleStringProperty eventProperty() {
            return event;
        }

        public SimpleStringProperty timestampProperty() {
            return timestamp;
        }
    }

    public static class ProcessMiningAnalysis {
        public Map<String, Integer> processDiscovery(ObservableList<EventLog> logs) {
            Map<String, Integer> processMap = new HashMap<>();
            for (EventLog log : logs) {
                processMap.put(log.eventProperty().get(), processMap.getOrDefault(log.eventProperty().get(), 0) + 1);
            }
            return processMap;
        }

        public boolean conformanceCheck(ObservableList<EventLog> logs, String expectedModel) {
            // Placeholder for actual conformance checking logic
            return true; // Assume it always conforms for this example
        }

        public double calculateAverageEventDuration(ObservableList<EventLog> logs) {
            // Placeholder for actual duration calculation logic
            return logs.size() > 0 ? 1.0 : 0; // Return dummy value for now
        }
    }
}
