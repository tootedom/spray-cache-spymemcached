package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configprocessing

import java.util.concurrent.atomic.AtomicLong

import org.greencheek.elasticacheconfig.confighandler.ConfigInfoProcessor
import org.greencheek.elasticacheconfig.domain.ConfigInfo
import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.ElasticacheConfigParser
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection.UpdateClientService
import org.slf4j.{LoggerFactory, Logger}


/**
 * Created by dominictootell on 22/07/2014.
 */
class ElasticCacheConfigInfoProcessor(val configParser : ElasticacheConfigParser,
                                      val updateClientService : UpdateClientService ) extends ConfigInfoProcessor{

  private val logger  : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])

  val currentConfigVersionNumber : AtomicLong = new AtomicLong(Long.MinValue)

  override def processConfig(info: ConfigInfo): Unit = {
    if(info.isValid) {
      val currentVersion = currentConfigVersionNumber.get();
      val latestConfigVersion = info.getVersion;

      if(latestConfigVersion>currentVersion) {
        updateClientService.updateClientConnections(configParser.parseServers(info.getServers))
      }

    } else {
      logger.debug("Invalid configuration provided for elasticache configuration")

    }

  }
}
