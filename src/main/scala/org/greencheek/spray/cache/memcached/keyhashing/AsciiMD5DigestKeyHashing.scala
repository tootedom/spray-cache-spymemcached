package org.greencheek.spray.cache.memcached.keyhashing

import org.greencheek.util.AsciiStringToBytes

/**
 * Created by dominictootell on 08/06/2014.
 */
class AsciiMD5DigestKeyHashing(numberOfDigests : Int = -1,upperCase : Boolean = true) extends MD5DigestKeyHashing(numberOfDigests,upperCase) {
  override def getBytes(key : String) : Array[Byte] = {
    AsciiStringToBytes.getBytes(key)
  }
}
