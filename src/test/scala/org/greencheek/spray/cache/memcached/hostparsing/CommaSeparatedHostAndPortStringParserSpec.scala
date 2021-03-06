package org.greencheek.spray.cache.memcached.hostparsing

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by dominictootell on 06/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class CommaSeparatedHostAndPortStringParserSpec extends Specification {


  "A host string with one host and port ending in a comma, should parse to one host" in {
    val hostparser = CommaSeparatedHostAndPortStringParser
    val listOfHosts : List[(String,Int)] = hostparser.parseMemcachedNodeList("localhost:11111,")

    listOfHosts.size mustEqual 1

    listOfHosts must contain(exactly(List(("localhost",11111)):_*))
  }
  "A host string with two hosts and ports ending in a comma, should parse to two hosts" in {
    val hostparser = CommaSeparatedHostAndPortStringParser
    val listOfHosts : List[(String,Int)] = hostparser.parseMemcachedNodeList("localhost:11111,hosttwo:13456,")

    listOfHosts.size mustEqual 2

    listOfHosts must contain(exactly(List(("localhost",11111),("hosttwo",13456)):_*))
  }
  "A host string with two hosts and large port, ending in a comma, should parse to two hosts with a default port" in {
    val hostparser = CommaSeparatedHostAndPortStringParser
    val listOfHosts : List[(String,Int)] = hostparser.parseMemcachedNodeList("localhost:11111,hosttwo:99999,")

    listOfHosts.size mustEqual 2

    listOfHosts must contain(exactly(List(("localhost",11111),("hosttwo",11211)):_*))
  }
  "A host string with three hosts and ports, should parse to 3 hosts" in {
    val hostparser = CommaSeparatedHostAndPortStringParser
    val listOfHosts : List[(String,Int)] = hostparser.parseMemcachedNodeList("localhost:11111,hosttwo:99999,hosty:12121,")

    listOfHosts.size mustEqual 3

    listOfHosts must contain(exactly(List(("localhost",11111),("hosttwo",11211),("hosty",12121)):_*))
  }
}
