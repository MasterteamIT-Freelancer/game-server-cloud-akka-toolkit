package Server

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, Tcp}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.util.ByteString

import scala.concurrent.Future

object GameServer extends App {
  implicit val system = ActorSystem("GameServer")
  implicit val materializer = Materializer

  val host = "127.0.0.1"
  val port = 54000

  val connections: Source[IncomingConnection, Future[ServerBinding]] = Tcp().bind(host, port)
  connections
    .to(Sink.foreach { connection =>
      // server logic, parses incoming commands
      val commandParser = Flow[String].takeWhile(_ != "BYE").map(_ + "!")

      import connection._
      val welcomeMsg = s"Welcome to: $localAddress, you are: $remoteAddress!"
      val welcome = Source.single(welcomeMsg)

      val serverLogic = Flow[ByteString]
        .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
        .map(_.utf8String)
        .via(commandParser)
        // merge in the initial banner after parser
        .merge(welcome)
        .map(_ + "\n")
        .map(ByteString(_))

      connection.handleWith(serverLogic)
    })
    .run()

}
