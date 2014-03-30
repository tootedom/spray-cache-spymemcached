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
    "add keys with toString methods" in memcachedContext {
      val cache = memcachedCache[String]("localhost:"+memcachedContext.memcached.port,1,binary = memcachedContext.binary)

      cache(1)("A").await == "A"
      cache(2)("B").await == "B"
      cache(3)("B").await == "B"
      cache(4)("B").await == "B"
      cache(5)("B").await == "F"


      cache.get(1).get.await == "A"
      cache.get(5).get.await == "F"
      cache.get(9) == None

      memcachedContext.memcached.daemon.get.getCache.getCurrentItems == 5

      true
    }
    "return stored values upon cache hit on existing values" in memcachedContext {
      val cache = memcachedCache[String]("localhost:"+memcachedContext.memcached.port,binary = false,
      waitForMemcachedSet = true)
      cache(1000)("A").await === "A"
      cache(1000)("").await === "A"
    }
    "return Futures on uncached values during evaluation and replace these with the value afterwards" in memcachedContext{
      val cache = memcachedCache[String]("localhost:"+memcachedContext.memcached.port,binary = false,
        waitForMemcachedSet = true)

      val latch = new CountDownLatch(1)
      val future1 = cache(1) { promise =>
        Future {
          latch.await()
          promise.success("A")
        }
      }
      val future2 = cache(1)("")
      latch.countDown()
      future1.await === "A"
      future2.await === "A"

    }
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

  }

  step(system.shutdown())

  def memcachedCache[T](hosts: String, maxCapacity: Int = 500, initialCapacity: Int = 16,
                        timeToLive: Duration = Duration.Zero, timeToIdle: Duration = Duration.Zero,
                        binary : Boolean = true, waitForMemcachedSet : Boolean = false) = {
    binary match {
      case true => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.BINARY, waitForMemcachedSet = waitForMemcachedSet)
      case false => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.TEXT, waitForMemcachedSet = waitForMemcachedSet)
    }
  }

}
