# Nova prospect project
## How to use it
* 1: Install rabbit mq in your device
  * https://www.rabbitmq.com/docs/install-debian
* 2: Run rabbit mq 
  * systemctl start rabbitmq-server
* 3: Make sure you open port 5672
* 4: Run the java program
## What is it
* It is a **High performanced** and **Recoverable** data fetcher 
## Core feature:
* 1: Multithread Raw producer that can monitor multiple product on the market at the same time
* 2: Multithread Raw producer insert the data cunrrently into sqllite3 achieve peristance data storeage
* 3: Leverage rabbit mq, making the program strechable and maintainable, it can forms computer groups.
* 4: Ordered queue in rabbit mq gurantees the squential insertion order
* 5: Using fixed thread pool to manage the producer thread, and consumer thread
* 6: Cool GUI interface
* 7: Recoverable, using rabbitmq to achieve recoverable operation, even rabbit mq is shutdown accidently, it can recover from the previous position
* 8: Clear and full test case, including correctness test and pressure test
## External library
* json library: https://github.com/stleary/JSON-java
  * lience: MIT
* Rabbit mq: https://www.rabbitmq.com/client-libraries/java-client
  * lience: Apache Public Lience 2.0
* sqllite3: https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
  * lience: Apache 2.0
