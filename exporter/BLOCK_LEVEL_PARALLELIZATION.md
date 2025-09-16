# Block-Level Parallelization (Phase 3.1)

This document describes the implementation of Phase 3.1 Block-Level Parallelization optimization for the TON4J Exporter.

## Overview

Phase 3.1 introduces block-level parallelization to improve the performance of TON blockchain data export by:

1. **Splitting large packages into block-level work units** - Instead of processing entire packages sequentially, individual blocks are extracted and processed in parallel
2. **Using work-stealing queues for load balancing** - Implements efficient work distribution across multiple threads using Java's ForkJoinPool
3. **Parallel BOC parsing within packages** - BOC (Bag of Cells) parsing and TLB deserialization operations are parallelized for maximum CPU utilization

## Architecture

### Core Components

#### 1. BlockLevelParallelProcessor
- **Location**: `org.ton.ton4j.exporter.BlockLevelParallelProcessor`
- **Purpose**: Manages block-level parallel processing across archive packages
- **Key Features**:
  - Work-stealing queue implementation using ForkJoinPool
  - Block work unit abstraction for fine-grained parallelization
  - Graceful shutdown handling
  - Support for both single archive and multi-archive parallel processing

#### 2. ParallelBocParser
- **Location**: `org.ton.ton4j.exporter.ParallelBocParser`
- **Purpose**: Handles CPU-intensive BOC parsing and TLB deserialization in parallel
- **Key Features**:
  - Configurable parsing modes (blocks vs. any cell)
  - Asynchronous and batch processing capabilities
  - Comprehensive performance statistics tracking
  - Thread-safe operations with dedicated thread pool

#### 3. Enhanced Exporter Integration
- **Location**: `org.ton.ton4j.exporter.Exporter`
- **Purpose**: Integrates block-level parallelization into the existing export workflow
- **Key Features**:
  - Configuration options to enable/disable block-level parallelization
  - Backward compatibility with existing package-level parallelization
  - Performance statistics access and management

## Implementation Details

### Work Unit Structure

```java
public static class BlockWorkUnit {
    private final String blockKey;      // Unique block identifier
    private final byte[] blockData;     // Raw block data (BOC format)
    private final String archiveKey;    // Source archive identifier
}
```

### Processing Flow

1. **Archive Scanning**: Archives are discovered and queued for processing
2. **Block Extraction**: Each archive is processed to extract individual blocks into work units
3. **Work Distribution**: Work units are distributed across worker threads using work-stealing queues
4. **Parallel Processing**: Multiple threads process blocks concurrently:
   - BOC parsing (bytes → Cell)
   - Magic number validation
   - TLB deserialization (Cell → Block object)
   - JSON serialization (if required)
   - Output writing
5. **Result Aggregation**: Results are collected and statistics updated

### Configuration Options

```java
// Enable/disable block-level parallelization
exporter.setBlockLevelParallelizationEnabled(true);

// Check current status
boolean enabled = exporter.isBlockLevelParallelizationEnabled();

// Access performance statistics
ParallelBocParser.ParsingStatistics stats = exporter.getBocParsingStatistics();
```

### Performance Statistics

The implementation provides detailed performance metrics:

```java
public class ParsingStatistics {
    long getTotalParsedBlocks();           // Successfully parsed blocks
    long getTotalParseErrors();            // Failed parsing attempts
    long getTotalBocParseTimeNanos();      // Time spent on BOC parsing
    long getTotalTlbDeserializeTimeNanos(); // Time spent on TLB deserialization
    double getAverageBocParseTimeMs();     // Average BOC parse time
    double getAverageTlbDeserializeTimeMs(); // Average TLB deserialize time
    double getSuccessRate();               // Success rate percentage
}
```

## Usage Examples

### Basic Usage

```java
// Create exporter with block-level parallelization enabled (default)
Exporter exporter = Exporter.builder()
    .tonDatabaseRootPath("/path/to/ton/db")
    .showProgress(true)
    .build();

// Export with 8 parallel threads
exporter.exportToFile("output.jsonl", true, 8);
```

