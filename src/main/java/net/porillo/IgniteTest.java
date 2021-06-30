package net.porillo;

import net.porillo.runnables.HeavyRunnable;
import net.porillo.runnables.LightRunnable;
import net.porillo.runnables.ReportRunnable;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Nick Porillo
 *
 * Benchmark tool for examining multi-threaded Ignite transaction-based applications
 * We use threads to simulate how in a Spring-boot WebService, Tomcat will execute logic
 * on a thread - pulling from a fixed thread pool. This tool only exercises the Distributed Key-Value
 * datastructure exposed by Ignite, which implements the JSR-107 specification.
 * JSR-107: https://download.oracle.com/otndocs/jcp/jcache-1_0-fr-spec/index.html
 *
 * This tool assumes a partitioned cache with data colocation using Affinity Mapping, but does not
 * currently connect to an existing Ignite cluster. We assume that the cost penalities incurred by
 * synchronizing data during cache transactions can be realized using our workload argument.
 *
 * Our threads will attempt to execute the transaction logic as fast as possible, which
 * can help establish some crude limits. We provide additional arguments such as number of keys and
 * a "workload" to simulate our threads doing work before, after, and during transactions.
 *
 * Workload is just the number of times we will "busy-loop" run a hash function in order to kill time
 * We do not want to use Thread.sleep() because that can only guarantee we will pause thread for
 * some number of milliseconds, not guarantee we resume execution immediately after. So this is our
 * way of simulating application business logic that take cycles before, after, and during transactions.
 *
 * Number of keys works in a similar way, but gives us flexibility to ONLY increase the time that
 * a given transaction will hold onto a lock. If our workload is light, but number of keys is high
 * the threads spend less time doing busy work and more time fighting for the lock on the key.
 */
public class IgniteTest {

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("IgniteTest").build()
                .defaultHelp(true)
                .description("Tool for exploring Ignite Transaction performance");
        parser.addArgument("-n", "--numKeys")
                .type(Integer.class).setDefault(3).dest("NUM_KEYS")
                .help("How many keys to operate on in single transaction");
        parser.addArgument("-w", "--workLoad")
                .type(Integer.class).setDefault(10).dest("WORKLOAD")
                .help("How many iterations of hashing to do (simulated work)");
        parser.addArgument("-lr", "--lightRunnables")
                .type(Integer.class).setDefault(1).dest("LIGHT_RUNNABLES")
                .help("How many 'light' runnables to execute");
        parser.addArgument("-hr", "--heavyRunnables")
                .type(Integer.class).setDefault(1).dest("HEAVY_RUNNABLES")
                .help("How many 'heavy' runnables to execute");
        parser.addArgument("-t", "--threads")
                .type(Integer.class).setDefault(32).dest("THREADS")
                .help("Total thread pool size");
        parser.addArgument("-d", "--duration")
                .type(Integer.class).setDefault(60).dest("SECONDS")
                .help("Duration of test. Autoexits after");
        parser.addArgument("-iso", "--isolation")
                .type(TransactionIsolation.class).setDefault(TransactionIsolation.REPEATABLE_READ).dest("ISOLATION")
                .help("Ignite Transaction Isolation setting");
        parser.addArgument("-conc", "--concurrency")
                .type(TransactionConcurrency.class).setDefault(TransactionConcurrency.PESSIMISTIC).dest("CONCURRENCY")
                .help("Ignite Transaction Concurrency setting");

        Namespace res = null;
        try {
            res = parser.parseArgs(args);
            System.out.println(res);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(res.getInt("THREADS"));
        TransactionConcurrency concurrency = res.get("CONCURRENCY");
        TransactionIsolation isolation = res.get("ISOLATION");
        IgniteConfiguration igniteConfiguration = Utility.getIgniteConfig(500, concurrency, isolation);
        Ignite ignite = Ignition.start(igniteConfiguration);

        // Warm up our work method to get these hashes loaded into CPU cache
        for (int i = 0; i < 10; i++) {
            Utility.hashToKillTime((i+1) * 100);
        }

        int numKeys = res.getInt("NUM_KEYS");
        int workLoad = res.getInt("WORKLOAD");
        int duration = res.getInt("SECONDS");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < res.getInt("LIGHT_RUNNABLES"); i++) {
            executor.execute(new LightRunnable(ignite, numKeys, workLoad));
        }

        for (int i = 0; i < res.getInt("HEAVY_RUNNABLES"); i++) {
            executor.execute(new HeavyRunnable(ignite, numKeys, workLoad));
        }

        executor.scheduleAtFixedRate(new ReportRunnable(ignite, startTime, workLoad), 1, 1, TimeUnit.SECONDS);

        executor.schedule(() -> {
            System.out.println("Shutting down all threads. Sleeping then exiting...");
            executor.shutdown();

            try {
                // clean shutdown, wait for all threads to be done executing before we hard exit
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }, duration, TimeUnit.SECONDS);
    }
}
