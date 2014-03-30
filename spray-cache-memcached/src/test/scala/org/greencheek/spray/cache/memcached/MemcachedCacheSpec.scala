package org.greencheek.spray.cache.memcached


import java.util.Random
import java.util.concurrent.CountDownLatch
import akka.actor.ActorSystem
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import org.specs2.matcher.Matcher
import spray.util._

import scala.concurrent._
import ExecutionContext.Implicits.global
import org.greencheek.util.memcached.{WithMemcached}
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
abstract class MemcachedCacheSpec extends Specification {
  implicit val system = ActorSystem()

  def getMemcacheContext() : WithMemcached

  val memcachedContext = getMemcacheContext();


  "A Memcached cache" >> {
    "store uncached values" in memcachedContext {
      val cache = memcachedCache[String]("localhost:"+memcachedContext.memcached.port,binary = memcachedContext.binary)

      cache("1")("A").await == "A"
      cache("2")("B").await == "B"
      cache("3")("B").await == "B"
      cache("4")("B").await == "B"
      cache("5")("B").await == "F"


      cache.get("1").get.await == "A"
      cache.get("5").get.await == "F"
      cache.get("9") == None

      true
    }
    "store more than max capacity" in memcachedContext {
      val cache = memcachedCache[String]("localhost:"+memcachedContext.memcached.port,1,binary = memcachedContext.binary)

      val option1 = cache("15")( future {
        try {
          Thread.sleep(1000)
        } catch {
          case e: Exception => {

          }
        }
        "hello"
      })

      val option2 = cache("25")( future {
        try {
          Thread.sleep(1000)
        } catch {
          case e: Exception => {

          }
        }
        "hello2"
      })


      option1.await == "hello"
      option2.await == "hello2"


      cache.get("15").get.await == "hello"
      cache.get("25").get.await == "hello2"

      memcachedContext.memcached.daemon.get.getCache.getCurrentItems == 2

      true
    }
//    "return stored values upon cache hit on existing values" in {
//      val cache = lruCache[String]()
//      cache(1)("A").await === "A"
//      cache(1)("").await === "A"
//      cache.store.toString === "{1=A}"
//    }
//    "return Futures on uncached values during evaluation and replace these with the value afterwards" in {
//      val cache = lruCache[String]()
//      val latch = new CountDownLatch(1)
//      val future1 = cache(1) { promise =>
//        Future {
//          latch.await()
//          promise.success("A")
//        }
//      }
//      val future2 = cache(1)("")
//      cache.store.toString === "{1=pending}"
//      latch.countDown()
//      future1.await === "A"
//      future2.await === "A"
//      cache.store.toString === "{1=A}"
//    }
//    "properly limit capacity" in {
//      val cache = lruCache[String](maxCapacity = 3)
//      cache(1)("A").await === "A"
//      cache(2)("B").await === "B"
//      cache(3)("C").await === "C"
//      cache.store.toString === "{2=B, 1=A, 3=C}"
//      cache(4)("D")
//      Thread.sleep(10)
//      cache.store.toString === "{2=B, 3=C, 4=D}"
//    }
//    "expire old entries" in {
//      val cache = lruCache[String](timeToLive = 75 millis span)
//      cache(1)("A").await === "A"
//      cache(2)("B").await === "B"
//      Thread.sleep(50)
//      cache(3)("C").await === "C"
//      cache.store.size === 3
//      Thread.sleep(50)
//      cache.get(2) must beNone // removed on request
//      cache.store.size === 2 // expired entry 1 still there
//      cache.get(1) must beNone // but not retrievable anymore
//    }
//    "not cache exceptions" in {
//      val cache = lruCache[String]()
//      cache(1)((throw new RuntimeException("Naa")): String).await must throwA[RuntimeException]("Naa")
//      cache(1)("A").await === "A"
//    }
//    "refresh an entries expiration time on cache hit" in {
//      val cache = lruCache[String]()
//      cache(1)("A").await === "A"
//      cache(2)("B").await === "B"
//      cache(3)("C").await === "C"
//      cache(1)("").await === "A" // refresh
//      cache.store.toString === "{2=B, 1=A, 3=C}"
//    }
//    "be thread-safe" in {
//      val cache = lruCache[Int](maxCapacity = 1000)
//      // exercise the cache from 10 parallel "tracks" (threads)
//      val views = Future.traverse(Seq.tabulate(10)(identityFunc)) { track =>
//        Future {
//          val array = Array.fill(1000)(0) // our view of the cache
//          val rand = new Random(track)
//          (1 to 10000) foreach { i =>
//            val ix = rand.nextInt(1000)            // for a random index into the cache
//          val value = cache(ix) {                // get (and maybe set) the cache value
//              Thread.sleep(0)
//              rand.nextInt(1000000) + 1
//            }.await
//            if (array(ix) == 0) array(ix) = value  // update our view of the cache
//            else if (array(ix) != value) failure("Cache view is inconsistent (track " + track + ", iteration " + i +
//              ", index " + ix + ": expected " + array(ix) + " but is " + value)
//          }
//          array
//        }
//      }.await
//      val beConsistent: Matcher[Seq[Int]] = (
//        (ints: Seq[Int]) => ints.filter(_ != 0).reduceLeft((a, b) => if (a == b) a else 0) != 0,
//        (_: Seq[Int]) => "consistency check"
//        )
//      views.transpose must beConsistent.forall
//    }
  }

  step(system.shutdown())

  def memcachedCache[T](hosts: String, maxCapacity: Int = 500, initialCapacity: Int = 16,
                        timeToLive: Duration = Duration.Zero, timeToIdle: Duration = Duration.Zero,
                        binary : Boolean = true) = {
    binary match {
      case true => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.BINARY)
      case false => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.TEXT)
    }
  }

}
