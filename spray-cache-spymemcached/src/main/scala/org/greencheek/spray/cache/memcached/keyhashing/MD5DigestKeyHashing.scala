package org.greencheek.spray.cache.memcached.keyhashing

/**
 * Created by dominictootell on 07/04/2014.
 */
class MD5DigestKeyHashing(numberOfDigests : Int = -1,upperCase : Boolean = true)
  extends DigestKeyHashing(DigestKeyHashing.MD5,numberOfDigests,upperCase) {
}
