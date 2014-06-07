package org.greencheek.spray.cache.memcached.perftests

import net.spy.memcached.util.StringUtils;
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit


@State(Scope.Thread)
class KeyBench {

  /**
   * 12 chars long
   */
  val  SHORT_KEY : String = "user:michael";

  /**
   * 240 chars long
   */
  val LONG_KEY : String = "thisIsAFunkyKeyWith_underscores_AndAlso334" +
    "3252545345NumberslthisIsAFunkyKeyWith_underscores_AndAlso3343252545345Numbe" +
    "rslthisIsAFunkyKeyWith_underscores_AndAlso3343252545345NumberslthisIsAFunkyK" +
    "eyWith_underscores_AndAlso3343252545345Numbersl";

  @GenerateMicroBenchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def validateShortKeyBinary() : Unit = {
    StringUtils.validateKey(SHORT_KEY, true);
  }

  @GenerateMicroBenchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def validateShortKeyAscii() : Unit = {
    StringUtils.validateKey(SHORT_KEY, false);
  }

  @GenerateMicroBenchmark
  @BenchmarkMode(Array(Mode.Throughput))
  def validateLongKeyBinary() : Unit = {
    StringUtils.validateKey(LONG_KEY, true);
  }

  @GenerateMicroBenchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @OutputTimeUnit(TimeUnit.SECONDS)
  def  validateLongKeyAscii() : Unit = {
    StringUtils.validateKey(LONG_KEY, false);
  }
}
