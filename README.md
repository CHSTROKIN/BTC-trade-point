# Trade point getter from coinbase

## Overview
This is a high-performance, recoverable data fetcher designed for monitoring multiple products in the market simultaneously.

## Key Features
1. Multi-threaded raw producer for concurrent market monitoring
2. Data insertion into SQLite3 for persistent storage
3. RabbitMQ integration for scalability and maintainability, supporting computer cluster
4. Ordered queues in RabbitMQ to guarantee sequential insertion
5. Fixed thread pool management for producer and consumer threads
6. User-friendly GUI interface
7. Fault-tolerant design with RabbitMQ for recovery from unexpected shutdowns
8. Comprehensive test suite, including correctness and stress tests

## Installation

### Prerequisites
- RabbitMQ
- Java Runtime Environment
- Sqlite3 is necessary

### Steps
1. Install RabbitMQ on your device
  - Follow the [official RabbitMQ installation guide](https://www.rabbitmq.com/docs/install-debian)
  - Also read the RABBITMQ TUTORIAL under Document folder

2. Start RabbitMQ server
   ```
   systemctl start rabbitmq-server
   ```

3. Ensure port 5672 is open for RabbitMQ communication

4. Install sqllite3

5. Run the SqlTableCreate, if the file.db is not under Databse's Folder

7. Run the Java program

## External Libraries

| Library              | Source                                                                             | License            |
|----------------------|------------------------------------------------------------------------------------|--------------------|
| JSON library         | [GitHub - JSON-java](https://github.com/stleary/JSON-java)                         | MIT                |
| RabbitMQ Java Client | [RabbitMQ Client Libraries](https://www.rabbitmq.com/client-libraries/java-client) | Apache License 2.0 |
| SQLite JDBC          | [Maven Repository](https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc)      | Apache License 2.0 |
| Jfree  Chart         | [Maven](https://mvnrepository.com/artifact/jfree/jfreechart/1.0.13)                 | LGPL               |

## Architecture
- Multi-threaded raw data producers
- RabbitMQ for message queuing and inter-process communication
- SQLite3 for persistent data storage
- Fixed thread pool for efficient thread management

## Fault Tolerance
The system is designed to recover from unexpected shutdowns, resuming operations from the last known state using RabbitMQ's message persistence.

## Testing
The project includes a comprehensive test suite covering:
- Correctness tests to ensure accurate functionality
- Stress tests to verify performance under heavy loads
## Code format
* google java format: https://github.com/google/google-java-format
