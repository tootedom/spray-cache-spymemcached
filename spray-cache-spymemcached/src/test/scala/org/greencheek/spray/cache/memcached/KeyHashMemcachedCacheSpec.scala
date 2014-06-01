package org.greencheek.spray.cache.memcached

import scala.concurrent.{ExecutionContext, Future}
import spray.util._
import org.greencheek.util.memcached.{MemcachedBasedSpec, WithMemcached}
import java.util.Random
import org.specs2.matcher.Matcher
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent.duration.Duration
import org.greencheek.spray.cache.memcached.keyhashing._
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import akka.actor.ActorSystem
import ExecutionContext.Implicits.global
import org.greencheek.util.memcached.WithMemcached
import net.spy.memcached.FailureMode

/**
 * Created by dominictootell on 07/04/2014.
 */
@RunWith(classOf[JUnitRunner])
class KeyHashMemcachedCacheSpec extends MemcachedBasedSpec {

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)

  "A Memcached cache" >> {
    "be thread-safe sha256 hash key" in memcachedContext {
      val cache = new MemcachedCache[Int](Duration.Zero, 10000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = SHA256KeyHash)

      // exercise the cache from 10 parallel "tracks" (threads)
      val views = Future.traverse(Seq.tabulate(10)(identityFunc)) {
        track =>
          Future {
            val array = Array.fill(1000)(0) // our view of the cache
            val rand = new Random(track)
            (1 to 10000) foreach {
              i =>
                val ix = rand.nextInt(1000) // for a random index into the cache
              val value = cache(ix) {
                  // get (and maybe set) the cache value
                  Thread.sleep(0)
                  rand.nextInt(1000000) + 1
                }.await
                if (array(ix) == 0) array(ix) = value // update our view of the cache
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

    "sha256 can have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = SHA256KeyHash)

      cache("1 space 1")("A").await === "A"
      cache("2 space 2")("B").await === "B"

      cache.get("1 space 1").get.await === "A"

    }
    "jenkins hash can have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = JenkinsHash)

      cache("1 space 1")("A").await === "A"
      cache("2 space 2")("B").await === "B"

      cache.get("1 space 1").get.await === "A"

    }
    "java xxhash can have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = XXJavaHash)

      cache("1 space 1")("A").await === "A"
      cache("2 space 2")("B").await === "B"

      cache.get("1 space 1").get.await === "A"

    }
    "native xxhash can have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = XXNativeJavaHash)

      cache("1 space 1")("A").await === "A"
      cache("2 space 2")("B").await === "B"

      cache.get("1 space 1").get.await === "A"

    }
    "md5 can have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = MD5KeyHash)

      cache("1 space 1")("A").await === "A"
      cache("2 space 2")("B").await === "B"

      cache.get("1 space 1").get.await === "A"
    }
    "key with no hashing cannot have a key with a linefeed/return in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = NoKeyHash, failureMode = FailureMode.Cancel)

      cache("1space1\r\n2to2")("A").await === "A"
      cache("1space1\r\n2to2")("B").await === "B"

      cache.get("1space1\r\n2to2") must beNone
    }
    "key with no hashing cannot have a key with a space in it" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = null)

      cache("1 space 1")("A").await === "A"
      cache("1 space 1")("B").await === "B"

      cache.get("1 space 1") must beNone
    }
    "key with no hashing can have a key with a space in it for binary client" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.BINARY,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = null)

      cache("1 space 1")("A").await === "A"
      cache("1 space 1")("B").await === "A"

      cache.get("1 space 1") must beSome
    }
    "key with no hashing can have a key with a space and linefeed in it for binary client" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 1000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.BINARY,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = null)

      cache("1 space 1\r\n2 to 2")("A").await === "A"
      cache("1 space 1\r\n2 to 2")("B").await === "A"

      cache.get("1 space 1\r\n2 to 2") must beSome
    }
  }
}
