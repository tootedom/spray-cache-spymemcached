package org.greencheek.spray.cache.memcached

import org.greencheek.util.memcached.WithMemcached
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created by dominictootell on 30/03/2014.
 */
// jmemcache binary protocol does not play nice with spy
// Therefore this class does not have the JUnit runner on it.
// But can be run manually from a ide
class BinaryMemcachedCacheSpec extends MemcachedCacheSpec{
  override def getMemcacheContext(): WithMemcached = WithMemcached(true)
  override def getMemcachedHostsString() : Option[String]  = { Some("localhost:11211") }
}
