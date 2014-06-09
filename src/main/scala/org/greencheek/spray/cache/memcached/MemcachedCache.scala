package org.greencheek.spray.cache.memcached

import spray.caching.Cache
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import net.spy.memcached._
import java.net.{InetSocketAddress, InetAddress}
import org.slf4j.{LoggerFactory, Logger}
import java.util.concurrent.{TimeoutException, TimeUnit}
import net.spy.memcached.ConnectionFactoryBuilder.{Protocol, Locator}
import net.spy.memcached.transcoders.Transcoder
import org.greencheek.spy.extensions.{FastSerializingTranscoder, SerializingTranscoder}
import org.greencheek.dns.lookup.{TCPAddressChecker, AddressChecker, LookupService}
import scala.collection.JavaConversions._
import org.greencheek.spray.cache.memcached.keyhashing._
import scala.Some
import org.greencheek.spy.extensions.connection.CustomConnectionFactoryBuilder
import org.greencheek.spy.extensions.hashing.{JenkinsHash => JenkinsHashAlgo, AsciiXXHashAlogrithm, XXHashAlogrithm}
import org.greencheek.spray.cache.memcached.hostparsing.{CommaSeparatedHostAndPortStringParser, HostStringParser}
import org.greencheek.spray.cache.memcached.hostparsing.dnslookup.{AddressByNameHostResolver, HostResolver}
import org.greencheek.spray.cache.memcached.hostparsing.connectionchecking.{TCPHostValidation, HostValidation}
import scala.Some
import scala.Some

/*
 * Created by dominictootell on 26/03/2014.
 */
object MemcachedCache {
  private val DEFAULT_EXPIRY : Duration = Duration(60,TimeUnit.MINUTES)
  private val DEFAULT_CAPACITY = 1000
  private val ONE_SECOND = Duration(1,TimeUnit.SECONDS)

