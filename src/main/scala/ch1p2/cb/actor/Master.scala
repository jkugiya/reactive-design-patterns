package ch1p2.cb.actor

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.pattern.CircuitBreaker
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import scala.concurrent.duration._
import akka.routing.AdjustPoolSize
import akka.actor.Props
import akka.pattern.CircuitBreakerOpenException

object Master {
  def props = Props[Master]
}

class Master extends Actor with ActorLogging {

  implicit val futureTimeout: Timeout = Timeout(1.seconds)
  val worker = context.actorOf(Worker.routerProps, "worker")
  lazy val producer = context.actorSelection("/user/producer")
  
  
  var numberOfPar = 1
  var isOpen = false

  val breaker = new CircuitBreaker(
    context.system.scheduler,
    maxFailures = 5,
    callTimeout = 150.milliseconds, // レイテンシを150 milli sec以下に抑えたい
    resetTimeout = 0.seconds)(context.dispatcher)
    .onOpen({
      if (!isOpen) {
        numberOfPar += 7
        worker ! AdjustPoolSize(7)
      }
      log.info(s"circuit breaker opened. par = " + numberOfPar)
      isOpen = true
    })
    .onClose({
      numberOfPar -= 6
      worker ! AdjustPoolSize(-6)
      log.info("circuit breaker closed. par = " + numberOfPar)
      isOpen = false
    })
    .onHalfOpen(log.info("circuit breaker half-opened"))

  val counter = context.actorOf(Props(new Actor with ActorLogging {
    var count = 0
    def receive = {
      case Worker.Finished =>
        count += 1
        log.info("count = " + count)
    }
  }), "counter")

  var count = 0
  def receive = {
    case m: Worker.Work =>
      count += 1
      log.info("receive count=" + count)

      implicit val dispatcher = context.system.dispatchers.lookup("master-future-dispatcher")
      val future = breaker.withCircuitBreaker {
        val future = worker ? m
        pipe(future) to counter
        future
      }
      future.onFailure {
        case ce: CircuitBreakerOpenException =>
          producer ! Producer.Retry(m)
        case t: Throwable =>
          log.warning(t.toString())
      }
  }
}