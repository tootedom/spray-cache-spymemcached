package org.greencheek.util.memcached

import org.specs2.specification.BeforeAfter

case class WithMemcached(val binary : Boolean = true) extends BeforeAfter {

  @volatile var memcached : MemcachedDaemonWrapper = null

  def before = {
    memcached = MemcachedDaemonFactory.createMemcachedDaemon(binary)

  }
  def after = {
    try {
      MemcachedDaemonFactory.stopMemcachedDaemon(memcached)
    } catch {
      case e : Exception => {

      }
    }
  }
}