### Advanced Configuration

```java
// Disable block-level parallelization for comparison
exporter.setBlockLevelParallelizationEnabled(false);

// Export and monitor performance
exporter.exportToFile("output.jsonl", true, 8);

// Get performance statistics
ParallelBocParser.ParsingStatistics stats = exporter.getBocParsingStatistics();
System.out.println("Success rate: " + stats.getSuccessRate() + "%");
System.out.println("Average BOC parse time: " + stats.getAverageBocParseTimeMs() + "ms");
```

## Performance Benefits

### Expected Improvements

1. **CPU Utilization**: Better utilization of multi-core systems through fine-grained parallelization
2. **Load Balancing**: Work-stealing queues automatically balance load across threads
3. **Scalability**: Performance scales with the number of available CPU cores
4. **Memory Efficiency**: Streaming approach reduces memory footprint compared to loading entire packages

### Benchmarking

The implementation includes comprehensive test coverage in `TestBlockLevelParallelization.java`:

- Component initialization and configuration tests
- BOC parsing accuracy and performance tests
- Concurrent processing and thread safety tests
- Statistics tracking and reporting tests
- Integration tests with the main Exporter class

## Thread Safety

All components are designed to be thread-safe:

- **AtomicLong** counters for statistics tracking
- **ConcurrentHashMap** for shared data structures where needed
- **ThreadLocal** Gson instances to avoid contention
- **Synchronized** blocks only where absolutely necessary
- **Lock-free** algorithms where possible

## Error Handling

The implementation includes robust error handling:

- Individual block parsing failures don't affect other blocks
- Graceful degradation when components fail to initialize
- Comprehensive logging for debugging and monitoring
- Timeout handling for async operations
- Resource cleanup on shutdown

## Backward Compatibility

Phase 3.1 maintains full backward compatibility:

- Existing export methods continue to work unchanged
- Block-level parallelization can be disabled if needed
- All existing configuration options remain functional
- Performance characteristics gracefully degrade to previous behavior when disabled

## Testing

Run the test suite to verify the implementation:

```bash
mvn test -Dtest=TestBlockLevelParallelization
```

The test suite covers:
- Component initialization and shutdown
- BOC parsing with valid and invalid data
- Asynchronous and batch processing
- Concurrent access and thread safety
- Performance statistics accuracy
- Integration with the main Exporter class

## Future Enhancements

Potential areas for future optimization:

1. **Adaptive Thread Pool Sizing**: Dynamically adjust thread count based on system load
2. **Memory-Mapped File Access**: Use memory-mapped files for even faster I/O
3. **NUMA Awareness**: Optimize for Non-Uniform Memory Access architectures
4. **GPU Acceleration**: Offload certain parsing operations to GPU
5. **Compression**: Implement on-the-fly compression for output data

## Troubleshooting

### Common Issues

1. **High Memory Usage**: Reduce thread count or enable streaming mode
2. **Poor Performance**: Check CPU core count and adjust thread pool size
3. **Parsing Errors**: Verify BOC data integrity and format
4. **Deadlocks**: Review custom BlockProcessor implementations for thread safety

### Debug Options

Enable detailed logging:
```java
// Set log level to DEBUG for detailed information
Logger logger = (Logger) LoggerFactory.getLogger(BlockLevelParallelProcessor.class);
logger.setLevel(Level.DEBUG);
```

### Performance Monitoring

Monitor key metrics:
- Thread pool utilization
- Queue sizes and wait times
- Memory usage patterns
- I/O throughput
- Error rates and types

## Conclusion

Phase 3.1 Block-Level Parallelization represents a significant performance improvement for TON blockchain data export operations. By implementing fine-grained parallelization with work-stealing queues and parallel BOC parsing, the system can efficiently utilize modern multi-core hardware while maintaining data integrity and backward compatibility.

The implementation provides comprehensive monitoring, robust error handling, and extensive test coverage to ensure reliable operation in production environments.
