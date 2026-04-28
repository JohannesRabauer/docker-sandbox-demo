You are a senior Java developer. Generate a complete, minimal but well-structured Spring Boot application using Vaadin Flow that implements a “Fake Racing Telemetry Viewer”.

## Context

* The project is already created via Spring Initializr
* Vaadin is already included as a dependency
* Use Java (not Kotlin)
* Use the library: https://github.com/xdev-software/chartjs-java-model for chart configuration
* The UI must be built with Vaadin Flow (server-side)

## Functional Requirements

Create a single-page Vaadin application that displays a live-updating telemetry chart with the following signals:

* Speed (km/h)
* Throttle (%)
* Brake (%)

The chart must:

* Use Chart.js via the xdev chartjs-java-model
* Display 3 line datasets simultaneously
* Update every 200–500 ms
* Show only the most recent ~50 data points (sliding window)

## Telemetry Simulation

Implement a deterministic fake telemetry generator:

* Use sine waves for smooth continuous signals
* Add slight randomness for realism
* Ensure values stay within realistic bounds:

  * Speed: 0–350
  * Throttle: 0–100
  * Brake: 0–100

Simulate driving behavior:

* Throttle and brake should not peak at the same time
* Speed should correlate with throttle and inversely with brake

## Technical Requirements

### Architecture

* Separate concerns:

  * `TelemetryService` (data generation)
  * `TelemetryPoint` (data model)
  * `TelemetryView` (Vaadin UI)
* Avoid putting logic directly into the UI class

### Vaadin UI

* Use a `VerticalLayout`
* Include:

  * Title (e.g. “Telemetry Viewer”)
  * Chart component
* Use `@Push` for live updates
* Use `ScheduledExecutorService` for periodic updates
* Ensure thread safety using `UI.access()`

### Chart Configuration

* Use xdev chartjs-java-model classes to build the config
* Configure:

  * Line chart
  * Labels as timestamps or tick index
  * Distinct colors for each dataset:

    * Speed (blue)
    * Throttle (green)
    * Brake (red)
* Disable animations if needed for smoother updates

### Performance

* Maintain a fixed-size dataset (max ~50 points)
* Remove oldest entries when limit is exceeded

## Testing Requirements

Write unit tests using JUnit 5:

### TelemetryService Tests

* Verify generated values stay within bounds
* Verify no simultaneous strong brake and throttle peaks
* Verify time progression changes values

### Basic Integration Test

* Load Spring context successfully
* Instantiate the view without errors

Do NOT skip tests.

## Code Quality

* Use clean, readable structure
* Avoid overly complex abstractions
* Add brief comments where non-obvious logic exists
* Use meaningful class and method names

## Output Format

Provide:

1. Full source code for all classes
2. Test classes
3. Any required configuration snippets

Do not include explanations unless necessary. Focus on correct, runnable code.
