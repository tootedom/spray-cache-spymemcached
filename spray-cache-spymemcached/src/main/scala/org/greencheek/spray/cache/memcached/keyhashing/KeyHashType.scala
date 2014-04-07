package org.greencheek.spray.cache.memcached.keyhashing

/**
 * Created by dominictootell on 07/04/2014.
 */
sealed abstract class KeyHashType
case object MD5KeyHash extends KeyHashType
case object SHA256KeyHash extends KeyHashType
case object NoKeyHash extends KeyHashType
