# Project Structure

## Source Organization

### Main Source (`src/main/java`)
```
com.duckblade.osrs.sailing/
├── SailingPlugin.java          # Main plugin entry point
├── SailingConfig.java          # Configuration interface with all user settings
├── features/                   # Feature implementations (one package per feature)
│   ├── barracudatrials/       # Barracuda Trials helpers
│   ├── charting/              # Sea charting overlays and solvers
│   ├── courier/               # Courier task tracking
│   ├── crewmates/             # Crewmate-related features
│   ├── facilities/            # Ship facility management
│   ├── mes/                   # Menu Entry Swaps
│   ├── navigation/            # Navigation overlays and helpers
│   ├── oceanencounters/       # Ocean encounter notifications
│   ├── reversebeep/           # Fun features
│   ├── salvaging/             # Salvaging highlights
│   ├── trawling/              # Trawling assistance
│   └── util/                  # Shared utilities for features
├── model/                      # Data models (Boat, Port, CourierTask, etc.)
└── module/                     # Dependency injection and lifecycle management
    ├── ComponentManager.java           # Manages component lifecycle
    ├── PluginLifecycleComponent.java   # Interface for managed components
    └── SailingModule.java              # Guice module configuration
```

### Test Source (`src/test/java`)
```
com.duckblade.osrs.sailing/
├── SailingPluginTest.java      # Test runner with main() method
├── debugplugin/                # Debug plugin for development
└── features/                   # Unit tests for features
```

### Resources
- `src/main/resources/` - Plugin resources (audio files, etc.)
- `src/generateSeaChartTasks/` - Code generation for sea chart tasks

## Architecture Patterns

### Component Lifecycle Pattern
All features implement `PluginLifecycleComponent` interface:
- `isEnabled(SailingConfig)` - Determines if component should be active based on config
- `startUp()` - Initialize when enabled
- `shutDown()` - Clean up when disabled

The `ComponentManager` automatically:
- Registers/unregisters components with EventBus
- Adds/removes Overlays from OverlayManager
- Adds/removes InfoBoxes from InfoBoxManager
- Simulates game events for newly enabled components
- Revalidates all components on config changes

### Dependency Injection
- Use `@Inject` constructor injection (prefer `@RequiredArgsConstructor(onConstructor = @__(@Inject))`)
- Components are `@Singleton` by default
- All components are bound in `SailingModule`

### Configuration
- All settings defined in `SailingConfig` interface
- Organized into sections with `@ConfigSection`
- Config changes trigger `ConfigChanged` events
- ComponentManager automatically enables/disables features based on config

### Event-Driven
- Subscribe to RuneLite events with `@Subscribe` annotation
- Components only receive events when enabled
- Use EventBus for inter-component communication

## Naming Conventions
- Feature packages are lowercase, descriptive names
- One feature per package under `features/`
- Overlay classes end with `Overlay` or `OverlayPanel`
- Tracker classes end with `Tracker`
- Helper classes end with `Helper`
- Config keys use camelCase with feature prefix (e.g., `barracudaHighlightLostCrates`)
