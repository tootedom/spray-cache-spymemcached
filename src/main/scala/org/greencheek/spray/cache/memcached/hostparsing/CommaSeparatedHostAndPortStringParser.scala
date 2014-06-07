package org.greencheek.spray.cache.memcached.hostparsing

import org.slf4j.{LoggerFactory, Logger}

/**
 * Created by dominictootell on 06/06/2014.
 */
object CommaSeparatedHostAndPortStringParser extends HostStringParser {
  private val logger : Logger = LoggerFactory.getLogger(classOf[HostStringParser])
  private val DEFAULT_MEMCACHED_PORT : Int = 11211

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
  override def parseMemcachedNodeList(urls: String): List[(String, Int)] = {
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
