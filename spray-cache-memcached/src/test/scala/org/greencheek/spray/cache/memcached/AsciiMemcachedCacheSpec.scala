package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.WithMemcached
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by dominictootell on 30/03/2014.
 */
@RunWith(classOf[JUnitRunner])
class AsciiMemcachedCacheSpec extends MemcachedCacheSpec{
  override def getMemcacheContext(): WithMemcached = WithMemcached(false)
}