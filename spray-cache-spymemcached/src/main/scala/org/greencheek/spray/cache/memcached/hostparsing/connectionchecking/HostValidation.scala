package org.greencheek.spray.cache.memcached.hostparsing.connectionchecking

import scala.concurrent.duration.Duration
import java.net.InetSocketAddress

/**
 * Created by dominictootell on 05/06/2014.
 */
trait HostValidation {
  def validateMemcacheHosts(checkTimeout : Duration,
                            addressesToCheck : List[InetSocketAddress]) : List[InetSocketAddress]
}
