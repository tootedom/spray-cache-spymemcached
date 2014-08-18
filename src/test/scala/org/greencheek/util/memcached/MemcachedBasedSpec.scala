package org.greencheek.util.memcached

import org.specs2.mutable.Specification

import java.net.ServerSocket
import org.greencheek.util.PortUtil
import spray.util.pimps.PimpedFuture

import scala.concurrent.Future


trait MemcachedBasedSpec extends Specification {
  implicit def pimpFuture[T](fut: Future[T]): PimpedFuture[T] = new PimpedFuture[T](fut)


  import org.specs2._
  import specification._

  @volatile var memcachedD: MemcachedDaemonWrapper = null
  @volatile var portServerSocket: ServerSocket = null
  @volatile var memcachedDport: Int = -1
  val portUtil = new PortUtil()

  /** the map method allows to "post-process" the fragments after their creation */
  override def map(fs: => Fragments) = Step(startMemcached) ^ fs ^ Step(stopMemcached)

  def useBinary : Boolean = true

  def startMemcached() = {
    portServerSocket = portUtil.findFreePort
    memcachedDport = portUtil.getPort(portServerSocket)
    memcachedD = MemcachedDaemonFactory.startMemcachedDaemon(memcachedDport,useBinary)
  }

  def stopMemcached: Unit = {
    MemcachedDaemonFactory.stopMemcachedDaemon(memcachedD)
  }


}
