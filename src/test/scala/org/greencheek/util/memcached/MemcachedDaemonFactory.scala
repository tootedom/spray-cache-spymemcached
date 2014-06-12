package org.greencheek.util.memcached

import org.slf4j.{LoggerFactory, Logger}
import org.greencheek.util.PortUtil
import com.thimbleware.jmemcached._
import com.thimbleware.jmemcached.storage.CacheStorage
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap
import scala.Some
import java.net.InetSocketAddress
import java.util.concurrent.Semaphore


object MemcachedDaemonFactory {
  private val logger : Logger = LoggerFactory.getLogger("MemcachedDaemonFactory")
  private val portUtil = new PortUtil()
  private val portSemaphore : Semaphore = new Semaphore(1)
  def createMemcachedDaemon(binary : Boolean = true) : MemcachedDaemonWrapper = {
    portSemaphore.acquire()
    try {
      val portServerSocket = portUtil.findFreePort
      val memcachedDport = portUtil.getPort(portServerSocket)
      startMemcachedDaemon(memcachedDport, binary)
    } finally {
      portSemaphore.release()
    }
  }

  private[memcached] def startMemcachedDaemon(port: Int, binary : Boolean = true): MemcachedDaemonWrapper = {
    try {
      val daemon: MemCacheDaemon[LocalCacheElement] = new MemCacheDaemon[LocalCacheElement]();


      val cacheStorage: CacheStorage[Key, LocalCacheElement] = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, 2000, 512000);
      val cacheImpl: Cache[LocalCacheElement] = new CacheImpl(cacheStorage)
      daemon.setCache(cacheImpl)
      daemon.setAddr(new InetSocketAddress("localhost", port))
      daemon.setIdleTime(100000)
      daemon.setBinary(binary)
      daemon.setVerbose(true)
      daemon.start()



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
        System.out.println("Shutting down the Memcached Daemon on port: " + memcachedDaemon.port);
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