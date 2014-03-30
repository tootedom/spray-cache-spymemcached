package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.WithMemcached
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import scala.concurrent.Future
import spray.util._
import org.greencheek.util.memcached.WithMemcached
import java.util.Random
import org.specs2.matcher.Matcher

import scala.concurrent._
import ExecutionContext.Implicits.global
/**
 * Created by dominictootell on 30/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class AsciiMemcachedCacheSpec extends MemcachedCacheSpec{
  override def getMemcacheContext(): WithMemcached = WithMemcached(false)

  "be thread-safe" in memcachedContext {
    val cache = memcachedCache[Int]("localhost:"+memcachedContext.memcached.port, maxCapacity = 1000,binary = memcachedContext.binary,
      waitForMemcachedSet = true)

    // exercise the cache from 10 parallel "tracks" (threads)
    val views = Future.traverse(Seq.tabulate(10)(identityFunc)) { track =>
      Future {
        val array = Array.fill(1000)(0) // our view of the cache
        val rand = new Random(track)
        (1 to 10000) foreach { i =>
          val ix = rand.nextInt(1000)            // for a random index into the cache
        val value = cache(ix) {                // get (and maybe set) the cache value
            Thread.sleep(0)
            rand.nextInt(1000000) + 1
          }.await
          if (array(ix) == 0) array(ix) = value  // update our view of the cache
          else if (array(ix) != value) failure("Cache view is inconsistent (track " + track + ", iteration " + i +
            ", index " + ix + ": expected " + array(ix) + " but is " + value)
        }
        array
      }
    }.await
    val beConsistent: Matcher[Seq[Int]] = (
      (ints: Seq[Int]) => ints.filter(_ != 0).reduceLeft((a, b) => if (a == b) a else 0) != 0,
      (_: Seq[Int]) => "consistency check"
      )
    views.transpose must beConsistent.forall

    true
  }
}