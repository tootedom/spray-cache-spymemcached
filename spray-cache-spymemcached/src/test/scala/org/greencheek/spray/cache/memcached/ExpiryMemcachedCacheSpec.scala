package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import org.greencheek.util.PortUtil
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.scalatest.time.Seconds
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import spray.util._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner


/**
 * Created by dominictootell on 01/04/2014.
 */
@RunWith(classOf[JUnitRunner])
class ExpiryMemcachedCacheSpec extends MemcachedBasedSpec {

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)


  "A Memcached cache" >> {
    "can store per item expiry times" in memcachedContext {

      val hosts = "localhost:"+memcachedContext.memcached.port

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        timeToLive = Duration(5,TimeUnit.SECONDS),waitForMemcachedSet = true)

      val x = Duration(1,TimeUnit.SECONDS)
      cache(("1",x))("A").await === "A"
      cache("1")("A").await === "A"
      cache("1")("A").await === "A"
      cache("1")("A").await === "A"
      Thread.sleep(2000)
      cache((x,"1"))("A").await === "A"

      memcachedContext.memcached.size === 1

      Thread.sleep(2000)
      cache.get("1") must beNone

      cache("2")("F").await === "F"
      Thread.sleep(2000)
      cache.get("2") must beSome
      Thread.sleep(5000)
      cache.get("2") must beNone

      memcachedD.size === 0

    }
    "can store forever" in memcachedContext {

      val hosts = "localhost:"+memcachedContext.memcached.port

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        timeToLive = Duration(1,TimeUnit.SECONDS),waitForMemcachedSet = true)


      cache("E")("L").await === "L"
      cache.get("E") must beSome
      Thread.sleep(2000)
      cache.get("E") must beNone

      cache(("3",Duration(100,TimeUnit.MILLISECONDS)))("A").await === "A"
      cache("3")("B").await === "A"
      cache("3")("C").await === "A"
      cache("3")("D").await === "A"
      Thread.sleep(2000)
      cache.get("3") must beSome
      cache((Duration.Inf,"3"))("E").await === "A"
      cache.get("3") must beSome

      cache.remove("3").get.await === "A"
      cache("3")("F").await === "F"
      cache.get("3") must beSome
      Thread.sleep(2000)
      cache.get("3") must beNone

      cache(("1",Duration.Inf))("A").await === "A"
      cache("1")("B").await === "A"
      cache("1")("C").await === "A"
      cache("1")("D").await === "A"
      Thread.sleep(2000)
      cache.get("1") must beSome
      cache((Duration.Inf,"1"))("E").await === "A"
      cache.get("1") must beSome

      Thread.sleep(2000)
      cache.get("1") must beSome

      cache(("2",Duration.Zero))("F").await === "F"
      Thread.sleep(2000)
      cache.get("2") must beSome
      Thread.sleep(5000)
      cache.get("2") must beSome

    }
  }

}