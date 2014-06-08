package org.greencheek.spray.cache.memcached.keyhashing

import org.greencheek.util.AsciiStringToBytes

/**
 * Created by dominictootell on 08/06/2014.
 */
class AsciiXXKeyHashing(allowNative : Boolean) extends XXKeyHashing(allowNative) {
  override def getBytes(key : String) : Array[Byte] = {
    AsciiStringToBytes.getBytes(key)
  }
}

object AsciiXXKeyHashing {
  val JNI_INSTANCE = new AsciiXXKeyHashing(true)
  val JAVA_INSTANCE = new AsciiXXKeyHashing(false)
}

