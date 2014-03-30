package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.greencheek.util.PortUtil
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import scala.reflect.io.{File, Path}

// adds await on the future
import spray.util._

object FileReader {
  def using[A <: { def close():Unit},B](resource:A)(f: A => B) : B = {
    try {
      f(resource)
    } finally {
      resource.close
    }
  }
}

/**
 * Created by dominictootell on 30/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class LargeContentCachingSpec extends MemcachedBasedSpec {

  val largeContentFile : java.io.File =  new java.io.File(this.getClass.getResource("/largecontent").getPath)

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)


  "A Memcached cache" >> {
    "can store a large piece of content" in memcachedContext {
      val content = FileReader.using(io.Source.fromFile(largeContentFile)) {
        source => { source.mkString }
      }

      val hosts = "localhost:"+memcachedContext.memcached.port

      val cache = new MemcachedCache[String] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        doHostConnectionAttempt = true, waitForMemcachedSet = true)


      cache("1")(content).await === content
      cache("1")("B").await === content

      memcachedContext.memcached.daemon.get.getCache.getCurrentItems === 1
      memcachedD.daemon.get.getCache.getCurrentItems === 0

    }
  }

}
