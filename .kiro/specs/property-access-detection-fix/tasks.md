# Implementation Plan

- [x] 1. Create MethodCallToPropertyAccessConverter component









  - Implement pattern matching for Kotlin setter/getter method calls
  - Add support for Java-style getter/setter method name conversion
  - Include robust error handling for invalid method names
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 2. Fix MethodAnalyzer property access extraction logic
  - Modify the analyzeMethod() function to properly convert method calls to property accesses
  - Ensure MethodCallToPropertyAccessConverter is called with detected method calls
  - Fix the logic that adds converted property accesses to the final result list
  - _Requirements: 3.1, 3.2, 5.3_

- [ ] 3. Enhance PropertyAccessCollector for direct IR analysis
  - Improve visitCall() method to better detect property access through method calls
  - Enhance visitGetValue() and visitSetValue() for direct property access detection
  - Add support for conditional property access in visitWhen() method
  - _Requirements: 1.1, 1.3, 2.1, 2.3, 4.2_

- [ ] 4. Implement comprehensive property access pattern detection
  - Add detection for comparison operations (status == OrderStatus.PENDING)
  - Add detection for assignment operations (status = OrderStatus.CONFIRMED)
  - Add detection for method chain property access (items.isNotEmpty())
  - _Requirements: 1.1, 2.1, 4.1, 4.2_

- [ ] 5. Add property access deduplication and validation
  - Implement deduplication logic based on propertyName, accessType, and ownerClass
  - Add validation to ensure PropertyAccessMetadata objects are correctly formed
  - Ensure no duplicate property accesses in the final results
  - _Requirements: 4.3, 5.3_

- [ ] 6. Enhance error handling for property access detection
  - Add try-catch blocks around IR analysis operations
  - Implement graceful degradation when property access detection fails
  - Add logging for debugging property access detection issues
  - _Requirements: 5.4_

- [ ] 7. Create unit tests for MethodCallToPropertyAccessConverter
  - Test Kotlin setter pattern conversion (<set-propertyName> → SET access)
  - Test Kotlin getter pattern conversion (<get-propertyName> → GET access)
  - Test Java-style getter/setter method name conversion
  - Test error handling for invalid method names
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 8. Create unit tests for enhanced PropertyAccessCollector
  - Test direct field access detection (IrGetField, IrSetField)
  - Test property access detection in conditional expressions
  - Test property access detection in method chains
  - Test error handling and edge cases
  - _Requirements: 1.1, 1.3, 2.1, 2.3, 4.1, 4.2_

- [ ] 9. Create integration tests for Order class property access
  - Test confirm() method property access detection (status read/write, items read)
  - Test simpleTest() method property access detection
  - Test other Order class methods for comprehensive property access detection
  - Verify that propertyAccesses array is no longer empty
  - _Requirements: 1.1, 1.4, 2.1, 2.2, 5.1, 5.2, 5.3_

- [ ] 10. Validate and fix existing test failures
  - Fix PropertyAccessType enum test failures (READ vs GET, WRITE vs SET)
  - Update test expectations to match the correct GET/SET enum values
  - Ensure all existing tests pass with the enhanced property access detection
  - _Requirements: 5.1, 5.2_

- [ ] 11. Add comprehensive logging and debugging support
  - Add debug logging to track property access detection process
  - Log when method calls are converted to property accesses
  - Log when direct IR property access is detected
  - Add error logging for failed property access detection attempts
  - _Requirements: 5.4_

- [ ] 12. Perform end-to-end testing and validation
  - Compile the demo project and verify property access detection works
  - Check that the generated JSON contains non-empty propertyAccesses arrays
  - Validate that all requirements are met by testing against the Order class
  - Ensure no regression in existing functionality
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4_