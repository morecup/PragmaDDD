# Implementation Plan

- [x] 1. Set up Kotlin compiler plugin infrastructure









  - Create compiler plugin registrar and component registrar classes
  - Implement basic IR generation extension framework
  - Configure Gradle plugin to register the Kotlin compiler plugin
  - _Requirements: 1.1, 1.2, 1.3, 6.1, 6.2_

- [x] 2. Implement annotation detection system





  - Create AnnotationDetector interface and implementation
  - Write methods to detect @AggregateRoot, @DomainEntity, and @ValueObj annotations
  - Implement annotation metadata extraction from IR constructor calls
  - Write unit tests for annotation detection functionality
  - _Requirements: 1.1, 1.2, 1.3, 1.5_

- [x] 3. Create core metadata data models





  - Define ClassMetadata, PropertyMetadata, MethodMetadata data classes
  - Implement MethodCallMetadata and PropertyAccessMetadata models
  - Create DocumentationMetadata and AnnotationMetadata structures
  - Define enums for DddAnnotationType and PropertyAccessType
  - Write unit tests for data model serialization
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 4. Implement class structure analysis












  - Create ClassAnalyzer interface and implementation
  - Write methods to extract class properties from IR class declarations
  - Implement method signature extraction from IR functions
  - Add class-level documentation extraction from KDoc
  - Write unit tests for class analysis functionality
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 5. Develop method body analysis engine





  - Create MethodAnalyzer interface and implementation
  - Implement IR visitor pattern to traverse method bodies
  - Write method call detection logic for IR call expressions
  - Implement property access detection for field reads and writes
  - Write unit tests for method analysis with synthetic IR structures
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 6. Implement property access pattern detection





  - Create PropertyAnalyzer interface and implementation
  - Write logic to detect direct field access (GETFIELD/PUTFIELD equivalent in IR)
  - Implement getter/setter method call recognition
  - Add support for property access through method chains
  - Write unit tests for various property access patterns
  - _Requirements: 3.2, 3.3, 3.5_

- [x] 7. Create documentation extraction system





  - Implement KDoc parsing from IR declarations
  - Write methods to extract parameter documentation
  - Add return value documentation extraction
  - Create documentation metadata collection for classes, methods, and properties
  - Write unit tests for documentation extraction
  - _Requirements: 2.4, 2.6_

- [x] 8. Implement metadata collection and aggregation





  - Create MetadataCollector interface and implementation
  - Write methods to aggregate class metadata from IR analysis
  - Implement separation of main and test source metadata
  - Add metadata validation and consistency checking
  - Write unit tests for metadata collection
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 9. Develop JSON generation and serialization





  - Create JsonGenerator interface and implementation using Jackson
  - Implement JSON schema generation for ClassMetadata structures
  - Write methods to generate separate JSON files for main and test sources
  - Add JSON formatting and pretty-printing configuration
  - Write unit tests for JSON generation and schema validation
  - _Requirements: 4.1, 4.3, 5.1, 5.2_

- [x] 10. Implement resource packaging system





  - Create ResourceWriter interface and implementation
  - Write methods to generate JSON files in META-INF directory
  - Implement automatic resource directory creation
  - Add support for custom output directory configuration
  - Write unit tests for resource file generation
  - _Requirements: 4.1, 4.2, 4.4, 7.1, 7.4_

- [x] 11. Enhance Gradle plugin configuration









  - Update PragmaDddAnalyzerExtension with new configuration options
  - Add properties for JSON file naming, output directory, and analysis features
  - Implement configuration validation in plugin application
  - Write methods to pass configuration to compiler plugin
  - Write unit tests for plugin configuration
  - _Requirements: 6.3, 6.4, 7.1, 7.2, 7.3, 7.5_

- [x] 12. Integrate compiler plugin with Gradle build lifecycle





  - Update PragmaDddAnalyzerPlugin to register compiler plugin properly
  - Configure compiler plugin to execute during Kotlin compilation phase
  - Implement proper task dependencies for JSON generation
  - Add support for incremental compilation compatibility
  - Write integration tests for Gradle plugin functionality
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 13. Implement test source set support













  - Modify compiler plugin to detect test vs main source compilation
  - Create separate metadata collection for test sources
  - Implement test-specific JSON file generation
  - Add test JAR packaging support for test metadata
  - Write integration tests for test source analysis
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 14. Add comprehensive error handling and reporting









  - Create AnalysisError sealed class hierarchy
  - Implement ErrorReporter interface for warning and error reporting
  - Add graceful degradation for failed class analysis
  - Implement compilation continuation on analysis errors
  - Write unit tests for error handling scenarios
  - _Requirements: 4.5, 6.5_

- [x] 15. Create end-to-end integration tests




  - Write integration tests using demo project DDD classes
  - Test complete compilation and JSON generation workflow
  - Verify JSON files are correctly packaged in JAR resources
  - Test multi-module project support
  - Create performance tests for compilation time impact
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 4.1, 4.2, 5.1, 5.2_

- [x] 16. Optimize and finalize implementation





  - Profile and optimize IR analysis performance
  - Implement memory usage optimizations for large projects
  - Add configuration options for disabling specific analysis features
  - Create comprehensive documentation and usage examples
  - Perform final integration testing with demo project
  - _Requirements: 7.3, 7.5_


- [x] 18. 去掉pragma-ddd-analyzer插件中 生成test.json的逻辑，只保留生成main.json的逻辑，现在是PragmaDddAnalyzerPlugin里还有isTestCompilation的逻辑，test里也还有






- [x] 19. pragma-ddd-analyzer main resource 下没有生成json