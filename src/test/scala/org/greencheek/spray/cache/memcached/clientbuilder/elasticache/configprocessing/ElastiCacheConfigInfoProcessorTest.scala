package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configprocessing

import java.util.concurrent.atomic.AtomicInteger

import org.greencheek.elasticacheconfig.confighandler.ConfigInfoProcessor
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.DefaultElastiCacheConfigParser
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection.{UnavailableReferencedClient, ReferencedClient, UpdateClientService}
import org.specs2.mutable.Specification
import org.greencheek.elasticacheconfig.domain.ConfigInfo
/**
 * Created by dominictootell on 23/07/2014.
 */
class ElastiCacheConfigInfoProcessorTest extends Specification {

  "Test new configuration version results in config change" in {
    val counter = new AtomicInteger(0)
    val configUpdateService : UpdateClientService = new CountingUpdateClientService(counter)

    val processor : ConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(DefaultElastiCacheConfigParser,configUpdateService)

    processor.processConfig(new ConfigInfo("header", 1,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 3,"localhost",false))
    processor.processConfig(new ConfigInfo("header", 4,"localhost",true))
    processor.processConfig(new ConfigInfo("header", 2,"localhost",false))

    counter.get must beEqualTo(3)
  }


  class CountingUpdateClientService(counter : AtomicInteger) extends UpdateClientService {
    override def updateClientConnections(hosts: Seq[ElastiCacheHost]): Boolean = {
      counter.incrementAndGet()
      true
    }

    override def shutdown: Unit = {

    }

    override def getClient: ReferencedClient = {
      UnavailableReferencedClient
    }
  }
}


