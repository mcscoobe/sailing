# Technology Stack

## Build System
- **Gradle** with Groovy DSL
- Java 11 target (options.release.set(11))
- Checkstyle for code quality

## Core Dependencies
- **RuneLite Client API** (latest.release) - Plugin framework and game client integration
- **Lombok** (1.18.30) - Reduces boilerplate with annotations (@Slf4j, @Inject, @RequiredArgsConstructor, etc.)
- **Google Guice** - Dependency injection framework (via RuneLite)
- **JUnit 4.12** - Testing framework

## Key Libraries
- RuneLite EventBus - Event-driven architecture
- RuneLite Overlay API - In-game visual overlays
- RuneLite Config API - User configuration management
- RuneLite InfoBox API - UI info boxes

## Common Commands

### Build
```bash
./gradlew build
```

### Run Test Client
```bash
./gradlew runTestClient
```
Launches the plugin in developer mode with the test client.

### Create Shadow JAR
```bash
./gradlew shadowJar
```
Creates a fat JAR with all dependencies at `build/libs/sailing-{version}-all.jar`.

### Run Tests
```bash
./gradlew test
```

### Code Quality
```bash
./gradlew checkstyleMain
./gradlew checkstyleTest
```

## Development Notes
- Use `@Slf4j` for logging
- Use `@Inject` for dependency injection
- Configuration changes trigger automatic component revalidation
- Components are managed via the ComponentManager lifecycle system
