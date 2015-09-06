package ch1p2.cb.actor

import scala.concurrent.duration._

import Worker.{ Finished, Ping, Work }
import akka.actor.{ Actor, ActorLogging, Props }
import akka.routing.FromConfig

object Worker {
  case class Work(task: String)

  // protocols
  case object DownLoad
  case object Finished
  case class Ping(n: Long)

  def routerProps = FromConfig.props(props)

  def props = Props[Worker]
}

/*
 * 処理時間が100msなのでProducerの送信能力(/50ms)よりも処理性能が低い。
 */
class Worker extends Actor with ActorLogging {
  import scala.concurrent.duration._

  context.system.scheduler.schedule(0.second, 1000.seconds) {
    self ! Ping(System.currentTimeMillis())
  }(context.system.dispatchers.lookup("main-scheduler"))

  def receive = {
    case Work(task) =>
      Thread.sleep(100L) // 処理時間は100ms
      sender ! Finished
  }
}