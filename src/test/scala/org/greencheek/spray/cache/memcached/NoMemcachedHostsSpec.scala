package org.greencheek.spray.cache.memcached

// adds await on the future

import spray.util._

import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.greencheek.util.PortUtil
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 30/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class NoMemcachedHostsSpec extends MemcachedBasedSpec {

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)

  "A Memcached cache" >> {
    "is non invasive when no host are available" in memcachedContext {

      val randomPort = portUtil.getPort(portUtil.findFreePort)
      val hosts = "127.0.0.1:" + randomPort + ",127.0.0.1:" + randomPort

      val cache = new MemcachedCache[String](memcachedHosts = hosts, protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, throwExceptionOnNoHosts = false)

      val myFuture = future {
        try {
          Thread.sleep(5000)
        } catch {
          case e: Exception => {

          }
        }
        "hello there"
      }

      cache("1")("A").await == "A"
      cache("2")("B").await == "B"

      val request1 = cache("3")(myFuture)
      val request2 = cache("3")(myFuture)
      val request3 = cache("3")(myFuture)

      request1.await == "hello there"
      request2.await == "hello there"
      request3.await == "hello there"

      memcachedContext.memcached.size === 0
      memcachedD.size === 0


    }
  }
  "is non invasive when no host are available" in memcachedContext {
    val cache = new MemcachedCache[String](memcachedHosts = "", protocol = Protocol.TEXT,
      doHostConnectionAttempt = true, throwExceptionOnNoHosts = false)

    val myFuture = future {
      try {
        Thread.sleep(5000)
      } catch {
        case e: Exception => {

        }
      }
      "hello there"
    }

    cache("1")("A").await == "A"
    cache("2")("B").await == "B"

    val request1 = cache("3")(myFuture)
    val request2 = cache("3")(myFuture)
    val request3 = cache("3")(myFuture)

    request1.await == "hello there"
    request2.await == "hello there"
    request3.await == "hello there"

    memcachedContext.memcached.size === 0
    memcachedD.size === 0


  }
  "is invasive when requested" in {
    var thrown = false
    try {
      new MemcachedCache[String](memcachedHosts = "", protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, throwExceptionOnNoHosts = true)
      thrown = false
    } catch {
      case e : InstantiationError => {
        thrown = true
      }
    }

    thrown == true
  }
  "is invasive when requested on fake dns name" in {
    var thrown = false
    try {
      new MemcachedCache[String](memcachedHosts = "localhost.1", protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, throwExceptionOnNoHosts = true)
      thrown = false
    } catch {
      case e : InstantiationError => {
        thrown = true
      }
    }

    thrown == true
  }
  "is not invasive on fake dns names" in memcachedContext {
    val cache = new MemcachedCache[String](memcachedHosts = "localhost.1", protocol = Protocol.TEXT,
      doHostConnectionAttempt = true, throwExceptionOnNoHosts = false, dnsConnectionTimeout = Duration(1,TimeUnit.SECONDS))

    val myFuture = future {
      try {
        Thread.sleep(5000)
      } catch {
        case e: Exception => {

        }
      }
      "hello there"
    }

    cache("1")("A").await == "A"
    cache("2")("B").await == "B"

    val request1 = cache("3")(myFuture)
    val request2 = cache("3")(myFuture)
    val request3 = cache("3")(myFuture)

    request1.await == "hello there"
    request2.await == "hello there"
    request3.await == "hello there"

    memcachedContext.memcached.size === 0
    memcachedD.size === 0

  }

}
