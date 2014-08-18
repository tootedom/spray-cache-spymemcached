package org.greencheek.spray.cache.memcached

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import spray.util.pimps.PimpedFuture

import scala.concurrent._
import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import org.greencheek.util.memcached.WithMemcached
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

/**
 * Created by dominictootell on 11/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class CheckSimpleCacheSpec extends Specification {
  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)

  "A Memcached cache" >> {


    "store and wait on same future" in memcachedContext {
      val hosts = "localhost:"+memcachedContext.memcached.port

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        timeToLive = Duration(5,TimeUnit.SECONDS),waitForMemcachedSet = true)

      val option1 = cache("20")( Future {
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
  }
}
