package org.greencheek.util.memcached

import org.specs2.mutable.Specification
import org.slf4j.{LoggerFactory, Logger}

import com.thimbleware.jmemcached._
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap
import java.net.{Inet4Address, SocketAddress, ServerSocket, InetSocketAddress}
import scala.Some
import com.thimbleware.jmemcached.storage.CacheStorage
import org.greencheek.util.PortUtil

class MemcachedDaemonWrapper(val daemon: Option[MemCacheDaemon[LocalCacheElement]], val port: Int) {
  def size() : Long = {
    daemon match {
      case None => {
        0
      }
      case Some(memcached) => {
        memcached.getCache.getCurrentItems
      }
    }
  }
}

//class MemcachedDaemonWrapper(val daemon: Option[InProcessMemcached], val port: Int)

object MemcachedDaemonFactory {
  private val logger : Logger = LoggerFactory.getLogger("MemcachedDaemonFactory")

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
      daemon.setCache(cacheImpl)
      daemon.setAddr(new InetSocketAddress("localhost", port))
      daemon.setIdleTime(100000)
      daemon.setBinary(binary)
      daemon.setVerbose(true)
      daemon.start()
      Thread.sleep(500)



      new MemcachedDaemonWrapper(Some(daemon), port);
    } catch {
      case e: Exception => {
        logger.error("Error starting memcached", e)
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
