package org.greencheek.util.memcached

import org.specs2.mutable.Specification
import com.thimbleware.jmemcached._
import java.io.IOException
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap
import java.net.{ServerSocket, InetSocketAddress}
import scala.Some
import com.thimbleware.jmemcached.storage.CacheStorage
import org.greencheek.jms.util.PortUtil
import org.apache.activemq.broker.BrokerService
import org.greencheek.util.PortUtil

private class MemCacheDaemonWrapper(val daemon : MemCacheDaemon[LocalCacheElement], val port : Int)

/**
 * Created by dominictootell on 29/03/2014.
 */
trait MemcachedBasedSpec extends Specification {
  import org.specs2._
  import specification._

  @volatile var memcachedd : Option[MemCacheDaemonWrapper] = None
  @volatile var portServerSocket : ServerSocket = null
  @volatile var port : Int = -1

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: =>Fragments) = Step(startMemcached) ^ fs ^ Step(stopMemcached)

  def startMemcached() = {
  {
    portServerSocket = PortUtil.findFreePort
    port = PortUtil.getPort(portServerSocket)
    startMemcachedDaemon(port)
  }

  def stopMemcached() = {
    stopMemcachedDaemon(memcachedd)
  }


  private def startMemcachedDaemon(port : Int)  : Option[MemCacheDaemonWrapper] =
  {
    try {
      val daemon: MemCacheDaemon[LocalCacheElement] = new MemCacheDaemon[LocalCacheElement]();

      val cacheStorage: CacheStorage[Key, LocalCacheElement] = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, 10, 512000);
      val cacheImpl: Cache[LocalCacheElement] = new CacheImpl(cacheStorage)
      daemon.setCache(cacheImpl);
      daemon.setAddr(new InetSocketAddress("localhost", port));
      daemon.setIdleTime(100000);
      daemon.setBinary(false);
      daemon.setVerbose(true);
      daemon.start();

      // give the daemon a moment to start
      Thread.sleep(500);
      Some(new MemCacheDaemonWrapper(daemon, port));
    } catch {
      case e : Exception => {
        None
      }
    }
  }

  def stopMemcachedDaemon(memcachedDaemon : Option[MemCacheDaemonWrapper]) : Unit =
  {
    if(memcachedDaemon!=None)
    {
      if(memcachedDaemon.get.daemon.isRunning())
      {
        System.out.println("Shutting down the Memcached Daemon");
        memcachedDaemon.get.daemon.stop();

        try {
          Thread.sleep(500);
        } catch {
          case e : InterruptedException => {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
  }

}
