package org.greencheek.spray.cache.memcached.clientbuilder

import net.spy.memcached.MemcachedClientIF

/**
 * Created by dominictootell on 14/07/2014.
 */
trait ClientFactory {
  def getClient() : MemcachedClientIF
  def isEnabled() : Boolean
  def shutdown() : Unit
}
