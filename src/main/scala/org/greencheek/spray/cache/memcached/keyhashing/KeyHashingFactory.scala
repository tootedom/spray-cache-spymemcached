package org.greencheek.spray.cache.memcached.keyhashing

/**
 * Creates the appropriate KeyHashing instance depending upon the given case type,
 * and if asciiOnlyKeys are to be used.
 *
 */
object KeyHashingFactory {

  def createKeyHashing(keyHashType : KeyHashType,
                       asciiOnlyKeys : Boolean) : KeyHashing = {
    val keyHashingFunction : KeyHashing = keyHashType match {
      case MD5KeyHash | MD5UpperKeyHash => createMd5KeyHasher(asciiOnlyKeys,true)
      case SHA256KeyHash | SHA256UpperKeyHash => createShaKeyHasher(asciiOnlyKeys,true)
      case MD5LowerKeyHash => createMd5KeyHasher(asciiOnlyKeys,false)
      case SHA256LowerKeyHash => createShaKeyHasher(asciiOnlyKeys,false)
      case NoKeyHash => NoKeyHashing.INSTANCE
      case XXJavaHash => createXXKeyHasher(asciiOnlyKeys,false)
      case XXNativeJavaHash => createXXKeyHasher(asciiOnlyKeys,true)
      case JenkinsHash => JenkinsKeyHashing
      case _ => NoKeyHashing.INSTANCE
    }

    keyHashingFunction
  }


  private def createXXKeyHasher(asciiOnlyKeys : Boolean, native : Boolean) : KeyHashing = {
    if(asciiOnlyKeys) {
      if(native) {
        AsciiXXKeyHashing.JNI_INSTANCE
      } else {
        AsciiXXKeyHashing.JAVA_INSTANCE
      }
    } else {
      if(native) {
        XXKeyHashing.JNI_INSTANCE
      } else {
        XXKeyHashing.JAVA_INSTANCE
      }
    }
  }

  private def createShaKeyHasher(asciiOnlyKeys : Boolean, uppercase : Boolean) : KeyHashing = {
    if (asciiOnlyKeys) {
      new AsciiSHA256DigestKeyHashing(upperCase = false)
    } else {
      new SHA256DigestKeyHashing(upperCase = false)
    }
  }

  private def createMd5KeyHasher(asciiOnlyKeys : Boolean, uppercase : Boolean) : KeyHashing = {
    if (asciiOnlyKeys) {
      new AsciiMD5DigestKeyHashing(upperCase = uppercase)
    } else {
      new MD5DigestKeyHashing(upperCase = uppercase)
    }
  }



}
