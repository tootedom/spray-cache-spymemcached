package org.greencheek.spray.cache.memcached.clientbuilder.staticclient

import java.net.InetSocketAddress

import net.spy.memcached._
import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory
import org.greencheek.spray.cache.memcached.hostparsing.connectionchecking.HostValidation
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.HostResolver
import org.greencheek.spray.cache.memcached.hostparsing.HostStringParser
import scala.collection.JavaConversions._


import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/07/2014.
 */
class SpyMemcachedClientFactory(val memcachedHosts : String,
                                val throwExceptionOnNoHosts : Boolean,
                                val dnsConnectionTimeout : Duration,
                                val doHostConnectionAttempt : Boolean,
                                val hostConnectionAttemptTimeout : Duration,
                                val hostStringParser : HostStringParser,
                                val hostHostResolver : HostResolver,
                                val hostValidation : HostValidation,
                                val connnectionFactory : ConnectionFactory) extends ClientFactory {



  val memcached = createMemcachedClient(memcachedHosts);
  val enabled : Boolean = memcached match {
    case Some(_) => true
    case None => false
  }

  private def createMemcachedClient(memcachedHosts : String) : Option[MemcachedClient] = {
    val parsedHosts : List[(String,Int)] =  hostStringParser.parseMemcachedNodeList(memcachedHosts)

    parsedHosts match {
      case Nil => {
        if(throwExceptionOnNoHosts) {
          throw new InstantiationError()
        } else {
          None
        }
      }
      case hosts : List[(String,Int)] => {
        var addresses : List[InetSocketAddress] = hostHostResolver.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout)
        addresses match {
          case Nil => {
            if(throwExceptionOnNoHosts) {
              throw new InstantiationError()
            } else {
              None
            }
          }
          case resolvedHosts : List[InetSocketAddress] => {
            if(doHostConnectionAttempt) {
              addresses = hostValidation.validateMemcacheHosts(hostConnectionAttemptTimeout,resolvedHosts)
            }
          }
        }

        addresses match {
          case Nil => {
            None
          }
          case hostsToUse : List[InetSocketAddress] => {
            Some(new MemcachedClient(connnectionFactory,hostsToUse))
          }
        }
      }
    }
  }


  override def getClient(): MemcachedClientIF = {
    if(memcached.isDefined) {
      memcached.get
    } else {
      null
    }
  }

  override def isEnabled() : Boolean = {
    enabled
  }

  override def shutdown() : Unit = {
    if(isEnabled()) {
      memcached.get.shutdown()
    }
  }
}
