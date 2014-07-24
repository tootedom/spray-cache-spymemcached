package org.greencheek.spray.cache.memcached


import java.util.Random
import org.greencheek.elasticacheconfig.server.StringServer
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.AddressByNameHostResolver
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.util._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 24/07/2014.
 */
@RunWith(classOf[JUnitRunner])
class ElastiCacheUpdateSpec  extends MemcachedBasedSpec{
  val memcachedContext = WithMemcached(false)

  implicit val system = ActorSystem()

  override def useBinary = false

  "A ElastiCache in" >> {
    "be thread-safe in xxjavahashing with xx java hash algo" in memcachedContext {

      val configurationsMessage = Array(
        "CONFIG cluster 0 147\r\n" + "1\r\n" + "localhost|127.0.0.1|" + memcachedDport + "\r\n" + "END\r\n",
        "CONFIG cluster 0 147\r\n" + "2\r\n" + "localhost|127.0.0.1|" + memcachedDport + " localhost|127.0.0.1|" + memcachedContext.memcached.port + "\r\n" + "END\r\n",
        "CONFIG cluster 0 147\r\n" + "3\r\n" + "localhost|127.0.0.1|" + memcachedContext.memcached.port + "\r\n" + "END\r\n"
      )

      var server: StringServer = new StringServer(configurationsMessage, 0, TimeUnit.SECONDS)
      server.before(configurationsMessage, TimeUnit.SECONDS, -1, false)

      var cache: ElastiCache[Int] = null
      try {

        val hosts = "localhost:" + memcachedDport



        cache = new ElastiCache[Int](elastiCacheConfigHost = "localhost",
          elastiCacheConfigPort = server.getPort,
          initialConfigPollingDelay = 2,
          configPollingTime = 2,
          configPollingTimeUnit = TimeUnit.SECONDS,
          protocol = Protocol.TEXT,
          waitForMemcachedSet = true,
          allowFlush = true,
          useStaleCache = true, timeToLive = Duration(60, TimeUnit.SECONDS),
          delayBeforeClientClose = Duration(10, TimeUnit.SECONDS),
          hostResolver = AddressByNameHostResolver,
          dnsConnectionTimeout = Duration(2, TimeUnit.SECONDS),
          staleCacheAdditionalTimeToLive = Duration(4, TimeUnit.SECONDS),
          keyHashType = XXJavaHash, hashAlgorithm = MemcachedCache.XXHASH_ALGORITHM,
          maxCapacity = 1000
        )

        Thread.sleep(2000)
        // exercise the cache from 10 parallel "tracks" (threads)
        val views = Future.traverse(Seq.tabulate(10)(identityFunc)) { track =>
          Future {
            val array = Array.fill(1000)(0) // our view of the cache
            val rand = new Random(track)
            (1 to 10000) foreach { i =>
              val ix = rand.nextInt(1000) // for a random index into the cache
              val value = cache(ix) {
                // get (and maybe set) the cache value
                Thread.sleep(0)
                ix
              }.await
              if (array(ix) == 0) array(ix) = value // update our view of the cache
              else if (array(ix) != value) failure("Cache view is inconsistent (track " + track + ", iteration " + i +
                ", index " + ix + ": expected " + array(ix) + " but is " + value)
            }
            array
          }
        }.await


      }
      finally {
        server.after()
        cache.close()
      }
      true
    }
  }
}
