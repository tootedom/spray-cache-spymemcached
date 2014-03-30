package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.WithMemcached

/**
 * Created by dominictootell on 30/03/2014.
 */
class AsciiMemcachedCacheSpec extends MemcachedCacheSpec{
  override def getMemcacheContext(): WithMemcached = WithMemcached(false)
}