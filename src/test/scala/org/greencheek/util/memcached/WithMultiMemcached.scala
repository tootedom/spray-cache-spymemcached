package org.greencheek.util.memcached

import org.specs2.specification.BeforeAfter

/**
 * Created by dominictootell on 08/06/2014.
 */
case class WithMultiMemcached (val binary : Boolean = true, val number : Int = 1) extends BeforeAfter {

  @volatile var memcachedInstances : Vector[MemcachedDaemonWrapper] = Vector[MemcachedDaemonWrapper]()

  def before = {
    for(x <- 1 to number) {
      memcachedInstances = memcachedInstances :+ MemcachedDaemonFactory.createMemcachedDaemon(binary)
    }

  }
  def after = {
    for (memcached <- memcachedInstances) {
      try {
        MemcachedDaemonFactory.stopMemcachedDaemon(memcached)
      } catch {
        case e: Exception => {

        }
      }
    }
  }
}
