package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing

import org.greencheek.spray.cache.memcached.clientbuilder.elasticache.ElastiCacheHost
import org.specs2.mutable.Specification

/**
 * Created by dominictootell on 22/07/2014.
 */
class ElastiCacheConfigParserTest extends Specification {

  "Given two host, with ips, they should both be parsed" in {
    val exampleHosts : String = "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\n\r\n";

    val hosts : Seq[ElastiCacheHost] =  DefaultElastiCacheConfigParser.parseServers(exampleHosts)

    hosts.size should be equalTo(2)
    hosts(0).hasIP should beTrue
    hosts(1).hasIP should beTrue

    hosts(0).hostName must beEqualTo("myCluster.pc4ldq.0001.use1.cache.amazonaws.com")
    hosts(0).ip must beEqualTo("10.82.235.120")
    hosts(0).port must beEqualTo("11211")

    hosts(1).hostName must beEqualTo("myCluster.pc4ldq.0002.use1.cache.amazonaws.com")
    hosts(1).ip must beEqualTo("10.80.249.27")
    hosts(1).port must beEqualTo("11211")

  }


  "Given two host, one with an ip, they should both be parsed" in {
    val exampleHosts : String = "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com||11212\n\r\n";

    val hosts : Seq[ElastiCacheHost] =  DefaultElastiCacheConfigParser.parseServers(exampleHosts)

    hosts.size should be equalTo(2)
    hosts(0).hasIP should beTrue
    hosts(1).hasIP should beFalse

    hosts(0).hostName must beEqualTo("myCluster.pc4ldq.0001.use1.cache.amazonaws.com")
    hosts(0).ip must beEqualTo("10.82.235.120")
    hosts(0).port must beEqualTo("11211")

    hosts(1).hostName must beEqualTo("myCluster.pc4ldq.0002.use1.cache.amazonaws.com")
    hosts(1).ip must beEqualTo("")
    hosts(1).port must beEqualTo("11212")

  }

  "Given two host, one no ips, they should both be parsed" in {
    val exampleHosts : String = "myCluster.pc4ldq.0001.use1.cache.amazonaws.com\r\n||11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com||11212\n\r\n";

    val hosts : Seq[ElastiCacheHost] =  DefaultElastiCacheConfigParser.parseServers(exampleHosts)

    hosts.size should be equalTo(2)
    hosts(0).hasIP should beFalse
    hosts(1).hasIP should beFalse

    hosts(0).hostName must beEqualTo("myCluster.pc4ldq.0001.use1.cache.amazonaws.com")
    hosts(0).ip must beEqualTo("")
    hosts(0).port must beEqualTo("11211")

    hosts(1).hostName must beEqualTo("myCluster.pc4ldq.0002.use1.cache.amazonaws.com")
    hosts(1).ip must beEqualTo("")
    hosts(1).port must beEqualTo("11212")

  }

  "Given 5 hosts they should all be parsed" in {
    val exampleHosts : String = "myCluster.pc4ldq.0001.use1.cache.amazonaws.com\r\n||11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com||11212\n\r\n myCluster.pc4ldq.0001.use1.cache.amazonaws.com\r\n||11211 myCluster.pc4ldq.0001.use1.cache.amazonaws.com\r\n||11211 myCluster.pc4ldq.0001.use1.cache.amazonaws.com\r\n||11211 ";

    val hosts : Seq[ElastiCacheHost] =  DefaultElastiCacheConfigParser.parseServers(exampleHosts)

    hosts.size should be equalTo(5)
    hosts(0).hasIP should beFalse
    hosts(1).hasIP should beFalse

    hosts(0).hostName must beEqualTo("myCluster.pc4ldq.0001.use1.cache.amazonaws.com")
    hosts(0).ip must beEqualTo("")
    hosts(0).port must beEqualTo("11211")

    hosts(1).hostName must beEqualTo("myCluster.pc4ldq.0002.use1.cache.amazonaws.com")
    hosts(1).ip must beEqualTo("")
    hosts(1).port must beEqualTo("11212")

  }
}
