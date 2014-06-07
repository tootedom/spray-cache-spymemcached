package org.greencheek.spray.cache.memcached.performancetest.runner

import  org.openjdk.jmh.runner.options._
import org.openjdk.jmh.output.results.ResultFormatType
import org.openjdk.jmh.runner.Runner
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Mode
import org.greencheek.spray.cache.memcached.perf.benchmarks.JvmArgs

/**
 * Created by dominictootell on 31/05/2014.
 */
object BenchmarkRunner {

  def main(args: Array[String]) {

    val opts : Options = new OptionsBuilder()
      .include("org.greencheek.spray.cache.memcached.perf.benchmarks.*")
      .warmupIterations(20)
      .measurementIterations(20)
      .timeUnit(TimeUnit.MILLISECONDS)
      .jvmArgs(JvmArgs.getJvmArgs)
      .forks(3)
      .threads(2)
      .resultFormat(ResultFormatType.TEXT)
      .verbosity(VerboseMode.EXTRA)
      .build()

    new Runner(opts).run()
//
//    Map<BenchmarkRecord,RunResult> records = new Runner(opts).run();
//    for (Map.Entry<BenchmarkRecord, RunResult> result : records.entrySet()) {
//      Result r = result.getValue().getPrimaryResult();
//      System.out.println("API replied benchmark score: "
//        + r.getScore() + " "
//        + r.getScoreUnit() + " over "
//        + r.getStatistics().getN() + " iterations");
//    }
  }
}
