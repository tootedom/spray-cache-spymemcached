package org.greencheek.elasticacheconfig.client;

import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing.DefaultElastiCacheConfigParser;

/**
 * Created by dominictootell on 25/07/2014.
 */
public class LocalhostElastiCacheServerConnectionDetails extends ElastiCacheServerConnectionDetails {

    public LocalhostElastiCacheServerConnectionDetails() {
        super("localhost", 11211);
    }
}
