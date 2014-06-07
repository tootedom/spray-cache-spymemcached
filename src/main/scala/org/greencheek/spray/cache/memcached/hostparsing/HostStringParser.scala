package org.greencheek.spray.cache.memcached.hostparsing

trait HostStringParser {

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
  def parseMemcachedNodeList(urls: String): List[(String,Int)]
}
