package org.greencheek.spray.cache.memcached.perftests.cacheobjects

import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.spray.cache.memcached.keyhashing.XXJavaHash
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.greencheek.spray.cache.memcached.perf.state.{LargeCacheObject, SmallCacheObject}
import org.greencheek.spy.extensions.FastSerializingTranscoder

/**
 * Created by dominictootell on 01/06/2014.
 */
object CacheFactory {
  def createSmallXXJavaTextCache : MemcachedCache[SmallCacheObject] = {
    new MemcachedCache[SmallCacheObject](
        memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
        maxCapacity = 10,
        keyHashType = XXJavaHash,
        protocol =  Protocol.TEXT
      )

  }

  def createSmallXXJavaTextXXHashCache : MemcachedCache[SmallCacheObject] = {
    new MemcachedCache[SmallCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT,
      hashAlgorithm = MemcachedCache.XXHASH_ALGORITHM
    )
  }

  def createLargeXXJavaTextCache : MemcachedCache[LargeCacheObject] = {
    new MemcachedCache[LargeCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT
    )
  }

  def createLargeXXJavaTextCacheWithFST : MemcachedCache[LargeCacheObject] = {
    new MemcachedCache[LargeCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT,
      serializingTranscoder =  new FastSerializingTranscoder
    )
  }

  def createAsciiOnlySmallXXJavaTextXXHashCache : MemcachedCache[SmallCacheObject] = {
    new MemcachedCache[SmallCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT,
      asciiOnlyKeys = true,
      hashAlgorithm = MemcachedCache.XXHASH_ALGORITHM
    )
  }
}
