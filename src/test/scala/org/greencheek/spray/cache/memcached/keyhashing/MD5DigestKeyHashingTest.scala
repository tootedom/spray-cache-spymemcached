package org.greencheek.spray.cache.memcached.keyhashing

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by dominictootell on 08/06/2014.
 */
@RunWith(classOf[JUnitRunner])
class MD5DigestKeyHashingTest extends Specification {

  "Check that upppercase md5 hashing returns expected key" in {

    val hashing = new MD5DigestKeyHashing(upperCase = true)
    hashing.hashKey("hello") mustEqual("5d41402abc4b2a76b9719d911017c592".toUpperCase())

  }
  "Check that lowercase md5 hashing returns expected key" in {

    val hashing = new MD5DigestKeyHashing(upperCase = false)
    hashing.hashKey("hello") mustEqual("5d41402abc4b2a76b9719d911017c592")

  }
}
