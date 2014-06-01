package org.greencheek.util

import java.net.{InetAddress, InetSocketAddress, ServerSocket}

/**
 * Created by dominictootell on 09/03/2014.
 */
class PortUtil {

  def apply() = new PortUtil


  def findFreePort: ServerSocket = {
    val server: ServerSocket = new ServerSocket(0,1,InetAddress.getLoopbackAddress())
    server.setReuseAddress(true)
    server
  }

  def getPort(server : ServerSocket) : Int = {
    val port: Int = server.getLocalPort
    server.close()
    return port
  }

}
