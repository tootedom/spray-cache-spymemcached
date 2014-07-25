package org.greencheek.spray.cache.memcached.clientbuilder.elasticache

import java.util.concurrent.TimeUnit

import net.spy.memcached.{ConnectionFactory, MemcachedClientIF}
import org.greencheek.elasticacheconfig.client.{ElastiCacheServerConnectionDetails, PeriodicConfigRetrievalClient, ConfigRetrievalSettingsBuilder, ConfigRetrievalSettings}
import org.greencheek.elasticacheconfig.confighandler.{AsyncExecutorServiceConfigInfoMessageHandler, ConfigInfoProcessor}
import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.DefaultElastiCacheConfigParser
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configprocessing.ElastiCacheConfigInfoProcessor
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection.{UpdateClientService, UpdateReferencedMemcachedClientService}
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.HostResolver

import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/07/2014.
 */
class ElastiCacheClientFactory(connnectionFactory : ConnectionFactory,
                               elastiCacheConfigHosts : Array[ElastiCacheServerConnectionDetails],
                               configPollingTime : Long,
                               initialConfigPollingDelay : Long,
                               configPollingTimeUnit : TimeUnit,
                               idleReadTimeout: Duration,
                               reconnectDelay: Duration,
                               delayBeforeClientClose : Duration,
                               dnsLookupService : HostResolver,
                               dnsLookupTimeout : Duration,
                               numberOfConsecutiveInvalidConfigurationsBeforeReconnect : Int,
                               connectionTimeoutInMillis : Int
                                ) extends ClientFactory {



  val memcachedClientHolder : UpdateClientService = new UpdateReferencedMemcachedClientService(dnsLookupService,
    dnsLookupTimeout,connnectionFactory,delayBeforeClientClose)

  val suppliedConfigInfoProcessor : ConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(DefaultElastiCacheConfigParser,memcachedClientHolder)
  val elastiCacheConfigPeriodicConfigRetrievalSettings : ConfigRetrievalSettings = createConfigRetrievalSettings()

  val configRetrievalClient = new PeriodicConfigRetrievalClient(elastiCacheConfigPeriodicConfigRetrievalSettings);
  configRetrievalClient.start()

  override def getClient(): MemcachedClientIF = {
    memcachedClientHolder.getClient.client
  }

  override def shutdown(): Unit = {
    configRetrievalClient.stop()
    memcachedClientHolder.shutdown
  }

  override def isEnabled(): Boolean = {
    memcachedClientHolder.getClient.isAvailable
  }


  private def createConfigRetrievalSettings() : ConfigRetrievalSettings = {
    val builder = new ConfigRetrievalSettingsBuilder()

    builder.addElastiCacheHosts(elastiCacheConfigHosts)
      .setConfigPollingTime(initialConfigPollingDelay,configPollingTime,configPollingTimeUnit)
      .setIdleReadTimeout(idleReadTimeout.toMillis,TimeUnit.MILLISECONDS)
      .setReconnectDelay(reconnectDelay.toMillis,TimeUnit.MILLISECONDS)
      .setNumberOfInvalidConfigsBeforeReconnect(numberOfConsecutiveInvalidConfigurationsBeforeReconnect)
      .setConfigInfoProcessor(suppliedConfigInfoProcessor)
      .setConnectionTimeoutInMillis(connectionTimeoutInMillis)

    builder.build()
  }
}
