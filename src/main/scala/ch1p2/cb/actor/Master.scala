package ch1p2.cb.actor

import scala.concurrent.duration.DurationInt

import akka.actor.{ Actor, ActorLogging }
import akka.actor.{ Props }
import akka.actor.ActorSelection
import akka.pattern.{ CircuitBreaker, CircuitBreakerOpenException, ask, pipe }
import akka.util.Timeout

object Master {
  def props = Props[Master]
}

class Master extends Actor with ActorLogging {
  implicit val futureTimeout: Timeout = Timeout(1.seconds)

  private val worker = context.actorOf(Worker.routerProps, "worker")
  private lazy val producer = context.actorSelection("/user/producer")
  private val resizer = context.actorOf(PoolResizer.props(worker.path))

  private var numberOfPar = 1

  /*
   * Circuit Breaker
   */
  val breaker = new CircuitBreaker(
    context.system.scheduler,
    maxFailures = 5, // この回数を超えた失敗があった場合にはOpen状態になる。
    callTimeout = 150.milliseconds, // 失敗とみなす処理時間のしきい値。レイテンシを150 milli sec以下に抑えたい
    resetTimeout = 50.milliseconds // Open状態になってからHalf-Openになるまでの時間。
    )(context.dispatcher)
    .onOpen({
      log.info("circuit breaker opened.")
      resizer ! PoolResizer.Up // スレッドプールのサイズを増やす
    })
    .onClose({
      log.info("circuit breaker closed.")
      resizer ! PoolResizer.Down // スレッドプールのサイズを減らす
    })
    .onHalfOpen(log.info("circuit breaker half-opened."))

  val counter = context.actorOf(Props(new Actor with ActorLogging {
    var finishCount = 0
    def receive = {
      case Worker.Finished =>
        finishCount += 1
        log.info("finish count = " + finishCount)
    }
  }), "counter")

  var receiveCount = 0

  def receive = {
    case m: Worker.Work =>
      receiveCount += 1
      log.info("receive count=" + receiveCount)
      implicit val dispatcher = context.system.dispatchers.lookup("master-future-dispatcher")
      
      // Circuit Breakerを使ってメッセージを
      val future = breaker.withCircuitBreaker {
        val future = worker ? m
        pipe(future) to counter// 結果を数える。
        future
      }
      future.onFailure {
        case ce: CircuitBreakerOpenException =>
          producer ! Producer.Retry(m) // 失敗したメッセージをリカバリ処理に乗せる。
        case t: Throwable =>
          log.warning(t.toString())
      }
  }
}