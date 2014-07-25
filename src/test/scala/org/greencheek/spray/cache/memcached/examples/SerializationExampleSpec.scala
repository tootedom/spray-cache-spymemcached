package org.greencheek.spray.cache.memcached.examples

import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.util.memcached.WithMemcached
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.specs2.runner.JUnitRunner
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import spray.util._
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.caching.Cache
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
import org.greencheek.spy.extensions.FastSerializingTranscoder


@SerialVersionUID(1l) case class PonziScheme(owner : Person, victims : Seq[Person])

@SerialVersionUID(2l) case class Person(val firstName : String,val lastName : String) {
  private val fullName = firstName + " " + lastName
  override def toString() : String = {
    fullName
  }
}

@RunWith(classOf[JUnitRunner])
class SerializationExampleSpec extends Specification {
  val memcachedContext = WithMemcached(false)

  "Example case class serialization" in memcachedContext {
    val memcachedHosts = "localhost:" + memcachedContext.memcached.port
    val cache: Cache[PonziScheme] = new MemcachedCache[PonziScheme](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)


    val madoff = new Person("Bernie","Madoff")
    val victim1 = new Person("Kevin","Bacon")
    val victim2 = new Person("Kyra", "Sedgwick")

    val madoffsScheme = new PonziScheme(madoff,Seq(victim1,victim2))

    cache(madoff)(madoffsScheme).await === madoffsScheme
    // Wait for a bit.. item is still cached (5 second expiry)
    Thread.sleep(2000)
    cache.get(madoff) must beSome
    cache.get(madoff).get.await.owner === madoff

    // Use the expensive operation method, this returns as it's in memcached
    cachedOp(cache, madoff).await === madoffsScheme
    cachedOp(cache, Person("Charles","Ponzi")).await === new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))

    // if we have an "expensive" operation
    def expensiveOp(): PonziScheme = {
      Thread.sleep(500)
      new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))
    }

    def cachedOp[T](cache: Cache[PonziScheme], key: T): Future[PonziScheme] = cache(key) {
      expensiveOp()
    }

    true
  }
  "Example case class serialization with FST Serializer" in memcachedContext {
    val memcachedHosts = "localhost:" + memcachedContext.memcached.port
    val cache: Cache[PonziScheme] = new MemcachedCache[PonziScheme](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash, serializingTranscoder = new FastSerializingTranscoder)


    val madoff = new Person("Bernie","Madoff")
    val victim1 = new Person("Kevin","Bacon")
    val victim2 = new Person("Kyra", "Sedgwick")

    val madoffsScheme = new PonziScheme(madoff,Seq(victim1,victim2))

    cache(madoff)(madoffsScheme).await === madoffsScheme
    // Wait for a bit.. item is still cached (5 second expiry)
    Thread.sleep(2000)
    cache.get(madoff) must beSome
    cache.get(madoff).get.await.owner === madoff

    // Use the expensive operation method, this returns as it's in memcached
    cachedOp(cache, madoff).await === madoffsScheme
    cachedOp(cache, Person("Charles","Ponzi")).await === new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))

    // if we have an "expensive" operation
    def expensiveOp(): PonziScheme = {
      Thread.sleep(500)
      new PonziScheme(Person("Charles","Ponzi"),Seq(Person("Rose","Gnecco")))
    }

    def cachedOp[T](cache: Cache[PonziScheme], key: T): Future[PonziScheme] = cache(key) {
      expensiveOp()
    }

    true
  }
}
