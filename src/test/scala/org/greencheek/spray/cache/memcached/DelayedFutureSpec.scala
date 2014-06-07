package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.{WithMemcached}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import scala.concurrent.duration.Duration
import org.specs2.mutable.Specification
import org.greencheek.spray.cache.memcached.keyhashing.SHA256KeyHash
import spray.util._

/**
 * Created by dominictootell on 05/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class DelayedFutureSpec extends Specification {
  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)

  "A Memcached cache" >> {
    "store more than max capacity" in memcachedContext {
      val cache = new MemcachedCache[String](Duration.Zero, 10000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = SHA256KeyHash)

      val option1 = cache("35")(future {
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

      val option2 = cache("45")(future {
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


      System.out.println("Before the Waiting.")
      option1.await == "hello"
      option2.await == "hello2"


      cache.get("35").get.await == "hello"
      cache.get("45").get.await == "hello2"

      memcachedContext.memcached.size() == 2

      true
    }
    "store case class object" in memcachedContext {
      val cache = new MemcachedCache[SimpleClass](Duration.Zero, 10000, "localhost:" + memcachedContext.memcached.port, protocol = Protocol.TEXT,
        waitForMemcachedSet = true, allowFlush = false, keyHashType = SHA256KeyHash)

      val option1 = cache("35")(future {
        try {
          Thread.sleep(1000)
          System.out.println("Sleep End.. 35")
          System.out.flush()
        } catch {
          case e: Exception => {

          }
        }
        SimpleClass(35)
      })

      val option2 = cache("35")(future {
        try {
          Thread.sleep(500)
          System.out.println("Sleep End.. 45")
          System.out.flush()
        } catch {
          case e: Exception => {

          }
        }
        SimpleClass(777)
      })


      System.out.println("Before the Waiting.")
      option1.await == SimpleClass(35)
      option2.await == SimpleClass(35)


      cache.get("35").get.await == SimpleClass(35)
      cache.get("35").get.await == SimpleClass(35)

      memcachedContext.memcached.size() == 2

      true
    }
  }
}

@SerialVersionUID(3200663006510408715L)
case class SimpleClass(i: Int)
