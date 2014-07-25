package org.greencheek.spray.cache.memcached;


import org.greencheek.spray.cache.memcached.clientbuilder.ClientFactory
import spray.caching.Cache
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.concurrent.duration.Duration
import net.spy.memcached._
import org.slf4j.{LoggerFactory, Logger}
import java.util.concurrent.{TimeoutException, TimeUnit}
import org.greencheek.spray.cache.memcached.keyhashing._
import net.spy.memcached.internal.CheckedOperationTimeoutException


class BaseMemcachedCache[Serializable](
                                   val clientFactory : ClientFactory,
                                   val timeToLive: Duration = MemcachedCache.DEFAULT_EXPIRY,
                                   val maxCapacity: Int = MemcachedCache.DEFAULT_CAPACITY,
                                   val memcachedGetTimeout : Duration = Duration(2500,TimeUnit.MILLISECONDS),
                                   val waitForMemcachedSet : Boolean = false,
                                   val setWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                   val allowFlush : Boolean = false,
                                   val waitForMemcachedRemove : Boolean = false,
                                   val removeWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                   val keyHashType : KeyHashType = NoKeyHash,
                                   val keyPrefix : Option[String] = None,
                                   val asciiOnlyKeys : Boolean = false,
                                   val useStaleCache : Boolean = false,
                                   val staleCacheAdditionalTimeToLive : Duration = Duration.MinusInf,
                                   val staleCachePrefix  : String = "stale",
                                   val staleMaxCapacity : Int = -1,
                                   val staleCacheMemachedGetTimeout : Duration = Duration.MinusInf)
  extends Cache[Serializable] {

  def getMemcachedClient() : MemcachedClientIF = clientFactory.getClient()
  def isEnabled() : Boolean = clientFactory.isEnabled()

  private val hashKeyPrefix : Boolean = keyPrefix match {
    case None => false
    case Some(_ : String) => true
  }

  private val keyprefix = keyPrefix match {
    case None => ""
    case Some(prefix) => prefix
  }

  private val logger  : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])
  private val cacheHitMissLogger  : Logger  = LoggerFactory.getLogger("MemcachedCacheHitsLogger")

  require(maxCapacity >= 0, "maxCapacity must not be negative")

  private[cache] val store = new ConcurrentLinkedHashMap.Builder[String, Future[Serializable]]
    .initialCapacity(maxCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  private val staleMaxCapacityValue : Int = if (staleMaxCapacity == -1) maxCapacity else staleMaxCapacityValue
  private val staleCacheAdditionalTimeToLiveValue : Duration = if (staleCacheAdditionalTimeToLive == Duration.MinusInf) timeToLive else staleCacheAdditionalTimeToLive

  private[cache] val staleStore = useStaleCache match {
    case true => {
      new ConcurrentLinkedHashMap.Builder[String, Future[Serializable]]
        .initialCapacity(staleMaxCapacityValue)
        .maximumWeightedCapacity(staleMaxCapacityValue)
        .build()
    }
    case false => null
  }

  private val keyHashingFunction : KeyHashing = KeyHashingFactory.createKeyHashing(keyHashType,asciiOnlyKeys)

  private val setWaitDurationInMillis = setWaitDuration.toMillis
  private val memcachedGetTimeoutMillis = memcachedGetTimeout.toMillis
  private val staleCacheMemachedGetTimeoutMillis = staleCacheMemachedGetTimeout match {
    case Duration.MinusInf => memcachedGetTimeoutMillis
    case _ => staleCacheMemachedGetTimeout.toMillis
  }

  private def getHashedKey(key : String) : String = {
    if(hashKeyPrefix) {
      keyHashingFunction.hashKey(keyprefix + key)
    } else {
      keyHashingFunction.hashKey(key)
    }
  }

  private def logCacheHit(key: Any, cacheType: Any): Unit = {
    cacheHitMissLogger.debug("{ \"cachehit\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType)
  }

  private def logCacheMiss(key: Any, cacheType: Any): Unit = {
    cacheHitMissLogger.debug("{ \"cachemiss\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType)
  }

  private def warnCacheDisabled() : Unit = {
    logger.warn("Cache is disabled")
  }



  private def logWarn(msg : String, item1 : Any, item2 : Any) : Unit = {
    logger.warn(msg,item1,item2)
  }

  private def getDuration(timeToLive : Duration) : Long = {
    if(timeToLive == Duration.Inf) {
      0
    }
    else if(timeToLive == null) {
      0
    } else {
      val timeToLiveSec : Long = timeToLive.toSeconds
      if(timeToLiveSec >= 1l) {
        timeToLiveSec
      } else {
        0
      }
    }


  }

  private def writeToDistributedCache(key: String, value : Serializable,
                                      timeToLive : Duration, waitForMemcachedSet : Boolean) : Unit = {
    val entryTTL : Long = getDuration(timeToLive)

    if( waitForMemcachedSet ) {
      val futureSet = getMemcachedClient().set(key, entryTTL.toInt, value)
      try {
        futureSet.get(setWaitDurationInMillis, TimeUnit.MILLISECONDS)
      } catch {
        case e: Exception => {
          logger.warn("Exception waiting for memcached set to occur",e)
        }
      }
    } else {
      try {
        getMemcachedClient().set(key, entryTTL.toInt, value)
      } catch {
        case e: Exception => {
          logger.warn("Exception waiting for memcached set to occur")
        }
      }
    }
  }

  def get(key: Any): Option[Future[Serializable]] = {
    val keyString : String = getHashedKey(key.toString)
    if(!isEnabled()) {
      logCacheMiss(keyString,MemcachedCache.CACHE_TYPE_CACHE_DISABLED)
      warnCacheDisabled()
      None
    } else {
      val future : Future[Serializable] = store.get(keyString)
      if(future==null) {
        getOptionFromDistributedCache(keyString)
      }
      else {
        logCacheHit(keyString,MemcachedCache.CACHE_TYPE_VALUE_CALCULATION)
        if(useStaleCache) {
          Some(getFutueForStaleDistributedCacheLookup(createStaleCacheKey(keyString), future, null))
        } else {
          Some(future)
        }
      }
    }
  }


  override def apply(key: Any,genValue: () => Future[Serializable])(implicit ec: ExecutionContext): Future[Serializable] = {
    if(key.isInstanceOf[Tuple2[_,_]]) {
      key match {
        case x: (_, _) if x._1.isInstanceOf[Duration] => {
          apply(x._1.asInstanceOf[Duration], x._2, genValue)
        }
        case x: (_, _) if x._2.isInstanceOf[Duration] => {
          apply(x._2.asInstanceOf[Duration], x._1, genValue)
        }
        case _ => {
          apply(timeToLive, key, genValue)
        }
      }
    } else {
      apply(timeToLive, key, genValue)
    }
  }

  def apply(itemExpiry : Duration, key : Any, genValue: () => Future[Serializable])(implicit ec: ExecutionContext): Future[Serializable] = {
    val keyToString : String = key.toString
    val keyString = getHashedKey(keyToString)

    if(!isEnabled()) {
      logCacheMiss(keyString,MemcachedCache.CACHE_TYPE_CACHE_DISABLED)
      warnCacheDisabled()
      genValue()
    }
    else {
      var staleCacheKey : String = null
      var staleCacheExpiry : Duration = null;
      if(useStaleCache) {
        staleCacheKey = createStaleCacheKey(keyString)
        staleCacheExpiry = itemExpiry.plus(staleCacheAdditionalTimeToLiveValue)
      }

      val promise = Promise[Serializable]()
      // create and store a new future for the to be generated value
      // first checking against local a cache to see if the computation is already
      // occurring

      val existingFuture : Future[Serializable] = store.putIfAbsent(keyString, promise.future)
//      val existingFuture : Future[Serializable] = store.get(keyString)
      if(existingFuture==null) {
        // check memcached.
        val cachedObject = getFromDistributedCache(keyString)
        if(cachedObject == null)
        {
            logger.debug("set requested for {}", keyString)
            cacheWriteFunction(genValue(), promise, keyString, staleCacheKey, itemExpiry,staleCacheExpiry,ec)
        }
        else {
          store.remove(keyString,promise.future)
          promise.success(cachedObject.asInstanceOf[Serializable])
          promise.future
        }
      }
      else  {
        if(useStaleCache) {
          getFutueForStaleDistributedCacheLookup(staleCacheKey,existingFuture,ec)
        } else {
          logCacheHit(keyString,MemcachedCache.CACHE_TYPE_VALUE_CALCULATION)
          existingFuture
        }
      }
    }
  }

  private def createStaleCacheKey(key : String) : String = {
    staleCachePrefix + key
  }

  /**
   * Talks to memcached to find a cached entry. If the entry does not exist, the backend Future will
   * be 'consulted' and it's value with be returned.
   *
   * @param key The cache key to lookup
   * @param promise the promise on which requests are waiting.
   * @param backendFuture the future that is running the long returning calculation that creates a fresh entry.
   */
  private def getFromStaleDistributedCache(key : String,
                                           promise : Promise[Serializable],
                                           backendFuture : Future[Serializable]) : Unit = {

    val item = getFromDistributedCache(key,staleCacheMemachedGetTimeoutMillis,MemcachedCache.CACHE_TYPE_STALE_CACHE)

    if(item==null) {
      promise.completeWith(backendFuture)
    } else {
      promise.success(item.asInstanceOf[Serializable])
    }

    staleStore.remove(key, promise.future)
  }

  /**
   * returns a future that is consulting the stale memcached cache.  If the item is not in the
   * cache, the backendFuture will be invoked (complete the operation).
   *
   * @param key The stale cache key
   * @param backendFuture The future that is actually calculating the fresh cache entry
   * @param ec  The require execution context to run the stale cache key.
   * @return  A future that will result in the stored Serializable object
   */
  private def getFutueForStaleDistributedCacheLookup(key : String, backendFuture : Future[Serializable],
                                                     ec: ExecutionContext) : Future[Serializable] = {

    // protection against thundering herd on stale memcached
    val promise = Promise[Serializable]()
    val existingFuture : Future[Serializable] = staleStore.putIfAbsent(key, promise.future)

    if(existingFuture == null) {
        if(ec!=null) {
          Future {
            getFromStaleDistributedCache(key, promise, backendFuture)
          }(ec)
        }
        else {
          getFromStaleDistributedCache(key, promise, backendFuture)
        }
        promise.future
    }
    else {
      existingFuture
    }
  }

  /**
   * Returns an Object from the distributed cache.  The object will be
   * an instance of Serializable.  If no item existed in the cached
   * null WILL be returned
   *
   * @param key The key to find in the distributed cache
   * @param timeoutInMillis The amount of time to wait for the get on the distributed cache
   * @param cacheType The cache type.  This is output to the log when a hit or miss is logged
   * @return
   */
  private def getFromDistributedCache(key: String, timeoutInMillis : Long,
                                      cacheType : String) : Object = {
    var serialisedObj : Object = null
    try {
      val future =  getMemcachedClient().asyncGet(key)
      val cacheVal = future.get(timeoutInMillis,TimeUnit.MILLISECONDS)
      if(cacheVal==null){
        logCacheMiss(key,cacheType)
      } else {
        logCacheHit(key,cacheType)
        serialisedObj = cacheVal
      }
    } catch {
      case e @ (_ : OperationTimeoutException | _ :  CheckedOperationTimeoutException | _ : TimeoutException) => {
        logger.warn("timeout when retrieving key {} from memcached",key)
      }
      case e: Exception => {
        logWarn("Unable to contact memcached for get({}): {}",key, e.getMessage)
      }
      case e: Throwable => {
        logWarn("Exception thrown when communicating with memcached for get({}): {}",key, e.getMessage)
      }
    }

    serialisedObj
  }

  /**
   * Obtains a item from the distributed cache.
   *
   * @param key The key under which to find a cached object.
   * @return The cached object
   */
  private def getFromDistributedCache(key: String): Object = {
    getFromDistributedCache(key,memcachedGetTimeoutMillis,MemcachedCache.CACHE_TYPE_DISTRIBUTED_CACHE)
  }

  private def getOptionFromDistributedCache(key: String) : Option[Future[Serializable]] = {
    val item = getFromDistributedCache(key)

    if(item == null) {
      None
    }
    else {
      Some(Future.successful(item.asInstanceOf[Serializable]))
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
                                 key : String, staleCacheKey : String, itemExpiry : Duration,
                                 staleItemExpiry : Duration,
                                 ec: ExecutionContext) : Future[Serializable] = {
    future.onComplete {
      value =>
        // Need to check memcached exception here
        try {
          if (!value.isFailure) {
            val entryValue : Serializable = value.get

            if(useStaleCache) {
              // overwrite the stale cache entry
              writeToDistributedCache(staleCacheKey, entryValue, staleItemExpiry, false)
            }
            // write the cache entry
            writeToDistributedCache(key,entryValue,itemExpiry,waitForMemcachedSet)
          }
        } catch {
          case e: Exception => {
            logger.error("problem setting key {} in memcached",key)
          }
        } finally {

          try {
            promise.complete(value)
          } catch {
            case e: IllegalStateException => {
              logger.error("future already completed for key {}",key)
            }
          } finally {
            // it is an LRU cache, so the value may have been thrown out.
            store.remove(key, promise.future)
          }
        }
    }(ec)
    promise.future
  }

  def remove(key: Any) : Option[Future[Serializable]] = {
    if(!isEnabled()) {
      warnCacheDisabled()
      None
    }
    else {
      val keyString: String = getHashedKey(key.toString)
      val removedFuture: Option[Future[Serializable]] = store.remove(keyString) match {
        case null => None
        case x => Some(x)
      }

      val memcachedRemovedFuture = getFromDistributedCache(keyString)

      if(memcachedRemovedFuture == null) {
        None
      }
      else {
        try {
          val memcached : MemcachedClientIF = getMemcachedClient();
          if (useStaleCache) {
            memcached.delete(createStaleCacheKey(keyString))
          }

          if (waitForMemcachedRemove) {
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




        removedFuture match {
          case None => {
            Some(Future.successful(memcachedRemovedFuture.asInstanceOf[Serializable]))
          }
          case Some(_) => {
            removedFuture
          }
        }
      }
    }
  }

  private def clearInternalCaches() : Unit = {
    store.clear()
    if(useStaleCache) {
      staleStore.clear()
    }
  }

  def clear(): Unit = {
    if(isEnabled()) {
      if (allowFlush) {
        try {
          clearInternalCaches()
          getMemcachedClient().flush()
        } catch {
          case e: Exception => {
            logger.error("Exception encountered when attempting to flush memcached")
          }
        }
      } else {
        throw new UnsupportedOperationException
      }
    }
  }

  def size = {
    if(!isEnabled()) {
      0
    }
    else {
      0
    }

  }

  def close()= {
    clearInternalCaches()
    clientFactory.shutdown()
  }


}
