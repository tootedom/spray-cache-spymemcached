package org.greencheek.spray.cache.memcached.keyhashing

import java.io.UnsupportedEncodingException

/**
 * Created by dominictootell on 28/05/2014.
 */
object JenkinsKeyHashing extends KeyHashing {
  def hashKey(key : String) : String = {
    try {
      var hash: Int = 0
      for (bt <- key.getBytes("UTF-8")) {
        hash += (bt & 0xFF)
        hash += (hash << 10)
        hash ^= (hash >>> 6)
      }
      hash += (hash << 3)
      hash ^= (hash >>> 11)
      hash += (hash << 15)
      (hash & 0xFFFFFFFFl).toString
    }
    catch {
      case e: UnsupportedEncodingException => {
        throw new IllegalStateException("Hash function error", e)
      }
    }
  }

}
