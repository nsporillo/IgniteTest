# IgniteTest
 Benchmark tool for examining multi-threaded Ignite transaction-based applications
 We use threads to simulate how in a Spring-boot WebService, Tomcat will execute logic
 on a thread - pulling from a fixed thread pool. This tool only exercises the Distributed Key-Value
 datastructure exposed by Ignite, which implements the JSR-107 specification.
 JSR-107: https://download.oracle.com/otndocs/jcp/jcache-1_0-fr-spec/index.html
  *
 This tool assumes a partitioned cache with data colocation using Affinity Mapping, but does not
 currently connect to an existing Ignite cluster. We assume that the cost penalities incurred by
 synchronizing data during cache transactions can be realized using our workload argument.
  *
 Our threads will attempt to execute the transaction logic as fast as possible, which
 can help establish some crude limits. We provide additional arguments such as number of keys and
 a "workload" to simulate our threads doing work before, after, and during transactions.
  *
 Workload is just the number of times we will "busy-loop" run a hash function in order to kill time
 We do not want to use Thread.sleep() because that can only guarantee we will pause thread for
 some number of milliseconds, not guarantee we resume execution immediately after. So this is our
 way of simulating application business logic that take cycles before, after, and during transactions.
  *
 Number of keys works in a similar way, but gives us flexibility to ONLY increase the time that
 a given transaction will hold onto a lock. If our workload is light, but number of keys is high
 the threads spend less time doing busy work and more time fighting for the lock on the key.
