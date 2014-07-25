package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by dominictootell on 22/07/2014.
 */
@RunWith(classOf[JUnitRunner])
class SplitByCharSpec extends Specification {

  "A string with no char separator should return the string" in  {
    val splitItems : Seq[String] = SplitByChar.split("test",',')

    splitItems.size should be equalTo(1)
    splitItems(0) must beEqualTo("test")
  }

  "A empty string should return an empty list" in {
    val splitItems : Seq[String] = SplitByChar.split("",',')

    splitItems.size should be equalTo(0)
  }

  "A null string should return an empty list" in {
    val splitItems : Seq[String] = SplitByChar.split(null,',')

    splitItems.size should be equalTo(0)
  }

  "A string with 2 separator chars should return 2 elements" in {
    val splitItems : Seq[String] = SplitByChar.split("test1|test2|test3",'|')

    splitItems.size should be equalTo(3)
    splitItems(0) must beEqualTo("test1")
    splitItems(1) must beEqualTo("test2")
    splitItems(2) must beEqualTo("test3")
  }

  "A string with multiple separators together should be treated as 1 spearator" in {
    val splitItems : Seq[String] = SplitByChar.split("test1|||test2|||||||||test3",'|')

    splitItems.size should be equalTo(3)
    splitItems(0) must beEqualTo("test1")
    splitItems(1) must beEqualTo("test2")
    splitItems(2) must beEqualTo("test3")
  }

  "A string with multiple separators together, no compaction, should not be treated as 1 spearator" in {
    val splitItems : Seq[String] = SplitByChar.split("test1|||test2|||test3",'|', false)

    splitItems.size should be equalTo(7)
    splitItems(0) must beEqualTo("test1")
    splitItems(1) must beEqualTo("")
    splitItems(2) must beEqualTo("")
    splitItems(3) must beEqualTo("test2")
    splitItems(4) must beEqualTo("")
    splitItems(5) must beEqualTo("")
    splitItems(6) must beEqualTo("test3")
  }

}
