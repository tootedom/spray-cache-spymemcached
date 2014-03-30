package org.greencheek.util.memcached

import org.specs2.mutable.Specification
import com.thimbleware.jmemcached._
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap
import java.net.{ServerSocket, InetSocketAddress}
import scala.Some
import com.thimbleware.jmemcached.storage.CacheStorage
import org.greencheek.util.PortUtil

class MemcachedDaemonWrapper(val daemon: Option[MemCacheDaemon[LocalCacheElement]], val port: Int)

object MemcachedDaemonFactory {

  def createMemcachedDaemon(binary : Boolean = true) : MemcachedDaemonWrapper = {
    val portServerSocket = PortUtil.findFreePort
    val memcachedDport = PortUtil.getPort(portServerSocket)
    startMemcachedDaemon(memcachedDport,binary)
  }

  private[memcached] def startMemcachedDaemon(port: Int, binary : Boolean = true): MemcachedDaemonWrapper = {
    try {
      val daemon: MemCacheDaemon[LocalCacheElement] = new MemCacheDaemon[LocalCacheElement]();


      val cacheStorage: CacheStorage[Key, LocalCacheElement] = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, 1000, 512000);
      val cacheImpl: Cache[LocalCacheElement] = new CacheImpl(cacheStorage)
      daemon.setCache(cacheImpl);
      daemon.setAddr(new InetSocketAddress("localhost", port));
      daemon.setIdleTime(100000);
      daemon.setBinary(binary);
      daemon.setVerbose(true);
      daemon.start();

      // give the daemon a moment to start
      Thread.sleep(500);
      new MemcachedDaemonWrapper(Some(daemon), port);
    } catch {
      case e: Exception => {
        new MemcachedDaemonWrapper(None, port);
      }
    }
  }

  def stopMemcachedDaemon(memcachedDaemon: MemcachedDaemonWrapper): Unit = {
    if (memcachedDaemon.daemon != None) {
      if (memcachedDaemon.daemon.get.isRunning()) {
        System.out.println("Shutting down the Memcached Daemon");
        memcachedDaemon.daemon.get.stop();

        try {
          Thread.sleep(500);
        } catch {
          case e: InterruptedException => {
            e.printStackTrace();
          }
        }
      }
    }
  }
}

trait MemcachedBasedSpec extends Specification {

  import org.specs2._
  import specification._

  @volatile var memcachedD: MemcachedDaemonWrapper = null
  @volatile var portServerSocket: ServerSocket = null
  @volatile var memcachedDport: Int = -1

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: => Fragments) = Step(startMemcached) ^ fs ^ Step(stopMemcached)

  def startMemcached() = {
    portServerSocket = PortUtil.findFreePort
    memcachedDport = PortUtil.getPort(portServerSocket)
    memcachedD = MemcachedDaemonFactory.startMemcachedDaemon(memcachedDport,true)
  }

  def stopMemcached: Unit = {
    MemcachedDaemonFactory.stopMemcachedDaemon(memcachedD)
  }


}
