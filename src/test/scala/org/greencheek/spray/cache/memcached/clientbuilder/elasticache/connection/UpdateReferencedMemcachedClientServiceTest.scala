package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import java.util.concurrent.{CountDownLatch, TimeUnit}

import net.spy.memcached.ops.Operation
import net.spy.memcached.{MemcachedNode, BroadcastOpFactory, ConnectionFactoryBuilder}
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.AddressByNameHostResolver
import org.greencheek.util.memcached.{MemcachedBasedSpec, WithMemcached}
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 23/07/2014.
 */
class UpdateReferencedMemcachedClientServiceTest extends MemcachedBasedSpec {

  override def useBinary = false

  val memcachedContext = WithMemcached(false)


  "Client Reference should be updated" in memcachedContext {
    val cf = new ConnectionFactoryBuilder().build()
    val clientService = new UpdateReferencedMemcachedClientService(AddressByNameHostResolver,Duration(3,TimeUnit.SECONDS),cf,Duration(1,TimeUnit.SECONDS))

    clientService.getClient.isAvailable must beFalse

    clientService.updateClientConnections(Seq(ElastiCacheHost("localhost","",memcachedContext.memcached.port,false)))

    var client = clientService.getClient.client

    clientService.getClient.isAvailable must beTrue
    clientService.updateClientConnections(Seq(ElastiCacheHost("localhost","127.0.0.1",memcachedDport,true)))

    // Previous client will be shutdown in 1 seconds.
    Thread.sleep(3000)
    client.shutdown(1,TimeUnit.SECONDS) must beFalse


    clientService.getClient.isAvailable must beTrue
    client must not beTheSameAs(clientService.getClient.client)

    client = clientService.getClient.client
    client must beTheSameAs(clientService.getClient.client)
    clientService.updateClientConnections(Seq(ElastiCacheHost("localhost","127.0.0.1",memcachedContext.memcached.port,false)))

    // Previous client will be shutdown in 1 seconds.
    Thread.sleep(3000)
    client.shutdown(1,TimeUnit.SECONDS) must beFalse
    client must not beTheSameAs(clientService.getClient.client)

    client = clientService.getClient.client

    clientService.shutdown
    clientService.getClient must beTheSameAs(UnavailableReferencedClient)

    client.shutdown(1,TimeUnit.SECONDS) must beFalse

  }
}
