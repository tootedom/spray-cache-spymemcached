package org.greencheek.spray.cache.memcached.examples

import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.util.memcached.WithMemcached
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.specs2.runner.JUnitRunner
import spray.util.pimps.PimpedFuture
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import spray.util._
import scala.concurrent._
import ExecutionContext.Implicits.global
import spray.caching.{SimpleLruCache, Cache}
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash

@SerialVersionUID(1l) case class ProductId(id : String)
@SerialVersionUID(1l) case class Product(description : String)
@SerialVersionUID(1l) case class ProductCategories(category : Seq[String])
/**
 * Created by dominictootell on 07/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class KeyCollisionSpec extends Specification {
  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)



  val productId = ProductId("1a")
  val fridge = Product("huge smeg")
  val fridgeCategories = ProductCategories(Seq("kitchen","home","luxury"))

  val memcachedContext = WithMemcached(false)

  "Example case class serialization" in {
    val categoryCache: Cache[ProductCategories] = new SimpleLruCache[ProductCategories](10,10)
    val productCache: Cache[Product] = new SimpleLruCache[Product](10,10)

    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await === fridge
    categoryCache.get(productId).get.await == fridgeCategories
  }
  "Example key clash" in memcachedContext {
    val memcachedHosts = "localhost:" + memcachedContext.memcached.port

    val categoryCache: Cache[ProductCategories] = new MemcachedCache[ProductCategories](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)
    val productCache: Cache[Product] = new MemcachedCache[Product](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash)


    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await must throwA[ClassCastException]

    categoryCache.get(productId).get.await == fridgeCategories
  }
  "Example key prefix, no clash" in memcachedContext {
    val memcachedHosts = "localhost:" + memcachedContext.memcached.port

    val categoryCache: Cache[ProductCategories] = new MemcachedCache[ProductCategories](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash, keyPrefix = Some("productcategories"))
    val productCache: Cache[Product] = new MemcachedCache[Product](memcachedHosts = memcachedHosts, protocol = Protocol.TEXT,
      timeToLive = Duration(5, TimeUnit.SECONDS), waitForMemcachedSet = true, keyHashType = XXJavaHash, keyPrefix = Some("product"))


    categoryCache(productId)(fridgeCategories).await === fridgeCategories
    productCache(productId)(fridge).await === fridge

    categoryCache.get(productId).get.await == fridgeCategories
  }
}
