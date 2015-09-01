package ch1p1.services

import ch1p1.models.protocols.Log
import akka.actor.Actor
import akka.actor.ActorRef
import ch1p1.consumers.SumUp
import scala.concurrent.duration.Duration._
import scala.concurrent.duration._
import akka.routing.FromConfig
import akka.actor.Props
import akka.actor.ActorSelection

object LogStore {
  case object Flush
  
}

object LogStoreSupervisor {
  def props = Props(classOf[LogStoreSupervisor])
}
class LogStoreSupervisor extends Actor {

  context.actorOf(FromConfig.props(Props(classOf[LogStore], "category")), "category")
  context.actorOf(FromConfig.props(Props(classOf[LogStore], "group")), "group")
  context.actorOf(FromConfig.props(Props(classOf[LogStore], "item")), "item")

  def receive = Actor.emptyBehavior
}

/**
 * ログを集めるActor。
 * 一定時間ごとにログをconsumer(集計サービスを想定)に送る。
 */
class LogStore(name: String) extends Actor {
  import LogStore._
 
  var buffer: Vector[Log] = Vector.empty
  lazy val consumer = context.actorSelection(s"/user/consumer/${name}")
  
  context.system.scheduler.schedule(0.seconds, 1.seconds, self, Flush)(context.dispatcher)

  override def preStart(): Unit = {
    // TODO FSMとか使って少しずつ送る
    val pastLogs = findPastLogs()
    consumer ! SumUp(pastLogs)
  }

  def receive = {
    case log: Log =>
      serialize(log)
      buffer = buffer :+ log
    case Flush =>
      // TODO bufferが大きくなりすぎるので、本当はbuffer-sizeの上限を設けた方がよい
      consumer ! SumUp(buffer)
      buffer = Vector.empty
  }
  
  def findPastLogs(): Vector[Log] = Vector.empty
  def serialize(log: Log) = ()
}