  val XXHASH_ALGORITHM: HashAlgorithm = new XXHashAlogrithm
  val JENKINS_ALGORITHM: HashAlgorithm = new JenkinsHashAlgo
  val DEFAULT_ALGORITHM: HashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH
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
                                   val hostConnectionAttemptTimeout : Duration = Duration(1,TimeUnit.SECONDS),
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
                                   val hostValidation : HostValidation = TCPHostValidation)
  extends Cache[Serializable] {

  @volatile private var isEnabled = false
  @volatile private var memcached: MemcachedClientIF = null;

  private val hashKeyPrefix = keyPrefix match {
    case None => false
    case Some(_ : String) => true
  }

  private val keyprefix = keyPrefix match {
    case None => ""
    case Some(prefix) => prefix
  }

  private var parsedHosts : List[(String,Int)] =  hostStringParser.parseMemcachedNodeList(memcachedHosts)
  parsedHosts match {
    case Nil => {
      if(throwExceptionOnNoHosts) {
        throw new InstantiationError()
      } else {
        isEnabled = false
      }
    }
    case hosts : List[(String,Int)] => {
      var addresses : List[InetSocketAddress] = hostHostResolver.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout)
      addresses match {
        case Nil => {
          if(throwExceptionOnNoHosts) {
            throw new InstantiationError()
          } else {
            isEnabled = false
          }
        }
        case resolvedHosts : List[InetSocketAddress] => {
          if(doHostConnectionAttempt) {
            addresses = hostValidation.validateMemcacheHosts(hostConnectionAttemptTimeout,resolvedHosts)
          }
        }
      }

      addresses match {
        case Nil => {
          isEnabled = false
        }
        case hostsToUse : List[InetSocketAddress] => {
          isEnabled = true
          val builder: ConnectionFactoryBuilder = keyValidationRequired(keyHashType) match {
            case true => new ConnectionFactoryBuilder();
            case false => new CustomConnectionFactoryBuilder();
          }
          builder.setHashAlg(createHashAlgorithm(asciiOnlyKeys,hashAlgorithm))
          builder.setLocatorType(hashingType)
          builder.setProtocol(protocol)
          builder.setReadBufferSize(readBufferSize)
          builder.setFailureMode(failureMode)
          builder.setTranscoder(serializingTranscoder)

          memcached = new MemcachedClient(builder.build(),hostsToUse)
        }

      }
    }

  }

  private def createHashAlgorithm(asciiOnlyKeys : Boolean, hashAlgorithm : HashAlgorithm) : HashAlgorithm = {
    hashAlgorithm match {
      case algo : XXHashAlogrithm => {
        if(asciiOnlyKeys) {
          new AsciiXXHashAlogrithm()
        } else {
          algo
        }
      }
      case algo : HashAlgorithm  => algo
    }
  }

  private def keyValidationRequired(keyHashType : KeyHashType ) : Boolean = {
    keyHashType match {
      case MD5KeyHash | MD5UpperKeyHash | MD5LowerKeyHash => false
      case SHA256KeyHash | SHA256UpperKeyHash | SHA256LowerKeyHash => false
      case XXJavaHash | XXNativeJavaHash => false
      case JenkinsHash => false
      case NoKeyHash => true
      case _ => true
    }
  }

  private val logger  : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])
  private val cachedHitMissLogger  : Logger  = LoggerFactory.getLogger("MemcachedCacheHitsLogger")

  require(maxCapacity >= 0, "maxCapacity must not be negative")

  private[cache] val store = new ConcurrentLinkedHashMap.Builder[String, Future[Serializable]]
    .initialCapacity(maxCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  private val keyHashingFunction : KeyHashing = keyHashType match {
    case MD5KeyHash | MD5UpperKeyHash => createMd5KeyHasher(asciiOnlyKeys,true)
    case SHA256KeyHash | SHA256UpperKeyHash => createShaKeyHasher(asciiOnlyKeys,true)
    case MD5LowerKeyHash => createMd5KeyHasher(asciiOnlyKeys,false)
    case SHA256LowerKeyHash => createShaKeyHasher(asciiOnlyKeys,false)
    case NoKeyHash => NoKeyHashing.INSTANCE
    case XXJavaHash => createXXKeyHasher(asciiOnlyKeys,false)
    case XXNativeJavaHash => createXXKeyHasher(asciiOnlyKeys,true)
    case JenkinsHash => JenkinsKeyHashing
    case _ => NoKeyHashing.INSTANCE
  }

  private def createXXKeyHasher(asciiOnlyKeys : Boolean, native : Boolean) : KeyHashing = {
    if(asciiOnlyKeys) {
      if(native) {
        AsciiXXKeyHashing.JNI_INSTANCE
      } else {
        AsciiXXKeyHashing.JAVA_INSTANCE
      }
    } else {
      if(native) {
        XXKeyHashing.JNI_INSTANCE
      } else {
        XXKeyHashing.JAVA_INSTANCE
      }
    }
  }

  private def createShaKeyHasher(asciiOnlyKeys : Boolean, uppercase : Boolean) : KeyHashing = {
    if (asciiOnlyKeys) {
      new AsciiSHA256DigestKeyHashing(upperCase = false)
    } else {
      new SHA256DigestKeyHashing(upperCase = false)
    }
  }

  private def createMd5KeyHasher(asciiOnlyKeys : Boolean, uppercase : Boolean) : KeyHashing = {
    if (asciiOnlyKeys) {
      new AsciiMD5DigestKeyHashing(upperCase = uppercase)
    } else {
      new MD5DigestKeyHashing(upperCase = uppercase)
    }
  }

  private def getHashedKey(key : String) : String = {
    if(hashKeyPrefix) {
      keyHashingFunction.hashKey(keyprefix + key)
    } else {
      keyHashingFunction.hashKey(key)
    }
  }

  private def logCacheHit(key: String): Unit = {
    cachedHitMissLogger.debug("{ \"cachehit\" : \"{}\"}",key)
  }

  private def logCacheMiss(key: String): Unit = {
    cachedHitMissLogger.debug("{ \"cachemiss\" : \"{}\"}",key)
  }

  private def getFromDistributedCache(key: String): Option[Future[Serializable]] = {
    try {
        val future =  memcached.asyncGet(key)
        val cacheVal = future.get(memcachedGetTimeout.toMillis,TimeUnit.MILLISECONDS)
        if(cacheVal==null){
            logCacheMiss(key)
            logger.debug("key {} not found in memcached", key)
            None
        } else {
            logCacheHit(key)
            logger.debug("key {} found in memcached", key)
            Some(Promise.successful(cacheVal.asInstanceOf[Serializable]).future)
        }
    } catch {
      case e : OperationTimeoutException => {
        logger.error("timeout when retrieving key {} from memcached",key)
        None
      }
      case e: Exception => {
        logger.error("Unable to contact memcached", e)
        None
      }
      case e: Throwable => {
        logger.error("Exception thrown when communicating with memcached", e)
        None
      }
    }
  }

  private def getDuration(timeToLive : Duration) : Duration = {
    timeToLive match {
      case Duration.Inf => Duration.Zero
      case Duration.MinusInf => Duration.Zero
      case Duration.Zero => Duration.Zero
      case x if x.lt(MemcachedCache.ONE_SECOND) => Duration.Zero
      case _  => timeToLive
    }
  }

  private def writeToDistributedCache(key: String, value : Serializable, timeToLive : Duration) : Unit = {
    val entryTTL : Duration = getDuration(timeToLive)

    if( waitForMemcachedSet ) {
      val futureSet = memcached.set(key, entryTTL.toSeconds.toInt, value)
      try {
        futureSet.get(setWaitDuration.toMillis, TimeUnit.MILLISECONDS)
      } catch {
        case e: Exception => {
          logger.warn("Exception waiting for memcached set to occur",e)
        }
      }
    } else {
      try {
        memcached.set(key, entryTTL.toSeconds.toInt, value)
      } catch {
        case e: Exception => {
          logger.warn("Exception waiting for memcached set to occur")
        }
      }
    }
  }

  def get(key: Any): Option[Future[Serializable]] = {
    val keyString : String = getHashedKey(key.toString)
    if(!isEnabled) {
      logCacheMiss(keyString)
      None
    } else {
      val future : Future[Serializable] = store.get(keyString)
      if(future==null) {
          getFromDistributedCache(keyString)
      }
      else {
          logCacheHit(keyString)
          Some(future)
      }
    }
  }


  override def apply(key: Any,genValue: () => Future[Serializable])(implicit ec: ExecutionContext): Future[Serializable] = {
    key match {
      case x: (_, _) if x._1.isInstanceOf[Duration] => {
        apply(x._1.asInstanceOf[Duration],x._2,genValue)
      }
      case x : (_,_) if x._2.isInstanceOf[Duration] => {
        apply(x._2.asInstanceOf[Duration],x._1,genValue)
      }
      case _ => {
        apply(timeToLive,key,genValue)
      }
    }
  }

  def apply(itemExpiry : Duration, key : Any, genValue: () => Future[Serializable])(implicit ec: ExecutionContext): Future[Serializable] = {
    val keyString = getHashedKey(key.toString)
    logger.info("put requested for {}", keyString)
    if(!isEnabled) {
      logCacheMiss(keyString)
      genValue()
    }
    else {
      // check local whilst computation is occurring cache.
      val existingFuture : Future[Serializable] = store.get(keyString)
      if(existingFuture==null) {
        // check memcached.
        getFromDistributedCache(keyString).getOrElse {
          // create and store a new future for the to be generated value
          val promise = Promise[Serializable]()
          val alreadyStoredFuture : Future[Serializable] = store.putIfAbsent(keyString, promise.future)
          if(alreadyStoredFuture == null) {
            cacheWriteFunction(genValue(), promise, keyString, itemExpiry, ec)
          } else {
            alreadyStoredFuture
          }
        }
      }
      else  {
          logCacheHit(keyString)
          existingFuture
      }
    }
  }

  /**
   * write to memcached when the future completes, the generated value,
   * against the given key, with the specified expiry
   * @param future  The future that will generate the value
   * @param promise The promise that is stored in the thurdering herd local cache
   * @param key The key against which to store an item
   * @param itemExpiry the expiry for the item
   * @return
   */
  private def cacheWriteFunction(future : Future[Serializable],promise: Promise[Serializable],
                                 key : String, itemExpiry : Duration,
                                 ec: ExecutionContext) : Future[Serializable] = {
    future.onComplete {
      value =>
        // Need to check memcached exception here
        try {
          if (!value.isFailure) {
            writeToDistributedCache(key,value.get,itemExpiry)
          }
        } catch {
          case e: Exception => {
            logger.error("problem setting key {} in memcached",key)
          }
        } finally {
          store.remove(key, promise.future)
          try {
            promise.complete(value)
          } catch {
            case e: IllegalStateException => {
              logger.error("future already completed for key {}",key)
            }
          }
        }
    }(ec)
    promise.future
  }

  def remove(key: Any) = {
    if(!isEnabled) {
      None
    }
    else {
      val keyString: String = getHashedKey(key.toString)
      val removedFuture: Option[Future[Serializable]] = store.remove(keyString) match {
        case null => None
        case x => Some(x)
      }

      val memcachedRemovedFuture = getFromDistributedCache(keyString) match {
        case None => {
          None
        }
        case future => {
          try {
            if(waitForMemcachedRemove) {
              val futureRemove = memcached.delete(keyString)
              try {
                futureRemove.get(removeWaitDuration.toMillis, TimeUnit.MILLISECONDS)
              } catch {
                case e: Exception => {
                  logger.warn("Exception waiting for memcached remove to occur")
                }
              }
            } else {
              memcached.delete(keyString)
            }
          } catch {
            case e: Exception => {
              logger.error("exception removing item {} from memcached", keyString)
            }
          }
          future
        }
      }

      removedFuture match {
        case None => {
          memcachedRemovedFuture
        }
        case Some(_) => {
          removedFuture
        }
      }
    }
  }

  def clear(): Unit = {
    if ( allowFlush ) {
      try {
        store.clear()
        memcached.flush()
      } catch {
        case e : Exception => {
          logger.error("Exception encountered when attempting to flush memcached")
        }
      }
    } else {
      throw new UnsupportedOperationException
    }
  }

  def size = {
    if(!isEnabled) {
      0
    }
    else {
      0
    }

  }

  def close()= {
    if(isEnabled) {
      store.clear();
      memcached.shutdown()
      isEnabled = false;
    }

  }

  def isCacheEnabled() : Boolean = {
    isEnabled
  }

}
