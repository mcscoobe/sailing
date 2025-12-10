# Requirements Document

## Introduction

This feature enhances the existing trawling system in the Sailing plugin by adding shoal depth state tracking and improving the net depth button highlighting logic. The enhancements will help players better track when their fishing net depth matches the current shoal depth, and provide more accurate button highlighting for areas where shoals can transition between all three depth levels (shallow, moderate, deep).

## Glossary

- **Shoal**: A group of fish that moves through fishing areas in the game
- **Net Depth**: The depth at which the player's fishing net is deployed (shallow, moderate, or deep)
- **Shoal Depth**: The current depth level of the active shoal (shallow, moderate, or deep)
- **Fishing Area**: A geographic region where trawling occurs (e.g., Bluefin area, Marlin area)
- **Bluefin/Marlin Areas**: Special fishing areas where shoals can use all three depth levels, unlike other areas that only use two depths
- **Button Highlighting**: Visual indication on UI buttons showing which net depth adjustment to make
- **Shoal Highlight**: Visual overlay on the game world showing the shoal's location
- **ShoalDepthTracker**: A new component that tracks the current depth state of the active shoal
- **NetDepthTimer**: Existing component that manages timing for net depth changes
- **NetDepthButtonHighlighter**: A new component that handles button highlighting logic for net depth adjustments
- **ShoalOverlay**: Existing component that renders visual highlights for shoals in the game world

## Requirements

### Requirement 1

**User Story:** As a player, I want to see when my net depth doesn't match the shoal depth, so that I can quickly adjust my nets to catch more fish.

#### Acceptance Criteria

1. WHEN the player's net depth does not match the shoal depth THEN the ShoalOverlay SHALL render the shoal highlight in red color
2. WHEN the player's net depth matches the shoal depth THEN the ShoalOverlay SHALL render the shoal highlight using the configured color from settings
3. WHEN the shoal depth changes THEN the ShoalOverlay SHALL update the highlight color within one game tick
4. WHEN no shoal is active THEN the ShoalOverlay SHALL not render any highlights

### Requirement 2

**User Story:** As a player, I want the system to track the current shoal depth state, so that the plugin can provide accurate visual feedback about net depth matching.

#### Acceptance Criteria

1. WHEN a shoal spawns THEN the ShoalDepthTracker SHALL initialize with the starting depth for that fishing area
2. WHEN the shoal depth changes based on timing THEN the ShoalDepthTracker SHALL update its tracked depth state
3. WHEN a shoal despawns THEN the ShoalDepthTracker SHALL clear its tracked state
4. WHEN queried for current depth THEN the ShoalDepthTracker SHALL return the current shoal depth or null if no shoal is active
5. WHEN the player changes fishing areas THEN the ShoalDepthTracker SHALL reset and track the new area's shoal depth pattern

### Requirement 3

**User Story:** As a player fishing in Bluefin or Marlin areas, I want accurate button highlighting that accounts for the three-depth system, so that I know which net adjustment to make.

#### Acceptance Criteria

1. WHEN the shoal is at deep depth in a three-depth area THEN the NetDepthButtonHighlighter SHALL highlight the button to move nets to moderate depth
2. WHEN the shoal is at shallow depth in a three-depth area THEN the NetDepthButtonHighlighter SHALL highlight the button to move nets to moderate depth
3. WHEN the shoal is at moderate depth in a three-depth area AND the system has not detected movement direction THEN the NetDepthButtonHighlighter SHALL not highlight any buttons
4. WHEN the shoal is at moderate depth in a three-depth area AND a chat message indicates movement to deep THEN the NetDepthButtonHighlighter SHALL highlight the button to move nets to deep depth
5. WHEN the shoal is at moderate depth in a three-depth area AND a chat message indicates movement to shallow THEN the NetDepthButtonHighlighter SHALL highlight the button to move nets to shallow depth

### Requirement 4

**User Story:** As a player, I want the system to parse chat messages about shoal movement, so that button highlighting works correctly in three-depth areas when the shoal is at moderate depth.

#### Acceptance Criteria

1. WHEN a chat message contains text indicating the shoal moved deeper THEN the ShoalDepthTracker SHALL record the next depth transition as moderate-to-deep
2. WHEN a chat message contains text indicating the shoal moved shallower THEN the ShoalDepthTracker SHALL record the next depth transition as moderate-to-shallow
3. WHEN the shoal completes a depth transition THEN the ShoalDepthTracker SHALL clear the recorded movement direction
4. WHEN multiple chat messages arrive in sequence THEN the ShoalDepthTracker SHALL use only the most recent movement direction

### Requirement 5

**User Story:** As a developer, I want the shoal depth tracking logic separated into its own class, so that both ShoalOverlay and NetDepthButtonHighlighter can access the same depth state.

#### Acceptance Criteria

1. WHEN ShoalDepthTracker is created THEN the system SHALL register it as a singleton component
2. WHEN ShoalOverlay needs current shoal depth THEN the system SHALL provide access to ShoalDepthTracker via dependency injection
3. WHEN NetDepthButtonHighlighter needs current shoal depth THEN the system SHALL provide access to ShoalDepthTracker via dependency injection
4. WHEN ShoalDepthTracker updates its state THEN both ShoalOverlay and NetDepthButtonHighlighter SHALL have access to the updated state without additional synchronization
5. WHEN the plugin shuts down THEN the ShoalDepthTracker SHALL clean up its state properly

### Requirement 6

**User Story:** As a developer, I want button highlighting logic separated from the timer component, so that the highlighting can be reused and the timer remains focused on timing concerns.

#### Acceptance Criteria

1. WHEN NetDepthButtonHighlighter is created THEN the system SHALL register it as an overlay component
2. WHEN NetDepthButtonHighlighter is enabled THEN the system SHALL render button highlights on the facilities panel
3. WHEN NetDepthTimer needs to display timing information THEN the system SHALL be able to access NetDepthButtonHighlighter state
4. WHEN the player's net depth matches the required depth THEN the NetDepthButtonHighlighter SHALL not render any highlights
5. WHEN the facilities panel is not visible THEN the NetDepthButtonHighlighter SHALL not render any highlights
