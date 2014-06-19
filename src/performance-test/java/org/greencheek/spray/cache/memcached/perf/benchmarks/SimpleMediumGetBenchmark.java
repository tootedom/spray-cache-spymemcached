package org.greencheek.spray.cache.memcached.perf.benchmarks;

import org.greencheek.spray.cache.memcached.perf.state.*;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.output.results.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 01/06/2014.
 */
public class SimpleMediumGetBenchmark {

    @GenerateMicroBenchmark
    @BenchmarkMode({Mode.SampleTime})
    public MediumCacheObject simpleGet(LargeCacheKey key, MediumCacheObject value,XXJavaMediumTextBenchmarkCache cache) {
        return org.greencheek.spray.cache.memcached.perftests.SimpleGetBenchmarks.testMediumGet(key, value, cache);
    }

    @GenerateMicroBenchmark
    @BenchmarkMode({Mode.SampleTime})
    public MediumCacheObject simpleCompressedGet(LargeCacheKey key, MediumCacheObject value,XXJavaMediumCompressedTextBenchmarkCache cache) {
        return org.greencheek.spray.cache.memcached.perftests.SimpleGetBenchmarks.testMediumCompressedGet(key, value, cache);
    }

    public static void main(String[] args) throws RunnerException {
        Options opts  = new OptionsBuilder()
                .include("org.greencheek.spray.cache.memcached.perf.benchmarks.SimpleMediumGetBenchmark.*")
                .warmupIterations(20)
                .measurementIterations(20)
                .timeUnit(TimeUnit.MILLISECONDS)
                .threads(2)
                .forks(3)
                .jvmArgs(JvmArgs.getJvmArgs())
                .resultFormat(ResultFormatType.TEXT)
                .verbosity(VerboseMode.EXTRA)
                .build();

        new Runner(opts).run();
    }
}
