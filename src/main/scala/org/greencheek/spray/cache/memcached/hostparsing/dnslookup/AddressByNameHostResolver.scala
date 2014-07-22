package org.greencheek.spray.cache.memcached.hostparsing.dnslookup

import scala.concurrent.duration.Duration
import java.net.{InetAddress, InetSocketAddress}
import org.greencheek.dns.lookup.LookupService
import java.util.concurrent.{TimeoutException, TimeUnit}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Created by dominictootell on 06/06/2014.
 */
object AddressByNameHostResolver extends HostResolver {
  private val logger : Logger = LoggerFactory.getLogger(classOf[HostResolver])

  override def returnSocketAddressesForHostNames(nodes: List[(String, Int)], dnsLookupTimeout: Duration): List[InetSocketAddress] = {
    val addressLookupService = LookupService.create()

    var workingNodes: List[InetSocketAddress] = Nil
    for (hostAndPort <- nodes) {
      var future: java.util.concurrent.Future[InetAddress] = null
      val host = hostAndPort._1
      val port = hostAndPort._2
      try {
        future = addressLookupService.getByName(host)
        var ia: InetAddress = future.get(dnsLookupTimeout.toSeconds, TimeUnit.SECONDS)
        if (ia == null) {
          logger.error("Unable to resolve dns entry for the host: {}", host)
        }
        else
        {
          try {
            workingNodes = new InetSocketAddress(ia,port ) :: workingNodes
          }
          catch {
            case e: IllegalArgumentException => {
              logger.error("Invalid port number has been provided for the memcached node: host({}),port({})", host, port)
            }
          }
        }
      }
      catch {
        case e: TimeoutException => {
          logger.error("Problem resolving host name ({}) to an ip address in fixed number of seconds: {}", host, dnsLookupTimeout, e)
        }
        case e: Exception => {
          logger.error("Problem resolving host name to ip address: {}", host)
        }
      }
      finally {
        if (future != null) future.cancel(true)
      }
    }
    addressLookupService.shutdown()

    workingNodes
  }


}
