package org.greencheek.spray.cache.memcached


import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.util._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/06/2014.
 */
class StaleCacheSpec extends MemcachedBasedSpec {

  val largeContent = LargeString.string
  val largeContent2 = "LARGE2"+ LargeString.string

  implicit val system = ActorSystem()

  override def useBinary = false

  "A Memcached cache" >> {
    "can store a piece of content, which is retrieved from stale cache" in {

      val hosts = "localhost:"+memcachedDport

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, waitForMemcachedSet = true,
        useStaleCache = true, timeToLive = Duration(1,TimeUnit.SECONDS),
        staleCacheAdditionalTimeToLive = Duration(4,TimeUnit.SECONDS))

      cache("content")( future {
        Thread.sleep(1000)
        "WILL BE STALE"
      }).await === "WILL BE STALE"

      cache("content")("B").await === "WILL BE STALE"
      cache("content")("B").await === "WILL BE STALE"

      Thread.sleep(2500)

      val passThrough = cache("content")( future {
        Thread.sleep(1000)
        "NEW VALUE"
      })


      cache("content")("B")
      cache("content")("B")
      cache("content")("B")
      cache("content")("B").await === "WILL BE STALE"
      cache("content")("B").await === "WILL BE STALE"

      passThrough.await === "NEW VALUE"

      cache("content")("B").await === "NEW VALUE"


    }
    "can store a piece of content, which is not retrieved from stale cache on timeout" in {

      val hosts = "localhost:"+memcachedDport

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, waitForMemcachedSet = true,
        useStaleCache = true, timeToLive = Duration(1,TimeUnit.SECONDS),
        staleCacheAdditionalTimeToLive = Duration(4,TimeUnit.SECONDS),
        staleCacheMemachedGetTimeout = Duration(1,TimeUnit.NANOSECONDS)
      )

      cache("content2")( future {
        Thread.sleep(1000)
        "WILL BE STALE"
      }).await === "WILL BE STALE"

      cache("content2")("B").await === "WILL BE STALE"
      cache("content2")("B").await === "WILL BE STALE"

      Thread.sleep(2500)

      val passThrough = cache("content2")( future {
        Thread.sleep(1000)
        "NEW VALUE"
      })


      cache("content2")("B")
      cache("content2")("B")
      cache("content2")("B")
      cache("content2")("B").await === "NEW VALUE"
      cache("content2")("B").await === "NEW VALUE"

      passThrough.await === "NEW VALUE"

      cache("content2")("B").await === "NEW VALUE"


    }
    "can store a piece of content, which is not retrieved from stale cache, when element is not present" in {

      val hosts = "localhost:"+memcachedDport

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, waitForMemcachedSet = true,
        useStaleCache = true, timeToLive = Duration(1,TimeUnit.SECONDS),
        staleCacheAdditionalTimeToLive = Duration(4,TimeUnit.SECONDS),
        staleCacheMemachedGetTimeout = Duration(1,TimeUnit.MILLISECONDS)
      )

      cache("content3")( future {
        Thread.sleep(1000)
        "WILL BE STALE"
      }).await === "WILL BE STALE"

      cache("content3")("B").await === "WILL BE STALE"
      cache("content3")("B").await === "WILL BE STALE"

      Thread.sleep(2500)

      val passThrough = cache("content3")( future {
        Thread.sleep(10000)
        "NEW VALUE"
      })


      cache.remove("stalecontent3")

      cache("content3")("B")
      cache("content3")("B")
      cache("content3")("B")
      cache("content3")("B").await === "NEW VALUE"
      cache("content3")("B").await === "NEW VALUE"

      passThrough.await === "NEW VALUE"

      cache("content3")("B").await === "NEW VALUE"


    }
  }

}

