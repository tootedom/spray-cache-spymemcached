package org.greencheek.spray.cache.memcached.keyhashing

/**
 * Created by dominictootell on 07/04/2014.
 */
trait KeyHashing {
  def hashKey(key : String) : String
}
object NoKeyHashing {
  val INSTANCE = new NoKeyHashing
}
class NoKeyHashing extends KeyHashing {
  override def hashKey(key: String): String = key
}
