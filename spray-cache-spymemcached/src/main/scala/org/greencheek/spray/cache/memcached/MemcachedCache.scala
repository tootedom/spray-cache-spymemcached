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
import scala.Some
import scala.util.Success
import net.spy.memcached.transcoders.Transcoder
import org.greencheek.spy.extensions.SerializingTranscoder
import org.greencheek.dns.lookup.{TCPAddressChecker, AddressChecker, LookupService}
import scala.collection.JavaConversions._
import scala.annotation.switch

/*
 * Created by dominictootell on 26/03/2014.
 */
object MemcachedCache {
  private val logger : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])
  private val DEFAULT_EXPIRY : Duration = Duration(60,TimeUnit.MINUTES)
  private val DEFAULT_MEMCACHED_PORT : Int = 11211
  private val DEFAULT_DNS_TIMEOUT : Duration = Duration(3,TimeUnit.SECONDS)
  private val DEFAULT_CAPACITY = 1000
  private val ONE_SECOND = Duration(1,TimeUnit.SECONDS)


  private def validateMemcacheHosts(checkTimeout : Duration,
                                    addressesToCheck : List[InetSocketAddress]) : List[InetSocketAddress] = {
    var okAddresses : List[InetSocketAddress]= Nil
    val addressChecker : AddressChecker = new TCPAddressChecker(checkTimeout.toMillis)
    for(addy <- addressesToCheck) {
        addressChecker.isAvailable(addy) match {
          case true => {
            okAddresses = addy :: okAddresses
          }
          case false => {
            logger.error("Unable to connect to memcached node: {}", addy)
          }
        }
    }
    okAddresses
  }
  /**
   * Takes the list of host and port pairs, interating over each in turn and attempting:
   * resolve the hostname to an ip, and attempting a connection to the host on the given port
   *
   *
   * @param nodes The list of ports to connect
   * @param dnsLookupTimeout The amount of time to wait for a dns lookup to take.
   * @return
   */
  private def returnSocketAddressesForHostNames(nodes: List[(String,Int)],
                                                dnsLookupTimeout : Duration = DEFAULT_DNS_TIMEOUT): List[InetSocketAddress] = {
    val addressLookupService = LookupService.create()

    var workingNodes: List[InetSocketAddress] = Nil
    for (hostAndPort <- nodes) {
      var future: java.util.concurrent.Future[InetAddress] = null
      val host = hostAndPort._1
      val port = hostAndPort._2
      try {
        future = addressLookupService.getByName(host)
        var ia: InetAddress = future.get(dnsLookupTimeout.toSeconds, TimeUnit.SECONDS)
        if (ia == null) {
          logger.error("Unable to resolve dns entry for the host: {}", host)
        }
        else
        {
          try {
            workingNodes = new InetSocketAddress(ia,port ) :: workingNodes
          }
          catch {
            case e: IllegalArgumentException => {
              logger.error("Invalid port number has been provided for the memcached node: host({}),port({})", host, port)
            }
          }
        }
      }
      catch {
        case e: TimeoutException => {
          logger.error("Problem resolving host name ({}) to an ip address in fixed number of seconds: {}", host, dnsLookupTimeout, e)
        }
        case e: Exception => {
          logger.error("Problem resolving host name to ip address: {}", host)
        }
      }
      finally {
        if (future != null) future.cancel(true)
      }
    }
    addressLookupService.shutdown()

    workingNodes
  }

  /**
   * Takes a string:
   *
   * url:port,url:port
   *
   * converting it to a list of 2 element string arrays:  [url,port],[url,port]
   *
   * @param urls
   * @return
   */
  private def parseMemcachedNodeList(urls: String): List[(String,Int)] = {
    if (urls == null) return Nil
    val hostUrls = urls.trim
    var memcachedNodes : List[(String,Int)] = Nil
    for (url <- hostUrls.split(",")) {
      var port: Int = DEFAULT_MEMCACHED_PORT
      val indexOfPort = url.indexOf(':')
      val host =  indexOfPort match {
        case -1 => {
          url.trim
        }
        case any => {
          url.substring(0, any).trim
        }
      }

      try {
          port = Integer.parseInt(url.substring(indexOfPort + 1, url.length))
          if(port > 65535) {
            port = DEFAULT_MEMCACHED_PORT
          }
        }
        catch {
          case e: NumberFormatException => {
            logger.info("Unable to parse memcached port number, not an integer")
          }
      }

      if ( host.length != 0 ) {
        memcachedNodes = (host, port) :: memcachedNodes
      }
    }
    return memcachedNodes
  }
}


