package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.connection

import java.net.InetSocketAddress

import net.spy.memcached.MemcachedClientIF

/**
 * Created by dominictootell on 22/07/2014.
 */
case class ReferencedClient(isAvailable : Boolean,
                            resolvedHosts : Seq[InetSocketAddress],
                            client : MemcachedClientIF)
