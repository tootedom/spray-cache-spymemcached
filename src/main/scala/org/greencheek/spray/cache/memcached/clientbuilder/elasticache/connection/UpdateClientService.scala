package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost

/**
 * Created by dominictootell on 22/07/2014.
 */
trait UpdateClientService {
  def getClient : ReferencedClient
  def updateClientConnections(hosts: Seq[ElastiCacheHost]) : Boolean
  def shutdown : Unit
}
