package ch1p2.cb

import akka.actor.ActorSystem
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import ch1p2.cb.actor.Master
import ch1p2.cb.actor.Worker
import scala.util.Random
import ch1p2.cb.actor.Producer

object CircuitBreakerExample extends App {
  
  val conf = ConfigFactory.load("ch1p2.cb.conf")
  val system = ActorSystem("ch1p2cb", conf)
  
  val master = system.actorOf(Master.props, "master")
  val producer = system.actorOf(Producer.props, "producer")
  
  
  system.awaitTermination()
}