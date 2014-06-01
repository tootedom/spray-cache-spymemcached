package org.greencheek.util.memcached

import com.thimbleware.jmemcached.{LocalCacheElement, MemCacheDaemon}

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