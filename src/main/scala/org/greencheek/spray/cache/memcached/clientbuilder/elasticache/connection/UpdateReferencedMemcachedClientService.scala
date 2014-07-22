package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import java.net.{InetSocketAddress, InetAddress}
import java.util.concurrent.atomic.AtomicReference

import net.spy.memcached.{MemcachedClient, ConnectionFactory}
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.HostResolver

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/07/2014.
 */
class UpdateReferencedMemcachedClientService(
  val dnsLookupService : HostResolver,
  val dnsConnectionTimeout : Duration,
  val memcachedConnectionFactory : ConnectionFactory

                                              ) extends UpdateClientService {

  val referencedClient : AtomicReference[ReferencedClient] = new AtomicReference[ReferencedClient](UnavailableReferencedClient)

  override def updateClientConnections(hosts: Seq[ElastiCacheHost]): Boolean = {
    val currentSetting : ReferencedClient = referencedClient.get()
    if(hosts.size == 0) {
       referencedClient.compareAndSet(currentSetting,UnavailableReferencedClient)
    } else {
      val resolvedHosts : Seq[InetSocketAddress] = getSocketAddresses(hosts);
      if(resolvedHosts.size==0) {
        referencedClient.compareAndSet(currentSetting,UnavailableReferencedClient)
      } else {
        referencedClient.compareAndSet(currentSetting,ReferencedClient(true,new MemcachedClient(memcachedConnectionFactory,convert(resolvedHosts))))
      }
    }
  }

  override def getClient: ReferencedClient = referencedClient.get


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
    }

    resolvedHosts
  }

  private def convert(seq: scala.collection.Seq[InetSocketAddress]) : java.util.List[InetSocketAddress] = {
    scala.collection.JavaConversions.seqAsJavaList(seq);
  }
}
