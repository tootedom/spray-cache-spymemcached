package org.greencheek.spray.cache.memcached

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import akka.actor.ActorSystem
import org.greencheek.util.memcached.WithMultiMemcached
import org.specs2.runner.JUnitRunner
import spray.util.pimps.PimpedFuture
import scala.concurrent.duration.Duration
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
import spray.util._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created by dominictootell on 08/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class XXHashingShardingSpec extends Specification {
  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)

  implicit val system = ActorSystem()

  val numberOfMemcaches = 3
  val memcachedContext = WithMultiMemcached(false,numberOfMemcaches)

  "Multiple Memcached caches" >> {
    "must be sharded over when using XXHASH_ALGORITHM" in memcachedContext {
      val b = new StringBuffer()
      for(x <- 0 to numberOfMemcaches-1) {
        b.append("localhost:").append(memcachedContext.memcachedInstances(x).port)
        b.append(',')
      }
      b.setLength(b.length()-1)


      val cache = new MemcachedCache[MyNumber](Duration.Zero, 10000, b.toString, protocol = Protocol.TEXT,
                                               waitForMemcachedSet = true, allowFlush = false, keyHashType = XXJavaHash,
                                               hashAlgorithm = MemcachedCache.XXHASH_ALGORITHM,asciiOnlyKeys = true)


      for ( x <- 1 to 2000) {
        cache(Integer.toString(x))(new MyNumber(x)).await.number === x
      }

      for ( x <- 1 to 2000) {
        cache(Integer.toString(x))(new MyNumber(x)).await.number === x
      }

      var finalSize : Long = 0
      for (x <- 0 to numberOfMemcaches-1) {
        memcachedContext.memcachedInstances(x).size() must be greaterThan(500)
        finalSize += memcachedContext.memcachedInstances(x).size()
      }


      finalSize must be equalTo(2000)
    }
  }
}

@SerialVersionUID(1l) case class MyNumber(number : Int)