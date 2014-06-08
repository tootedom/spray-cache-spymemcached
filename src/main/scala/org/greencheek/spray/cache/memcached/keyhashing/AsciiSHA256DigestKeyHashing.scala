package org.greencheek.spray.cache.memcached.keyhashing

import org.greencheek.util.AsciiStringToBytes

/**
 * Created by dominictootell on 08/06/2014.
 */
class AsciiSHA256DigestKeyHashing(numberOfDigests : Int = -1,upperCase : Boolean = true) extends SHA256DigestKeyHashing(numberOfDigests,upperCase) {
  override def getBytes(key : String) : Array[Byte] = {
    AsciiStringToBytes.getBytes(key)
  }
}
