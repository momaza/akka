/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.io

import scala.annotation.tailrec
import akka.testkit.{ AkkaSpec, TestProbe }
import akka.actor.ActorRef
import scala.collection.immutable
import akka.io.Inet.SocketOption
import Tcp._
import akka.TestUtils
import TestUtils._

trait TcpIntegrationSpecSupport { _: AkkaSpec ⇒

  class TestSetup {
    val bindHandler = TestProbe()
    val endpoint = temporaryServerAddress()

    bindServer()

    def bindServer(): Unit = {
      val bindCommander = TestProbe()
      bindCommander.send(IO(Tcp), Bind(bindHandler.ref, endpoint, options = bindOptions))
      bindCommander.expectMsg(Bound)
    }

    def establishNewClientConnection(): (TestProbe, ActorRef, TestProbe, ActorRef) = {
      val connectCommander = TestProbe()
      connectCommander.send(IO(Tcp), Connect(endpoint, options = connectOptions))
      val Connected(`endpoint`, localAddress) = connectCommander.expectMsgType[Connected]
      val clientHandler = TestProbe()
      connectCommander.sender ! Register(clientHandler.ref)

      val Connected(`localAddress`, `endpoint`) = bindHandler.expectMsgType[Connected]
      val serverHandler = TestProbe()
      bindHandler.sender ! Register(serverHandler.ref)

      (clientHandler, connectCommander.sender, serverHandler, bindHandler.sender)
    }

    @tailrec final def expectReceivedData(handler: TestProbe, remaining: Int): Unit =
      if (remaining > 0) {
        val recv = handler.expectMsgType[Received]
        expectReceivedData(handler, remaining - recv.data.size)
      }

    /** allow overriding socket options for server side channel */
    def bindOptions: immutable.Traversable[SocketOption] = Nil

    /** allow overriding socket options for client side channel */
    def connectOptions: immutable.Traversable[SocketOption] = Nil
  }

}
