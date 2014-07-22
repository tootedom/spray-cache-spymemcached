package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing

import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost

/**
 * Created by dominictootell on 22/07/2014.
 */
trait ElasticacheConfigParser {
  def parseServers(serversString : String ): Seq[ElastiCacheHost]
}
