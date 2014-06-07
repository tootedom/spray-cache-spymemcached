package org.greencheek.spray.cache.memcached.hostparsing.dnslookup

import scala.concurrent.duration.Duration
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * Created by dominictootell on 06/06/2014.
 */
trait HostResolver {
  private val DEFAULT_DNS_TIMEOUT : Duration = Duration(3,TimeUnit.SECONDS)

  def returnSocketAddressesForHostNames(nodes: List[(String,Int)],
                                        dnsLookupTimeout : Duration = DEFAULT_DNS_TIMEOUT): List[InetSocketAddress]
}