class MemcachedCache[Serializable](val timeToLive: Duration = MemcachedCache.DEFAULT_EXPIRY,
                                   val maxCapacity: Int = MemcachedCache.DEFAULT_CAPACITY,
                                   val memcachedHosts : String = "localhost:11211",
                                   val hashingType : Locator = Locator.CONSISTENT,
                                   val failureMode : FailureMode = FailureMode.Redistribute,
                                   val hashAlgorithm : DefaultHashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH,
                                   val serializingTranscoder : Transcoder[Object] = new SerializingTranscoder(),
                                   val protocol : ConnectionFactoryBuilder.Protocol = Protocol.BINARY,
                                   val readBufferSize : Int = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
                                   val memcachedGetTimeout : Duration = Duration(2500,TimeUnit.MILLISECONDS),
                                   val throwExceptionOnNoHosts : Boolean = false,
                                   val dnsConnectionTimeout : Duration = Duration(3,TimeUnit.SECONDS),
                                   val doHostConnectionAttempt : Boolean = true,
                                   val hostConnectionAttemptTimeout : Duration = Duration(1,TimeUnit.SECONDS),
                                   val waitForMemcachedSet : Boolean = false,
                                   val setWaitDuration : Duration = Duration(2,TimeUnit.SECONDS),
                                   val allowFlush : Boolean = false,
                                   val waitForMemcachedRemove : Boolean = false,
                                   val removeWaitDuration : Duration = Duration(2,TimeUnit.SECONDS)
                                   ) extends Cache[Serializable] {

  @volatile private var isEnabled = false
  @volatile private var memcached: MemcachedClientIF = null;

  private var parsedHosts : List[(String,Int)] =  MemcachedCache.parseMemcachedNodeList(memcachedHosts)
  parsedHosts match {
    case Nil => {
      if(throwExceptionOnNoHosts) {
        throw new InstantiationError()
      } else {
        isEnabled = false
      }
    }
    case hosts : List[(String,Int)] => {
      var addresses : List[InetSocketAddress] = MemcachedCache.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout)
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
            addresses = MemcachedCache.validateMemcacheHosts(hostConnectionAttemptTimeout,resolvedHosts)
          }
        }
      }

      addresses match {
        case Nil => {
          isEnabled = false
        }
        case hostsToUse : List[InetSocketAddress] => {
          isEnabled = true
          val builder: ConnectionFactoryBuilder = new ConnectionFactoryBuilder()
          builder.setHashAlg(hashAlgorithm)
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

  private val logger  : Logger = LoggerFactory.getLogger(classOf[MemcachedCache[Serializable]])
  private val cachedHitMissLogger  : Logger  = LoggerFactory.getLogger("MemcachedCacheHitsLogger")

  require(maxCapacity >= 0, "maxCapacity must not be negative")

  private[cache] val store = new ConcurrentLinkedHashMap.Builder[String, Future[Serializable]]
    .initialCapacity(maxCapacity)
    .maximumWeightedCapacity(maxCapacity)
    .build()

  private def logCacheHit(key: String): Unit = {
    cachedHitMissLogger.debug("{ \"cachehit\" : \"{}\"}",key)
  }

  private def logCacheMiss(key: String): Unit = {
    cachedHitMissLogger.debug("{ \"cachemiss\" : \"{}\"}",key)
  }

  private def getFromDistributedCache(key: String): Option[Future[Serializable]] = {
    try {
        memcached.get(key) match {
          case null => {
            logCacheMiss(key)
            logger.debug("key {} not found in memcached", key)
            None
          }
          case o: Object => {
            logCacheHit(key)
            logger.debug("key {} found in memcached", key)
            Some(Promise.successful(o.asInstanceOf[Serializable]).future)
          }
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
    }
  }

  private def writeToDistributedCache(key: String, value : Serializable, timeToLive : Duration) : Unit = {
    val entryTTL : Duration = timeToLive match {
      case Duration.Inf => Duration.Zero
      case Duration.MinusInf => Duration.Zero
      case Duration.Zero => Duration.Zero
      case x if x.lt(MemcachedCache.ONE_SECOND) => Duration.Zero
      case _  => timeToLive
    }

    if( waitForMemcachedSet ) {
      val futureSet = memcached.set(key, entryTTL.toSeconds.toInt, value)
      try {
        futureSet.get(setWaitDuration.toMillis, TimeUnit.MILLISECONDS)
      } catch {
        case e: Exception => {
          logger.warn("Exception waiting for memcached set to occur")
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
    if(!isEnabled) {
      logCacheMiss(key.toString)
      None
    } else {
      store.get(key) match {
        case null => {
          val keyString: String = key.toString
          getFromDistributedCache(keyString)
        }
        case existing => {
          val keyString: String = key.toString
          logCacheHit(keyString)
          Some(existing)
        }
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
    // check local whilst computation is occurring cache.
    val keyString = key.toString
    logger.info("put requested for {}", keyString)
    if(!isEnabled) {
      logCacheMiss(keyString)
      genValue()
    }
    else {
      store.get(keyString) match {
        case null => {
          // check memcached.
          getFromDistributedCache(keyString) match {
            case None => {
              val promise = Promise[Serializable]()
              store.putIfAbsent(keyString, promise.future) match {
                case null => {
                  val future = genValue()
                  future.onComplete {
                    value =>
                      promise.complete(value)
                      // Need to check memcached exception here
                      try {
                        if (!value.isFailure) {
                          writeToDistributedCache(keyString,value.get,itemExpiry)
                        }
                      } catch {
                        case e: Exception => {
                           logger.error("problem setting key {} in memcached",key)
                        }
                      } finally {
                        store.remove(keyString, promise.future)
                      }

                  }
                  future
                }
                case existingFuture => {
                  existingFuture
                }
              }
            }
            case Some(future) => {
              future
            }
          }
        }
        case existingFuture => {
          logCacheHit(keyString)
          existingFuture
        }
      }
    }
  }



  def remove(key: Any) = {
    if(!isEnabled) {
      None
    }
    else {
      val keyString: String = key.toString
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

}
