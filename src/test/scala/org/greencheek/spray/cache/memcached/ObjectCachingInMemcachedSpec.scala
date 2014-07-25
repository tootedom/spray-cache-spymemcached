package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.{WithMemcached, MemcachedBasedSpec}
import akka.actor.ActorSystem
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.{Future, ExecutionContext}
import ExecutionContext.Implicits.global
import spray.util._
import spray.caching.Cache

case class MyCase(val name : String,val millis : Long = System.currentTimeMillis())

class MyObject(val name : String) extends Serializable {
  val millis = System.currentTimeMillis()
  val bytes = Array[Byte](1,2,3,4)

  override def equals(instance : Any) : Boolean = {
    if(! instance.isInstanceOf[MyObject] ) {
      false
    }
    else {
      if ( instance == null ) {
        false
      } else {
        val theObject = instance.asInstanceOf[MyObject]
        if( name == theObject.name && millis == theObject.millis) {
          true
        } else {
          false
        }
      }
    }
  }
}

/**
 * Created by dominictootell on 02/04/2014.
 */
@RunWith(classOf[JUnitRunner])
class ObjectCachingInMemcachedSpec  extends MemcachedBasedSpec {

  implicit val system = ActorSystem()

  val memcachedContext = WithMemcached(false)


  "A Memcached cache" >> {
    "can store serializable objects" in memcachedContext {
      val hosts = "localhost:"+memcachedContext.memcached.port

      val cache = new MemcachedCache[Serializable] ( memcachedHosts = hosts, protocol = Protocol.TEXT,
        timeToLive = Duration(5,TimeUnit.SECONDS),waitForMemcachedSet = true)


      val bernie = new MyObject("Bernie")
      cache("1")(bernie).await === bernie
      cache("1")(bernie).await === bernie
      Thread.sleep(2000)
      cache.get("1") must beSome
      cache.get("1").get.await === bernie


      val madoff = new MyCase("Madoff")
      val victim = new MyCase("actorX")
      cache("PonziScheme")(madoff).await === madoff
      Thread.sleep(2000)
      cache.get("PonziScheme") must beSome
      cache.get("PonziScheme").get.await === madoff


      cachedOp(cache,"PonziScheme").await === madoff
      cachedOp(cache,"PonziScheme").await !== victim
    }
  }

  // if we have an "expensive" operation
  def expensiveOp(name : String = "Madoff"): MyCase = {
    Thread.sleep(500)
    new MyCase(name)
  }

  def cachedOp[T](cache : Cache[Serializable],key: T): Future[Serializable] = cache(key) {
    expensiveOp()
  }


}
