package org.greencheek.spray.cache.memcached.keyhashing

class SHA256DigestKeyHashing(numberOfDigests : Int = -1,upperCase : Boolean = true)
  extends DigestKeyHashing(DigestKeyHashing.SHA526,numberOfDigests,upperCase) {
}
