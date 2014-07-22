package org.greencheek.spray.cache.memcached.clientbuilder.elasticache

import net.spy.memcached.{ConnectionFactory, MemcachedClientIF}
import org.greencheek.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler
import org.greencheek.elasticacheconfig.handler.RequestConfigInfoScheduler
import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory

import scala.concurrent.duration.Duration

/**
 * Created by dominictootell on 22/07/2014.
 */
class ElasticacheClientFactory(
                                connnectionFactory : ConnectionFactory,
                                elasticacheConfigHost: String,
                                elasticacheConfigPort: Int,
                                configPolling : Duration,
                                idleReadTimeout: Duration,
                                reconnectDelay: Duration,
                                numberOfInvalidConfigsBeforeReconnect: Int
                                ) extends ClientFactory {





  override def getClient(): MemcachedClientIF = ???

  override def shutdown(): Unit = ???

  override def isEnabled(): Boolean = ???
}
