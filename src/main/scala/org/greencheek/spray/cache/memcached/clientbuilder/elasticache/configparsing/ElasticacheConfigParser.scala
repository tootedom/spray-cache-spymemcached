package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing

import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost

import scala.collection.mutable.ArrayBuffer

/**
 * Created by dominictootell on 22/07/2014.
 */
object ElasticacheConfigParser {

  def parseServers(serversString : String ): Seq[ElastiCacheHost] = {
    val separatedHosts : Seq[String] = SplitByChar.split(serversString,' ',true)

    val elastiCacheHosts : ArrayBuffer[ElastiCacheHost] = new ArrayBuffer[ElastiCacheHost]()
    val size = separatedHosts.size

    var i = 0;
    while(i<size) {
      val hostString = separatedHosts(i)
      i+=1

      val hostInfo : Seq[String] = SplitByChar.split(hostString,'|',false)

      if(hostInfo.size==3) {
        val hostName = hostInfo(0).trim
        val hostIP = hostInfo(1).trim
        val hostPort = hostInfo(2).trim
        elastiCacheHosts.append(ElastiCacheHost(hostName,hostIP,hostPort,hostIP.length>0))
      }
    }


    elastiCacheHosts

  }
}
