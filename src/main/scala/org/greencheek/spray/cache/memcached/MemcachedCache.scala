package org.greencheek.spray.cache.memcached

import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory
import org.greencheek.spray.cache.memcached.clientbuilder.staticclient.SpyMemcachedClientFactory
import org.greencheek.spray.cache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder
import spray.caching.Cache
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import net.spy.memcached._
import java.net.InetSocketAddress
import org.slf4j.{LoggerFactory, Logger}
import java.util.concurrent.{TimeoutException, TimeUnit}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol, Locator}
import net.spy.memcached.transcoders.Transcoder
import org.greencheek.spy.extensions.FastSerializingTranscoder
import scala.collection.JavaConversions._
import org.greencheek.spray.cache.memcached.keyhashing._
import org.greencheek.spy.extensions.connection.CustomConnectionFactoryBuilder
import org.greencheek.spy.extensions.hashing.{JenkinsHash => JenkinsHashAlgo, AsciiXXHashAlogrithm, XXHashAlogrithm}
import org.greencheek.spray.cache.memcached.hostparsing.{CommaSeparatedHostAndPortStringParser, HostStringParser}
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.{AddressByNameHostResolver, HostResolver}
import org.greencheek.spray.cache.memcached.hostparsing.connectionchecking.{TCPHostValidation, HostValidation}
import net.spy.memcached.internal.CheckedOperationTimeoutException

/*
 * Created by dominictootell on 26/03/2014.
 */
object MemcachedCache {
  val DEFAULT_EXPIRY : Duration = Duration(60,TimeUnit.MINUTES)
  val DEFAULT_CAPACITY = 1000
  val ONE_SECOND = Duration(1,TimeUnit.SECONDS)

  val XXHASH_ALGORITHM: HashAlgorithm = new XXHashAlogrithm
  val JENKINS_ALGORITHM: HashAlgorithm = new JenkinsHashAlgo
  val DEFAULT_ALGORITHM: HashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH

  val CACHE_TYPE_VALUE_CALCULATION : String = "value_calculation_cache"
  val CACHE_TYPE_CACHE_DISABLED : String = "disabled_cache"
  val CACHE_TYPE_STALE_CACHE : String = "stale_distributed_cache"
  val CACHE_TYPE_DISTRIBUTED_CACHE : String = "distributed_cache"

}


class MemcachedCache[Serializable](val timeToLive: Duration = MemcachedCache.DEFAULT_EXPIRY,
                                   val maxCapacity: Int = MemcachedCache.DEFAULT_CAPACITY,
                                   val memcachedHosts : String = "localhost:11211",
                                   val hashingType : Locator = Locator.CONSISTENT,
                                   val failureMode : FailureMode = FailureMode.Redistribute,
                                   val hashAlgorithm : HashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH,
                                   val serializingTranscoder : Transcoder[Object] = new FastSerializingTranscoder(),
                                   val protocol : ConnectionFactoryBuilder.Protocol = Protocol.BINARY,
                                   val readBufferSize : Int = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
                                   val memcachedGetTimeout : Duration = Duration(2500,TimeUnit.MILLISECONDS),
                                   val throwExceptionOnNoHosts : Boolean = false,
                                   val dnsConnectionTimeout : Duration = Duration(3,TimeUnit.SECONDS),
                                   val doHostConnectionAttempt : Boolean = false,
                                   val hostConnectionAttemptTimeout : Duration = MemcachedCache.ONE_SECOND,
                                   val waitForMemcachedSet : Boolean = false,
                                   val setWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                   val allowFlush : Boolean = false,
                                   val waitForMemcachedRemove : Boolean = false,
                                   val removeWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                   val keyHashType : KeyHashType = NoKeyHash,
                                   val keyPrefix : Option[String] = None,
                                   val asciiOnlyKeys : Boolean = false,
                                   val hostStringParser : HostStringParser = CommaSeparatedHostAndPortStringParser,
                                   val hostHostResolver : HostResolver = AddressByNameHostResolver,
                                   val hostValidation : HostValidation = TCPHostValidation,
                                   val useStaleCache : Boolean = false,
                                   val staleCacheAdditionalTimeToLive : Duration = Duration.MinusInf,
                                   val staleCachePrefix  : String = "stale",
                                   val staleMaxCapacity : Int = -1,
                                   val staleCacheMemachedGetTimeout : Duration = Duration.MinusInf)
  extends Cache[Serializable] {


  val clientFactory : ClientFactory = new SpyMemcachedClientFactory(
    memcachedHosts = memcachedHosts,
    throwExceptionOnNoHosts = throwExceptionOnNoHosts,
    dnsConnectionTimeout = dnsConnectionTimeout,
    doHostConnectionAttempt = doHostConnectionAttempt,
    hostConnectionAttemptTimeout = hostConnectionAttemptTimeout,
    hostStringParser = hostStringParser,
    hostHostResolver = hostHostResolver,
    hostValidation = hostValidation,
    connnectionFactory = SpyConnectionFactoryBuilder.createConnectionFactory(
      hashingType = hashingType,
      failureMode = failureMode,
      hashAlgorithm = hashAlgorithm,
      serializingTranscoder = serializingTranscoder,
      protocol  = protocol,
      readBufferSize = readBufferSize,
      keyHashType = keyHashType
    )
  )

  val baseMemcachedCached = new BaseMemcachedCache[Serializable](
    clientFactory = clientFactory,
    timeToLive = timeToLive,
    maxCapacity = maxCapacity,
    memcachedGetTimeout = memcachedGetTimeout,
    waitForMemcachedSet  = waitForMemcachedSet,
    setWaitDuration = setWaitDuration,
    allowFlush = allowFlush,
    waitForMemcachedRemove = waitForMemcachedRemove,
    removeWaitDuration = removeWaitDuration,
    keyHashType = keyHashType,
    keyPrefix  = keyPrefix,
    asciiOnlyKeys = asciiOnlyKeys,
    useStaleCache = useStaleCache,
    staleCacheAdditionalTimeToLive = staleCacheAdditionalTimeToLive,
    staleCachePrefix  = staleCachePrefix,
    staleMaxCapacity = staleMaxCapacity,
    staleCacheMemachedGetTimeout = staleCacheMemachedGetTimeout
  )



  override def get(key: Any): Option[Future[Serializable]] = {
    baseMemcachedCached.get(key)
  }


  override def apply(key: Any,genValue: () => Future[Serializable])(implicit ec: ExecutionContext): Future[Serializable] = {
    baseMemcachedCached.apply(key,genValue)
  }


  override def remove(key: Any) : Option[Future[Serializable]] = {
   baseMemcachedCached.remove(key)

  }

  override def clear(): Unit = {
    baseMemcachedCached.clear()
  }

  override def size = {
    baseMemcachedCached.size
  }

  def close()= {
    baseMemcachedCached.close()

  }
}
