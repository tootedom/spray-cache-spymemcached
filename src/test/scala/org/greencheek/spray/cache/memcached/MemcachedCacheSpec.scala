package org.greencheek.spray.cache.memcached


import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.actor.ActorSystem
import scala.concurrent.duration._
import org.specs2.mutable.Specification
import spray.util._
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.greencheek.util.memcached.WithMemcached
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.greencheek.spray.cache.memcached.keyhashing.{NoKeyHash, KeyHashType}
import net.spy.memcached.HashAlgorithm

abstract class MemcachedCacheSpec extends Specification {
  implicit val system = ActorSystem()

  def getMemcacheContext() : WithMemcached
  def getMemcachedHostsString() : Option[String] = {
    None
  }

  val memcachedContext = getMemcacheContext();


  "A Memcached cache" >> {
    "store uncached values" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary)

      cache("1")("A").await === "A"
      cache("2")("B").await === "B"
      cache("3")("B").await === "B"
      cache("4")("B").await === "B"
      cache("5")("F").await === "F"


      cache.get("1").get.await === "A"
      cache.get("5").get.await === "F"
      cache.get("9") === None

      true
    }
    "store and wait on same future" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary)

      val option1 = cache("20")(future {
        try {
          Thread.sleep(1000)
        } catch {
          case e: Exception => {

          }
        }
        "hello"
      })

      cache.get("20").get.await == "hello"

      option1.await == "hello"


      true
    }
    "store more than max capacity" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),1,binary = memcachedContext.binary,
                                          waitForMemcachedSet = true)

      val option1 = cache("35")( future {
        try {
          Thread.sleep(1000)
          System.out.println("Sleep End.. 35")
          System.out.flush()
        } catch {
          case e: Exception => {

          }
        }
        "hello"
      })

      val option2 = cache("45")( future {
        try {
          Thread.sleep(500)
          System.out.println("Sleep End.. 45")
          System.out.flush()
        } catch {
          case e: Exception => {

          }
        }
        "hello2"
      })


      option1.await == "hello"
      option2.await == "hello2"


      cache.get("35").get.await == "hello"
      cache.get("45").get.await == "hello2"

      memcachedContext.memcached.size() == 2

      true
    }
    "add keys with toString methods" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),1,binary = memcachedContext.binary)

      cache(100)("A").await == "A"
      cache(200)("B").await == "B"
      cache(300)("B").await == "B"
      cache(400)("B").await == "B"
      cache(500)("B").await == "F"


      cache.get(100).get.await == "A"
      cache.get(500).get.await == "F"
      cache.get(900) == None

      memcachedContext.memcached.daemon.size == 5

      true
    }
    "return stored values upon cache hit on existing values" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary =  memcachedContext.binary,
      waitForMemcachedSet = true)
      cache(1000)("A").await === "A"
      cache(1000)("").await === "A"
    }
    "return Futures on uncached values during evaluation and replace these with the value afterwards" in memcachedContext{
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary =  memcachedContext.binary,
        waitForMemcachedSet = true)

      val latch = new CountDownLatch(1)
      val future1 = cache(199) { promise =>
        Future {
          latch.await()
          promise.success("A")
        }
      }
      val future2 = cache(199)("")
      latch.countDown()
      future1.await === "A"
      future2.await === "A"

    }
    "not cache exceptions" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary)

      cache(167)((throw new RuntimeException("Naa")): String).await must throwA[RuntimeException]("Naa")
      cache(167)("A").await === "A"
    }
    "remove items from the cache" in memcachedContext{
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,waitForMemcachedRemove = true)

      cache(144)("A").await === "A"

      cache.remove(144)
      cache.remove(144)

      cache(144)("B").await === "B"

    }
    "expire old entries" in memcachedContext {
      val cache = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,
        timeToLive = Duration(1,SECONDS))

      cache(15678)("A").await === "A"
      cache(25678)("B").await === "B"
      Thread.sleep(2000)
      cache(35678)("C").await === "C"

      cache(25678)("F").await === "F"
      Thread.sleep(2000)
      cache.get(15678) must beNone // removed on request
      cache.get(25678) must beNone // removed on request
      cache.get(35678) must beNone // but not retrievable anymore



    }
    "provide a key prefix" in memcachedContext {
      val cacheWithPrefixA = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,
        timeToLive = Duration(10,SECONDS),keyPrefix = Some("A"))
      val cacheWithPrefixB = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,
        timeToLive = Duration(10,SECONDS),keyPrefix = Some("B"))
      val cacheWithNoPrefixA = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,
        timeToLive = Duration(10,SECONDS))
      val cacheWithNoPrefixB = memcachedCache[String](getMemcachedHostsString.getOrElse("localhost:"+memcachedContext.memcached.port),binary = memcachedContext.binary,
        timeToLive = Duration(10,SECONDS))

      cacheWithPrefixA(15678)("A").await === "A"
      cacheWithPrefixA(15678)("B").await === "A"
      cacheWithPrefixB(15678)("F").await === "F"
      cacheWithPrefixB(15678)("B").await === "F"
      cacheWithNoPrefixA(15678)("F").await === "F"
      cacheWithNoPrefixB(15678)("F").await === "F"
    }

  }

  step(system.shutdown())

  def memcachedCache[T](hosts: String, maxCapacity: Int = 500, initialCapacity: Int = 16,
                        timeToLive: Duration = Duration.Zero, timeToIdle: Duration = Duration.Zero,
                        binary : Boolean = true, waitForMemcachedSet : Boolean = false,
                        allowFlush : Boolean = true, waitForMemcachedRemove : Boolean = false, keyPrefix : Option[String]= None,
                        keyHashType : KeyHashType = NoKeyHash, hashAlgo : HashAlgorithm = MemcachedCache.DEFAULT_ALGORITHM ) = {
    binary match {
      case true => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.BINARY,
        waitForMemcachedSet = waitForMemcachedSet, allowFlush = allowFlush, waitForMemcachedRemove = waitForMemcachedRemove,
        removeWaitDuration = Duration(4,TimeUnit.SECONDS), keyPrefix = keyPrefix, keyHashType = keyHashType, hashAlgorithm = hashAlgo)
      case false => new MemcachedCache[T] (timeToLive, maxCapacity, hosts, protocol = Protocol.TEXT,
        waitForMemcachedSet = waitForMemcachedSet,allowFlush = allowFlush, waitForMemcachedRemove = waitForMemcachedRemove,
        removeWaitDuration = Duration(4,TimeUnit.SECONDS), keyPrefix = keyPrefix)
    }
  }

}
