package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configprocessing

import java.util.concurrent.atomic.AtomicLong

import org.greencheek.elasticacheconfig.confighandler.ConfigInfoProcessor
import org.greencheek.elasticacheconfig.domain.ConfigInfo
import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.ElastiCacheConfigParser
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection.UpdateClientService
import org.slf4j.{LoggerFactory, Logger}


/**
 * Created by dominictootell on 22/07/2014.
 */
class ElastiCacheConfigInfoProcessor(val configParser : ElastiCacheConfigParser,
                                      val updateClientService : UpdateClientService ) extends ConfigInfoProcessor{

  private val logger  : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])

  @volatile var currentConfigVersionNumber : Long = Long.MinValue

  override def processConfig(info: ConfigInfo): Unit = {
    if(info.isValid) {
      val currentVersion = currentConfigVersionNumber;
      val latestConfigVersion = info.getVersion;

      if(latestConfigVersion>currentVersion) {
        logger.info("Configuration version has increased.  Reconfiguring client")
        updateClientService.updateClientConnections(configParser.parseServers(info.getServers))
      }
      else if(latestConfigVersion==currentVersion) {
        logger.info("Configuration is up to date")
      } else {
        logger.warn("Supplied Configuration had a version lower than current configuration")
      }

    } else {
      logger.debug("Invalid configuration provided for elasticache configuration")

    }

  }
}
