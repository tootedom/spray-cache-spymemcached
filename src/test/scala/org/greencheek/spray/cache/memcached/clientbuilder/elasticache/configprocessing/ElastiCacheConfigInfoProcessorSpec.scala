package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configprocessing

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import net.spy.memcached.ConnectionFactoryBuilder
import org.greencheek.elasticacheconfig.confighandler.ConfigInfoProcessor
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.DefaultElastiCacheConfigParser
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection.{UpdateReferencedMemcachedClientService, UnavailableReferencedClient, ReferencedClient, UpdateClientService}
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.AddressByNameHostResolver
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.greencheek.elasticacheconfig.domain.ConfigInfo
import org.specs2.runner.JUnitRunner

import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 23/07/2014.
 */
@RunWith(classOf[JUnitRunner])
class ElastiCacheConfigInfoProcessorSpec extends Specification {

  "Test new configuration version results in config change" in {
    val counter = new AtomicInteger(0)
    val configUpdateService : UpdateClientService = new CountingUpdateClientService(counter)

    val processor : ConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(DefaultElastiCacheConfigParser,configUpdateService,true)

    processor.processConfig(new ConfigInfo("header", 1,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 3,"localhost",false))
    processor.processConfig(new ConfigInfo("header", 4,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",false))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",true))

    counter.get must beEqualTo(3)
  }

  "Test unresolved host does not result in config change" in {
    val counter = new AtomicInteger(0)
    val configUpdateService: UpdateClientService = new CountingAndResolvingClientService(counter)

    try {

      val processor: ConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(DefaultElastiCacheConfigParser, configUpdateService, false)

      processor.processConfig(new ConfigInfo("header", 1, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 3, "unknownhost.123.com||11211", true))
      processor.processConfig(new ConfigInfo("header", 3, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 4, "localhost|127.0.0.1|11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", false))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))

      counter.get must beEqualTo(5)
    }
    finally {
      configUpdateService.shutdown
    }
  }

  "Test unresolved host does can result in config change" in {
    val counter = new AtomicInteger(0)
    val configUpdateService: UpdateClientService = new CountingAndResolvingClientService(counter)

    try {

      val processor: ConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(DefaultElastiCacheConfigParser, configUpdateService, true)

      processor.processConfig(new ConfigInfo("header", 1, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))
      processor.processConfig(new ConfigInfo("header", 3, "unknownhost.123.com||11211", false))
      processor.processConfig(new ConfigInfo("header", 3, "localhost||11211", false))
      processor.processConfig(new ConfigInfo("header", 4, "localhost|127.0.0.1|11211", true))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", false))
      processor.processConfig(new ConfigInfo("header", 2, "localhost||11211", true))

      counter.get must beEqualTo(3)
    }
    finally {
      configUpdateService.shutdown
    }
  }


  class CountingAndResolvingClientService(counter : AtomicInteger) extends UpdateReferencedMemcachedClientService(
    AddressByNameHostResolver,
    Duration(3,TimeUnit.SECONDS),
    new ConnectionFactoryBuilder().build(),
    Duration(1,TimeUnit.SECONDS)) {

    @volatile var currentClient : ReferencedClient = null

    override def updateClientConnections(hosts: Seq[ElastiCacheHost]): ReferencedClient = {
      val updatedClient = super.updateClientConnections(hosts)
      if(updatedClient != currentClient) {
        counter.incrementAndGet()
      }
      updatedClient
    }

  }

  class CountingUpdateClientService(counter : AtomicInteger) extends UpdateClientService {
    override def updateClientConnections(hosts: Seq[ElastiCacheHost]): ReferencedClient = {
      counter.incrementAndGet()
      null
    }

    override def shutdown: Unit = {

    }

    override def getClient: ReferencedClient = {
      UnavailableReferencedClient
    }
  }

}


