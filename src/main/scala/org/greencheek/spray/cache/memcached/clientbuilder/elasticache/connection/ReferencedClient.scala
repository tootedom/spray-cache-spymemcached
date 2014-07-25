package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import net.spy.memcached.MemcachedClientIF

/**
 * Created by dominictootell on 22/07/2014.
 */
case class ReferencedClient(isAvailable : Boolean,
                            client : MemcachedClientIF)
