package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import java.net.{InetSocketAddress, InetAddress}
import java.util.concurrent.{TimeUnit, ScheduledExecutorService, Executors}
import java.util.concurrent.atomic.AtomicBoolean

import net.spy.memcached.{MemcachedClient, ConnectionFactory}
import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.HostResolver
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/07/2014.
 */
class UpdateReferencedMemcachedClientService(val dnsLookupService : HostResolver,
                                             val dnsConnectionTimeout : Duration,
                                             val memcachedConnectionFactory : ConnectionFactory,
                                             val delayBeforeOldClientClose : Duration) extends UpdateClientService {
  private val logger : Logger = LoggerFactory.getLogger(classOf[UpdateReferencedMemcachedClientService])

  val scheduledExecutor : ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
  val isShutdown = new AtomicBoolean(false)
  @volatile var referencedClient : ReferencedClient = UnavailableReferencedClient


  override def updateClientConnections(hosts: Seq[ElastiCacheHost]): Boolean = {
      val shutdown = isShutdown.get()
      if(shutdown) {
        return false
      }

      val currentClient: ReferencedClient = referencedClient
      if (hosts.size == 0) {
        logger.warn("No cache hosts available.  Marking cache as disabled.")
        referencedClient = UnavailableReferencedClient
        true
      } else {
        val resolvedHosts: Seq[InetSocketAddress] = getSocketAddresses(hosts);
        if (resolvedHosts.size == 0) {
          logger.warn("No resolvable cache hosts available.  Marking cache as disabled.")
          referencedClient = UnavailableReferencedClient
          true
        } else {
          logger.info("New client being created for new cache hosts.")

          referencedClient =  ReferencedClient(true, new MemcachedClient(memcachedConnectionFactory, convert(resolvedHosts)))

          if (currentClient.isAvailable) {
            logger.debug("Scheduling shutdown of old cache client in {}ms",delayBeforeOldClientClose.toMillis )
            scheduledExecutor.schedule(new Runnable {
              override def run(): Unit = {
                logger.info("Shutting down old cache client.")
                currentClient.client.shutdown();
              }
            }, delayBeforeOldClientClose.toMillis, TimeUnit.MILLISECONDS)
          }

          if(isShutdown.get() != shutdown) {
            shutdown
          }
          true
        }
      }

  }

  override def getClient: ReferencedClient = referencedClient

  override def shutdown: Unit = {
    isShutdown.set(true)
    var currentClient : ReferencedClient = referencedClient

    if(currentClient.isAvailable) {
      try {
        scheduledExecutor.shutdown()
        currentClient.client.shutdown()
      } finally {
        referencedClient = UnavailableReferencedClient
      }
    }
  }

  private def getSocketAddresses( hosts : Seq[ElastiCacheHost]) : Seq[InetSocketAddress] = {
    val resolvedHosts : ArrayBuffer[InetSocketAddress] = new ArrayBuffer[InetSocketAddress]()
    val size = hosts.size
    var i = 0

    while(i<size) {
      val host : ElastiCacheHost = hosts(i)
      if(host.hasIP) {
        val socketAddress = new InetSocketAddress(InetAddress.getByName(host.ip) , host.port);
        resolvedHosts.append(socketAddress)
      } else {
        val socketAddress : List[InetSocketAddress] = dnsLookupService.returnSocketAddressesForHostNames(List((host.hostName,host.port)),dnsConnectionTimeout)
        if(socketAddress.size==1) {
          resolvedHosts.append(socketAddress(0))
        }
      }
      i+=1
    }

    resolvedHosts
  }

  private def convert(seq: scala.collection.Seq[InetSocketAddress]) : java.util.List[InetSocketAddress] = {
    scala.collection.JavaConversions.seqAsJavaList(seq);
  }


}
