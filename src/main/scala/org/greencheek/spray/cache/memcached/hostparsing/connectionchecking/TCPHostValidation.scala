package org.greencheek.spray.cache.memcached.hostparsing.connectionchecking

import scala.concurrent.duration.Duration
import java.net.InetSocketAddress
import org.greencheek.dns.lookup.{TCPAddressChecker, AddressChecker}
import org.slf4j.{Logger, LoggerFactory}


object TCPHostValidation extends HostValidation{
  private val logger : Logger = LoggerFactory.getLogger(classOf[HostValidation])

  def validateMemcacheHosts(checkTimeout : Duration,
                                    addressesToCheck : List[InetSocketAddress]) : List[InetSocketAddress] = {
    var okAddresses : List[InetSocketAddress]= Nil
    val addressChecker : AddressChecker = new TCPAddressChecker(checkTimeout.toMillis)
    for(addy <- addressesToCheck) {
      addressChecker.isAvailable(addy) match {
        case true => {
          okAddresses = addy :: okAddresses
        }
        case false => {
          logger.error("Unable to connect to memcached node: {}", addy)
        }
      }
    }
    okAddresses
  }
}
