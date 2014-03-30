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

/**
 * Created by dominictootell on 30/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class NoMemcachedHostsSpec extends MemcachedBasedSpec {

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)


  "A Memcached cache" >> {
    "can store values when one host is unavailabled" in memcachedContext {

      val randomPort = PortUtil.getPort(PortUtil.findFreePort)
      val hosts = "localhost:" + randomPort + ",localhost:" + randomPort

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

      memcachedContext.memcached.daemon.get.getCache.getCurrentItems == 0
      memcachedD.daemon.get.getCache.getCurrentItems == 0

    }
  }

}
