package org.greencheek.spray.cache.memcached.keyhashing

import net.jpountz.xxhash.XXHashFactory
import java.io.UnsupportedEncodingException

/**
 * Created by dominictootell on 28/05/2014.
 */
class XXKeyHashing(allowNative : Boolean) extends KeyHashing {
  private val factory: XXHashFactory = allowNative match {
    case true => XXHashFactory.fastestInstance()
    case false => XXHashFactory.fastestJavaInstance();
  }

  override def hashKey(key: String): String = {
    var bytes: Array[Byte] = null
    try {
      bytes = key.getBytes("UTF-8")
    }
    catch {
      case e: UnsupportedEncodingException => {
        bytes = key.getBytes
      }
    }
    return hash(bytes, 0, bytes.length)
  }

  def hash(bytes: Array[Byte], offset: Int, length: Int): String = {
    return Integer.toString(factory.hash32.hash(bytes, offset, length, 0))
  }

}

object XXKeyHashing {
  val JNI_INSTANCE = new XXKeyHashing(true)
  val JAVA_INSTANCE = new XXKeyHashing(false)
}

