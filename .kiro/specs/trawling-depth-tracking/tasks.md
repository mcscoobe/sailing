# Implementation Plan

- [x] 1. Create NetDepth enum and supporting data models











  - Create `NetDepth` enum with SHALLOW, MODERATE, DEEP values
  - Add level-based comparison methods (isShallowerThan, isDeeperThan)
  - Create `MovementDirection` enum with SHALLOWER, DEEPER, UNKNOWN values
  - Create `FishingAreaType` enum with TWO_DEPTH and THREE_DEPTH values
  - _Requirements: 2.1, 2.2, 3.1, 3.2, 4.1, 4.2_

- [x] 2. Implement ShoalDepthTracker service component





  - Create ShoalDepthTracker class implementing PluginLifecycleComponent
  - Add state fields: currentDepth, isThreeDepthArea, nextMovementDirection, activeShoalLocation
  - Implement isEnabled() to always return true (service component)
  - Implement startUp() and shutDown() lifecycle methods
  - Add public getter methods: getCurrentDepth(), isThreeDepthArea(), getNextMovementDirection()
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 5.1, 5.4_

- [ ] 3. Add shoal spawn and despawn handling to ShoalDepthTracker







  - Implement onGameObjectSpawned to detect shoal spawns
  - Determine fishing area from shoal location using TrawlingData.FishingAreas
  - Initialize currentDepth based on area's depth pattern
  - Set isThreeDepthArea flag for Bluefin/Marlin areas
  - Implement onGameObjectDespawned to clear state on shoal despawn
  - Implement onWorldEntitySpawned to track moving shoal entity
  - _Requirements: 2.1, 2.3, 2.5_

- [ ] 3.1 Write property test for shoal spawn initialization
  - **Property 5: Shoal spawn initializes correct depth**
  - **Validates: Requirements 2.1**

- [ ] 3.2 Write property test for despawn state clearing
  - **Property 7: Despawn clears state**
  - **Validates: Requirements 2.3**

- [ ] 4. Add depth transition logic to ShoalDepthTracker
  - Add notifyDepthChange(NetDepth newDepth) method
  - Update currentDepth when notified of transitions
  - Clear nextMovementDirection after depth transitions
  - Implement onGameTick to track timing-based transitions
  - _Requirements: 2.2, 4.3_

- [ ] 4.1 Write property test for depth state updates
  - **Property 6: Depth state updates on timing transitions**
  - **Validates: Requirements 2.2**

- [ ] 4.2 Write property test for transition clearing direction
  - **Property 13: Transition clears movement direction**
  - **Validates: Requirements 4.3**

- [ ] 5. Add chat message parsing to ShoalDepthTracker
  - Implement onChatMessage event handler
  - Parse messages for "deeper" keywords and set nextMovementDirection to DEEPER
  - Parse messages for "shallower" keywords and set nextMovementDirection to SHALLOWER
  - Only process messages when in three-depth areas
  - Store only the most recent movement direction
  - _Requirements: 4.1, 4.2, 4.4_

- [ ] 5.1 Write property test for chat message parsing (deeper)
  - **Property 11: Chat message sets movement direction for deep**
  - **Validates: Requirements 4.1**

- [ ] 5.2 Write property test for chat message parsing (shallower)
  - **Property 12: Chat message sets movement direction for shallow**
  - **Validates: Requirements 4.2**

- [ ] 5.3 Write property test for latest message wins
  - **Property 14: Latest chat message wins**
  - **Validates: Requirements 4.4**

- [ ] 6. Register ShoalDepthTracker in SailingModule
  - Add ShoalDepthTracker to lifecycleComponents() provider method
  - Ensure it's registered as a singleton
  - _Requirements: 5.1_

