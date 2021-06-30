# IgniteTest
 Benchmark tool for examining multi-threaded Ignite transaction-based applications.We use threads to simulate how in a Spring-boot WebService, Tomcat will execute logic on a thread - pulling from a fixed thread pool. This tool only exercises the Distributed Key-Value data structure exposed by Ignite, which implements the
 JSR-107 specification.
 
 JSR-107: https://download.oracle.com/otndocs/jcp/jcache-1_0-fr-spec/index.html

 This tool assumes a partitioned cache with data colocation using Affinity Mapping, but does not
 currently connect to an existing Ignite cluster. We assume that the cost penalities incurred by
 synchronizing data during cache transactions can be realized using our workload argument.

 Our threads will attempt to execute the transaction logic as fast as possible, which
 can help establish some crude limits. We provide additional arguments such as number of keys and
 a "workload" to simulate our threads doing work before, after, and during transactions.

 Workload is just the number of times we will "busy-loop" run a hash function in order to kill time
 We do not want to use Thread.sleep() because that can only guarantee we will pause thread for
 some number of milliseconds, not guarantee we resume execution immediately after. So this is our
 way of simulating application business logic that take cycles before, after, and during transactions.

 Number of keys works in a similar way, but gives us flexibility to ONLY increase the time that
 a given transaction will hold onto a lock. If our workload is light, but number of keys is high
 the threads spend less time doing busy work and more time fighting for the lock on the key.

```
usage: IgniteTest [-h] [-n NUM_KEYS] [-w WORKLOAD] [-lr LIGHT_RUNNABLES]
                  [-hr HEAVY_RUNNABLES] [-t THREADS] [-d SECONDS]
                  [-iso {READ_COMMITTED,REPEATABLE_READ,SERIALIZABLE}]
                  [-conc {OPTIMISTIC,PESSIMISTIC}]

Tool for exploring Ignite Transaction performance

named arguments:
  -h, --help             show this help message and exit
  -n NUM_KEYS, --numKeys NUM_KEYS
                         How many keys to operate  on in single transaction
                         (default: 3)
  -w WORKLOAD, --workLoad WORKLOAD
                         How many iterations  of  hashing  to do (simulated
                         work) (default: 10)
  -lr LIGHT_RUNNABLES, --lightRunnables LIGHT_RUNNABLES
                         How many 'light' runnables to execute (default: 1)
  -hr HEAVY_RUNNABLES, --heavyRunnables HEAVY_RUNNABLES
                         How many 'heavy' runnables to execute (default: 1)
  -t THREADS, --threads THREADS
                         Total thread pool size (default: 32)
  -d SECONDS, --duration SECONDS
                         Duration of test. Autoexits after (default: 60)
  -iso {READ_COMMITTED,REPEATABLE_READ,SERIALIZABLE}, --isolation {READ_COMMITTED,REPEATABLE_READ,SERIALIZABLE}
                         Ignite  Transaction  Isolation  setting  (default:
                         REPEATABLE_READ)
  -conc {OPTIMISTIC,PESSIMISTIC}, --concurrency {OPTIMISTIC,PESSIMISTIC}
                         Ignite Transaction  Concurrency  setting (default:
                         PESSIMISTIC)
 ```
 
 Sample output for `Namespace(LIGHT_RUNNABLES=10, WORKLOAD=0, ISOLATION=REPEATABLE_READ, CONCURRENCY=PESSIMISTIC, THREADS=32, SECONDS=15, HEAVY_RUNNABLES=10, NUM_KEYS=1)`
 ```
 --------------------------------------
Running for 13 seconds.
TPS: 24699.040
Avg Tx Time: 0.777 ms
Tx Commited: 321528
Holding locks for: 20 tx. Total locked keys: 1
Took 3.0E-4 ms to do 0 hashes. (0.000 h/ms)
Atomic Value: 5
--------------------------------------
 ```
