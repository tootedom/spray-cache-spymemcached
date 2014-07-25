package org.greencheek.spray.cache.memcached.clientbuilder.elasticache.configparsing


import scala.collection.mutable.ArrayBuffer


/**
 * Created by dominictootell on 22/07/2014.
 */
object SplitByChar extends SplitByChar


trait SplitByChar {

  val EMPTY_LIST : List[String] = List()

  def split( str : String, separatorChar : Char, compact : Boolean = true) : Seq[String] = {
    if (str == null) {
      return EMPTY_LIST
    }
    val len: Int = str.length

    if (len == 0) {
      return EMPTY_LIST
    }

    val list: ArrayBuffer[String] = new ArrayBuffer[String]()
    var i: Int = 0
    var start: Int = 0

    var isMatch: Boolean = if(compact) false else true
    var lastMatch: Boolean = false

    while (i < len) {
      if (str.charAt(i) == separatorChar) {
        if (isMatch) {
          list.append(str.substring(start, i))
          isMatch = if(compact) false else true
          lastMatch = true
        }
        i+=1
        start = i
      }
      else {
        lastMatch = false
        isMatch = true
        i += 1
      }
    }

    if (isMatch || lastMatch) {
      list.append(str.substring(start, i))
    }

    list
  }



}
