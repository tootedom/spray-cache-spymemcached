package org.greencheek.spray.cache.memcached.spyconnectionfactory

import net.spy.memcached.ConnectionFactoryBuilder.{Protocol, Locator}
import net.spy.memcached._
import net.spy.memcached.transcoders.Transcoder
import org.greencheek.spray.cache.memcached.keyhashing._
import org.greencheek.spy.extensions.FastSerializingTranscoder
import org.greencheek.spy.extensions.connection.CustomConnectionFactoryBuilder

/**
 * Created by dominictootell on 22/07/2014.
 */
object SpyConnectionFactoryBuilder {

  def createConnectionFactory(
    hashingType : Locator = Locator.CONSISTENT,
    failureMode : FailureMode = FailureMode.Redistribute,
    hashAlgorithm : HashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH,
    serializingTranscoder : Transcoder[Object] = new FastSerializingTranscoder(),
    protocol : ConnectionFactoryBuilder.Protocol = Protocol.BINARY,
    readBufferSize : Int = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE,
    keyHashType : KeyHashType = NoKeyHash) : ConnectionFactory = {

    val builder : ConnectionFactoryBuilder = keyValidationRequired(keyHashType) match {
      case true => new ConnectionFactoryBuilder();
      case false => new CustomConnectionFactoryBuilder();
    }
    builder.setHashAlg(hashAlgorithm)
    builder.setLocatorType(hashingType)
    builder.setProtocol(protocol)
    builder.setReadBufferSize(readBufferSize)
    builder.setFailureMode(failureMode)
    builder.setTranscoder(serializingTranscoder)

    builder.build();
  }

  private def keyValidationRequired(keyHashType : KeyHashType ) : Boolean = {
    keyHashType match {
      case MD5KeyHash | MD5UpperKeyHash | MD5LowerKeyHash => false
      case SHA256KeyHash | SHA256UpperKeyHash | SHA256LowerKeyHash => false
      case XXJavaHash | XXNativeJavaHash => false
      case JenkinsHash => false
      case NoKeyHash => true
      case _ => true
    }
  }
}
