package ch1p1.consumers

import akka.actor.Actor
import ch1p1.models.protocols._
import akka.actor.Props
import akka.routing.FromConfig

case class Sum(sales: Int, number: Int)
case class SumUp(logs: Seq[Log])


object SummarizerSupervisor {
  def props = Props[SummarizerSupervisor]
}

class SummarizerSupervisor extends Actor {
  
  context.actorOf(FromConfig.props(Props(classOf[Summarizer], "CategoryLog")), "category")
  context.actorOf(FromConfig.props(Props(classOf[Summarizer], "GroupLog")), "group")
  context.actorOf(FromConfig.props(Props(classOf[Summarizer], "ItemLog")), "item")
  
  def receive = Actor.emptyBehavior
}

/**
 * ログを集計するActor
 */
class Summarizer(logName: String) extends Actor {
  
  type Id = Long

  // 集計結果
  var sum: Map[Id, Sum] = Map.empty

  def receive = {
    case SumUp(logs) =>
      logs.foreach { log =>
        val record =
          sum.get(log.id) map { before =>
            Sum(before.sales + log.price * log.number, before.number + log.number)
          } getOrElse {
            Sum(log.price, log.number)
          }
        sum += log.id -> record
        println(s"${logName}: id=${log.id}, sales=${record.sales}, number=${record.number}")
      }
  }
}