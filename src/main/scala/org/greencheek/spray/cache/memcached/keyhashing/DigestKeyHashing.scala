package org.greencheek.spray.cache.memcached.keyhashing

import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.io.UnsupportedEncodingException

object DigestKeyHashing {
  val MD5: String = "MD5"
  val SHA526 : String = "SHA-256"

  private val DIGITS_LOWER: Array[Char] = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  private val DIGITS_UPPER: Array[Char] = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')


  def bytesToUpperCaseHex(data : Array[Byte]) : String = {
    bytesToHex(data,DIGITS_UPPER)
  }

  def bytesToLowerCaseHex(data : Array[Byte]) : String = {
    bytesToHex(data,DIGITS_LOWER)
  }

  private def bytesToHex(data : Array[Byte], chars : Array[Char]) : String = {
    val length: Int = data.length;
    val out = new Array[Char](length << 1);
    for (i <- 0 until length
         ) {
      out(i+i) = chars((0xF0 & data(i)) >>> 4);
      out(i+i+1) = chars(0x0F & data(i));
    }
    new String(out);
  }
}

class DigestKeyHashing(val algorithm : String, val numberOfDigests : Int = -1,
                       val upperCase : Boolean = true) extends KeyHashing {


  private val digests : ArrayBlockingQueue[MessageDigest] = numberOfDigests match {
    case -1 => {
      createDigests(Runtime.getRuntime().availableProcessors() * 2, algorithm)
    }
    case _ => {
      createDigests(numberOfDigests, algorithm)
    }
  }

  private def createDigests(num : Int, algorithm : String) : ArrayBlockingQueue[MessageDigest] = {
    val queue = new ArrayBlockingQueue[MessageDigest](num)
    for(a <- 1 to num) {
      queue.add(MessageDigest.getInstance(algorithm))
    }
    queue
  }

  private def digest(input: Array[Byte]): Array[Byte] = {
    var md: MessageDigest = digests.remove()
    val result: Array[Byte] = md.digest(input)
    md.reset()
    digests.add(md)
    result
  }

  def getBytes(key : String) : Array[Byte] = {
    var bytes: Array[Byte] = null
    try {
      bytes = key.getBytes("UTF-8")
    }
    catch {
      case e: UnsupportedEncodingException => {
        bytes = key.getBytes
      }
    }
    bytes
  }


  override def hashKey(key: String): String = {
    upperCase match {
      case true => {
        DigestKeyHashing.bytesToUpperCaseHex(digest(getBytes(key)))
      }
      case false => {
        DigestKeyHashing.bytesToLowerCaseHex(digest(getBytes(key)))
      }
    }
  }
}

