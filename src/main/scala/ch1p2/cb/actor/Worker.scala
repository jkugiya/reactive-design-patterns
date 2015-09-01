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

class Worker extends Actor with ActorLogging {
  import scala.concurrent.duration._

  context.system.scheduler.schedule(0.second, 1000.seconds) {
    self ! Ping(System.currentTimeMillis())
  }(context.system.dispatchers.lookup("main-scheduler"))

  private[this] val processWork: Actor.Receive = {
    case Work(task) =>
      Thread.sleep(100L) // 処理時間は100ms
      sender ! Finished
  }

  private[this] val receivable: Actor.Receive = {
    case Ping(before) =>
      val current = System.currentTimeMillis()
      if (current - before > 150) {
        context become (unreceivable orElse processWork)
      }
  }

 private[this] val unreceivable: Actor.Receive = {
    case Ping(before) =>
      val current = System.currentTimeMillis()
      if (current - before <= 150) {
        context unbecome
      }
  }

 def receive = receivable orElse processWork

}