- [ ] 7. Create NetDepthButtonHighlighter overlay component
  - Create NetDepthButtonHighlighter class extending Overlay and implementing PluginLifecycleComponent
  - Inject ShoalDepthTracker, BoatTracker, Client, and SailingConfig
  - Set overlay position to DYNAMIC and layer to ABOVE_WIDGETS
  - Implement isEnabled() to check config.trawlingShowNetDepthTimer()
  - Implement startUp() and shutDown() lifecycle methods
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2_

- [ ] 8. Implement button highlighting logic in NetDepthButtonHighlighter
  - Implement render() method to draw button highlights
  - Add shouldHighlightButtons() to check if highlighting is needed
  - Query ShoalDepthTracker for current depth and movement direction
  - Query BoatTracker for player's current net depths
  - Implement highlightButtonsForDepth() to determine which buttons to highlight
  - Handle three-depth area special case (no highlight at moderate with unknown direction)
  - Only highlight if widget opacity is 0 (player can interact)
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.4_

- [ ] 9. Add button selection and rendering helpers to NetDepthButtonHighlighter
  - Implement highlightNetButton() to render highlight on specific button
  - Implement getNetDepth() to read current net depth from widget sprites
  - Implement getNetWidget() to safely access widget children
  - Add widget viewport visibility checking
  - Use configured color from settings for highlights
  - _Requirements: 3.1, 3.2, 3.4, 3.5_

- [ ] 9.1 Write property test for three-depth highlighting
  - **Property 10: Three-depth areas highlight toward moderate**
  - **Validates: Requirements 3.1, 3.2**

- [ ] 9.2 Write property test for matching depth disables highlighting
  - **Property 15: Matching depth disables highlighting**
  - **Validates: Requirements 6.4**

- [ ] 10. Register NetDepthButtonHighlighter in SailingModule
  - Add NetDepthButtonHighlighter to lifecycleComponents() provider method
  - Ensure it's registered as a singleton
  - _Requirements: 6.1_

- [ ] 11. Update ShoalOverlay to use ShoalDepthTracker
  - Inject ShoalDepthTracker into ShoalOverlay constructor
  - Inject BoatTracker to get player's net depths
  - Update getShoalColor() to implement color priority system
  - Priority 1: Red for depth mismatch (highest)
  - Priority 2: Green for special shoals (VIBRANT, GLISTENING, SHIMMERING)
  - Priority 3: Configured color for normal shoals with matching depth
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 12. Add helper method to ShoalOverlay for player net depth
  - Implement getPlayerNetDepth() to query BoatTracker
  - Handle cases where player has no nets equipped
  - Return null if nets are not available
  - _Requirements: 1.1, 1.2_

- [ ] 12.1 Write property test for depth mismatch red highlight
  - **Property 1: Depth mismatch shows red highlight**
  - **Validates: Requirements 1.1**

- [ ] 12.2 Write property test for depth match configured color
  - **Property 2: Depth match shows configured color for normal shoals**
  - **Validates: Requirements 1.2**

- [ ] 12.3 Write property test for special shoals green color
  - **Property 3: Special shoals use green when depth matches**
  - **Validates: Requirements 1.2**

- [ ] 12.4 Write property test for depth change color update
  - **Property 4: Depth change updates color within one tick**
  - **Validates: Requirements 1.3**

- [ ] 13. Refactor NetDepthTimer to use ShoalDepthTracker
  - Inject ShoalDepthTracker into NetDepthTimer
  - Remove button highlighting code (moved to NetDepthButtonHighlighter)
  - Keep timing logic and shoal movement tracking
  - Call shoalDepthTracker.notifyDepthChange() when depth transitions occur
  - Simplify render() to only handle timer overlay display (if needed)
  - _Requirements: 2.2, 4.3_

- [ ] 14. Update TrawlingData with fishing area metadata
  - Add methods to determine if a location is in a three-depth area
  - Add getStopDurationForLocation() if not already present
  - Ensure all fishing areas have correct stop duration values
  - _Requirements: 2.1, 2.5, 3.1, 3.2_

- [ ] 15. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
