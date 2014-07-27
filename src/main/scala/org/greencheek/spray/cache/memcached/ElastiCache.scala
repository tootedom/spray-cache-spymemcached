package org.greencheek.spray.cache.memcached

import org.greencheek.elasticacheconfig.client.{LocalhostElastiCacheServerConnectionDetails, ElastiCacheServerConnectionDetails}
import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory
import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheClientFactory
import org.greencheek.spray.cache.memcached.hostparsing.{CommaSeparatedHostAndPortStringParser, HostStringParser}
import org.greencheek.spray.cache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder
import spray.caching.Cache
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ ExecutionContext, Future}
import scala.concurrent.duration.Duration
import net.spy.memcached._
import java.util.concurrent.TimeUnit
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol, Locator}
import net.spy.memcached.transcoders.Transcoder
import org.greencheek.spy.extensions.FastSerializingTranscoder
import org.greencheek.spray.cache.memcached.keyhashing._
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.{AddressByNameHostResolver, HostResolver}
/**
 * Created by dominictootell on 23/07/2014.
 */
class ElastiCache[Serializable](val timeToLive: Duration = MemcachedCache.DEFAULT_EXPIRY,
                                val maxCapacity: Int = MemcachedCache.DEFAULT_CAPACITY,
                                val elastiCacheConfigHosts : String = "localhost:11211",
                                val configPollingTime : Long = 60,
                                val initialConfigPollingDelay : Long = 0,
                                val connectionTimeoutInMillis : Int = 3000,
                                val configPollingTimeUnit : TimeUnit = TimeUnit.SECONDS,
                                val idleReadTimeout: Duration = Duration(125,TimeUnit.SECONDS),
                                val reconnectDelay: Duration = Duration(5,TimeUnit.SECONDS),
                                val delayBeforeClientClose : Duration = Duration(10,TimeUnit.SECONDS),
                                val numberOfConsecutiveInvalidConfigurationsBeforeReconnect : Int = 3,
                                val hashingType : Locator = Locator.CONSISTENT,
                                val failureMode : FailureMode = FailureMode.Redistribute,
                                val hashAlgorithm : HashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH,
                                val serializingTranscoder : Transcoder[Object] = new FastSerializingTranscoder(),
                                val protocol : ConnectionFactoryBuilder.Protocol = Protocol.BINARY,
                                val readBufferSize : Int = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
                                val memcachedGetTimeout : Duration = Duration(2500,TimeUnit.MILLISECONDS),
                                val dnsConnectionTimeout : Duration = Duration(3,TimeUnit.SECONDS),
                                val updateConfigVersionOnDnsTimeout : Boolean = true,
                                val waitForMemcachedSet : Boolean = false,
                                val setWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                val allowFlush : Boolean = false,
                                val waitForMemcachedRemove : Boolean = false,
                                val removeWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                val keyHashType : KeyHashType = NoKeyHash,
                                val keyPrefix : Option[String] = None,
                                val asciiOnlyKeys : Boolean = false,
                                val hostResolver : HostResolver = AddressByNameHostResolver,
                                val useStaleCache : Boolean = false,
                                val staleCacheAdditionalTimeToLive : Duration = Duration.MinusInf,
                                val staleCachePrefix  : String = "stale",
                                val staleMaxCapacity : Int = -1,
                                val staleCacheMemachedGetTimeout : Duration = Duration.MinusInf)
  extends Cache[Serializable] {



  val clientFactory : ClientFactory = new ElastiCacheClientFactory(
    connnectionFactory = SpyConnectionFactoryBuilder.createConnectionFactory(
      hashingType = hashingType,
      failureMode = failureMode,
      hashAlgorithm = hashAlgorithm,
      serializingTranscoder = serializingTranscoder,
      protocol  = protocol,
      readBufferSize = readBufferSize,
      keyHashType = keyHashType
    ),
    elastiCacheConfigHosts = parseElastiCacheConfigHosts(elastiCacheConfigHosts),
    configPollingTime = configPollingTime,
    initialConfigPollingDelay = initialConfigPollingDelay,
    configPollingTimeUnit = configPollingTimeUnit,
    idleReadTimeout = idleReadTimeout,
    reconnectDelay = reconnectDelay,
    delayBeforeClientClose = delayBeforeClientClose,
    dnsLookupService = hostResolver,
    dnsLookupTimeout = dnsConnectionTimeout,
    updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout,
    numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect,
    connectionTimeoutInMillis = connectionTimeoutInMillis
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

  private def parseElastiCacheConfigHosts(hostsString : String) : Array[ElastiCacheServerConnectionDetails] = {
    val hostStringParser : HostStringParser =  CommaSeparatedHostAndPortStringParser
    var hosts : String = null
    if(hostsString == null || hostsString.trim.length == 0) {
      hosts = "localhost:11211"
    } else {
      hosts = hostsString
    }

    val parsedHosts = hostStringParser.parseMemcachedNodeList(hosts)

    val connectionDetails = if(parsedHosts.size>0) {
      new Array[ElastiCacheServerConnectionDetails](parsedHosts.size)
    } else {
      new Array[ElastiCacheServerConnectionDetails](0)
    }

    var i : Int = 0
    while(i<parsedHosts.size) {
      val details : (String,Int) = parsedHosts(i)
      connectionDetails(i) = new ElastiCacheServerConnectionDetails(details._1,details._2)
      i+=1
    }

    if(parsedHosts.size==0) {
      connectionDetails(0) = new LocalhostElastiCacheServerConnectionDetails
    }

    connectionDetails
  }

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

  def close() = {
    baseMemcachedCached.close()

  }
}

