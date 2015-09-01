package ch1p2.cb.actor

import scala.concurrent.duration._
import scala.util.Random
import akka.actor.Actor
import scala.collection.mutable.Queue
import akka.actor.Props
import akka.actor.ActorLogging

object Producer {
  case object SendWork
  case object Close
  case class Retry(work: Worker.Work)
  
  def props = Props(classOf[Producer])
}

class Producer extends Actor with ActorLogging {
  import Producer._
  implicit val dispatcher = context.system.dispatchers.lookup("main-scheduler")

  private[this] lazy val master = context.actorSelection("/user/master")

  private var buffer = Queue.empty[Worker.Work]

  private var retryCount = 0
  /*
   * 処理時間 => 100 millisec => 0.1
   * リクエスト数 => 20/s
   * 
   * リトルの法則に当てはめると、2(= 20 * 0.1)スレッドあればレイテンシを捌けるはず。
   * オーバーヘッドを含めると3スレッドくらいまでスケールするはず
   */
  val c = context.system.scheduler.schedule(0.second, 50.milliseconds) {
    self ! SendWork
  }
  context.system.scheduler.scheduleOnce(10.seconds) {
    c.cancel()
    self ! Close
  }

  def receive = {
    case Retry(work) =>
      retryCount += 1
      buffer += work
      log.info("retry count = " + retryCount)
    case SendWork =>
      val work =
        if (buffer.isEmpty) Worker.Work(Random.nextString(10))
        else buffer.dequeue()
      master ! work
    case Close =>
      if (buffer.isEmpty) context.system.shutdown()
      else {
        val work = buffer.dequeue()
        context.system.scheduler.scheduleOnce(50.millisecond) {
          master ! work
          self ! Close
        }
      }
  }
}