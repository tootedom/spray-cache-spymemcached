package org.greencheek.spray.cache.memcached.perftests.cacheobjects

import org.greencheek.spray.cache.memcached.MemcachedCache
import org.greencheek.spray.cache.memcached.keyhashing.{XXNativeJavaHash, XXJavaHash}
import net.spy.memcached.ConnectionFactoryBuilder.Protocol
import org.greencheek.spray.cache.memcached.perf.state.{MediumCacheObject, LargeCacheObject, SmallCacheObject}
import org.greencheek.spy.extensions.{SerializingTranscoder, FastSerializingTranscoder}
import scala.collection.JavaConversions._

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

  def createSmallXXNativeJavaTextCache : MemcachedCache[SmallCacheObject] = {
    new MemcachedCache[SmallCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXNativeJavaHash,
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

  def createMediumXXJavaTextCache : MemcachedCache[MediumCacheObject] = {
    new MemcachedCache[MediumCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT
    )
  }

  def createMediumCompressedXXJavaTextCache : MemcachedCache[MediumCacheObject] = {
    new MemcachedCache[MediumCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT,
      serializingTranscoder = new FastSerializingTranscoder(SerializingTranscoder.MAX_CONTENT_SIZE_IN_BYTES,4096)
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

  def createLargeXXJavaTextCacheWithFSTKnownClasses : MemcachedCache[LargeCacheObject] = {
    new MemcachedCache[LargeCacheObject](
      memcachedHosts = System.getProperty("memcached.hosts","localhost:11211"),
      maxCapacity = 10,
      keyHashType = XXJavaHash,
      protocol =  Protocol.TEXT,
      serializingTranscoder =  new FastSerializingTranscoder(FastSerializingTranscoder.DEFAULT_SHARE_REFERENCES,Array[Class[_]](classOf[LargeCacheObject]))

       //new FastSerializingTranscoder(false,Array(classOf[LargeCacheObject]))
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
