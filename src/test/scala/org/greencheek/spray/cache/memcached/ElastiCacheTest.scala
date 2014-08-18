package org.greencheek.spray.cache.memcached

import java.util.Random
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.greencheek.elasticacheconfig.server.StringServer
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.AddressByNameHostResolver
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import org.junit.runner.RunWith
import org.specs2.matcher.Matcher
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.concurrent._
import scala.concurrent.duration.Duration



import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.util._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 23/07/2014.
 */
@RunWith(classOf[JUnitRunner])
class ElastiCacheTest extends MemcachedBasedSpec {

  val memcachedContext = WithMemcached(false)

  val largeContent = LargeString.string
  val largeContent2 = "LARGE2"+ LargeString.string

  implicit val system = ActorSystem()

  override def useBinary = false

  "A ElastiCache with static config " >> {
    "can store a piece of content, which is retrieved from stale cache" in memcachedContext {

      System.out.println("===========")
      System.out.println(memcachedDport)
      System.out.println("===========")
      System.out.flush()

      val configurationsMessage = Array(
        "CONFIG cluster 0 147\r\n" + "1\r\n" + "localhost|127.0.0.1|" + memcachedContext.memcached.port + "\r\n" + "\nEND\r\n"
      )
      var server: StringServer = new StringServer(configurationsMessage, 0, TimeUnit.SECONDS)
      server.before(configurationsMessage, TimeUnit.SECONDS, -1, false)

      var cache : ElastiCache[String] = null

      try {


        val hosts = "localhost:" + memcachedDport

        cache = new ElastiCache[String](elastiCacheConfigHosts = "localhost:"+server.getPort.toString,
          configPollingTime = 10,
          configPollingTimeUnit = TimeUnit.SECONDS,
          protocol = Protocol.TEXT,
          waitForMemcachedSet = true,
          delayBeforeClientClose = Duration(1,TimeUnit.SECONDS),
          hostResolver= AddressByNameHostResolver,
          dnsConnectionTimeout = Duration(2,TimeUnit.SECONDS),
          useStaleCache = true, timeToLive = Duration(1, TimeUnit.SECONDS),
          staleCacheAdditionalTimeToLive = Duration(4, TimeUnit.SECONDS))


        Thread.sleep(1000)

        cache("content")(Future {
          Thread.sleep(1000)
          "WILL BE STALE"
        }).await === "WILL BE STALE"

        cache("content")("B").await === "WILL BE STALE"
        cache("content")("B").await === "WILL BE STALE"

        Thread.sleep(2500)

        val passThrough = cache("content")(Future {
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
      finally {
        server.after()
        cache.close()
      }

      true

    }
    "can store a piece of content, in different memcached as the configurations are updated in elasticache" in memcachedContext {

      System.out.println("===========")
      System.out.println(memcachedDport)
      System.out.println("===========")
      System.out.flush()

      val configurationsMessage = Array(
        "CONFIG cluster 0 147\r\n" + "1\r\n" + "localhost|127.0.0.1|" + memcachedDport + "\r\n" + "\nEND\r\n",
        "CONFIG cluster 0 147\r\n" + "2\r\n" + "localhost|127.0.0.1|" + memcachedDport + " localhost|127.0.0.1|" + memcachedContext.memcached.port + "\r\n" + "\nEND\r\n",
        "CONFIG cluster 0 147\r\n" + "3\r\n" + "localhost|127.0.0.1|" + memcachedContext.memcached.port + "\r\n" + "\nEND\r\n"
      )

      var server: StringServer = new StringServer(configurationsMessage, 0, TimeUnit.SECONDS)
      server.before(configurationsMessage, TimeUnit.SECONDS, -1, false)

      var cache : ElastiCache[String] = null
      try {


        val hosts = "localhost:" + memcachedDport

        cache = new ElastiCache[String](elastiCacheConfigHosts = "localhost:"+server.getPort.toString,
          configPollingTime = 5,
          configPollingTimeUnit = TimeUnit.SECONDS,
          protocol = Protocol.TEXT,
          waitForMemcachedSet = true,
          allowFlush = true,
          useStaleCache = true, timeToLive = Duration(1, TimeUnit.SECONDS),
          delayBeforeClientClose = Duration(1,TimeUnit.SECONDS),
          hostResolver= AddressByNameHostResolver,
          dnsConnectionTimeout = Duration(2,TimeUnit.SECONDS),
          staleCacheAdditionalTimeToLive = Duration(4, TimeUnit.SECONDS))


        Thread.sleep(1000)

        cache("content")("WILL BE STALE").await === "WILL BE STALE"

        cache("content2")("B").await === "B"
        cache("contentmore")("more").await === "more"

        cache("content")("B").await === "WILL BE STALE"

        memcachedContext.memcached.size() must beEqualTo(0l)
        memcachedD.size() must beGreaterThan(0l)


        Thread.sleep(6000)

        cache("content")("WILL BE STALE").await === "WILL BE STALE"

        cache("content2")("B").await === "B"
        cache("contentmore")("more").await === "more"

        cache("content")("B").await === "WILL BE STALE"

        memcachedContext.memcached.size() must beGreaterThan(0l)
        memcachedD.size() must beGreaterThan(0l)

        cache.clear()

        Thread.sleep(6000)

        cache("content")("WILL BE STALE").await === "WILL BE STALE"

        cache("content2")("B").await === "B"
        cache("contentmore")("more").await === "more"

        cache("content")("B").await === "WILL BE STALE"

        memcachedContext.memcached.size() must beGreaterThan(0l)
        memcachedD.size() must beEqualTo(0l)
      }
      finally {
        server.after()
        cache.close()
      }

      true

    }

  }

}
