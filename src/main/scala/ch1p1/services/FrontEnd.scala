package ch1p1.services

import akka.actor.Actor
import akka.actor.ActorRef
import ch1p1.models.protocols._
import akka.routing.FromConfig
import akka.actor.Props

object FrontEnd {
  def props = FromConfig.props(Props[FrontEnd])
}

/**
 * 最初にログを受け取るActor。
 * 受け取ったログはそれぞれの集計サービスに転送する。
 */
class FrontEnd extends Actor {
  
  lazy val categoryLogStore = context.actorSelection("/user/log-store/category")
  lazy val groupLogStore = context.actorSelection("/user/log-store/group")
  lazy val itemLogStore = context.actorSelection("/user/log-store/item")

  def receive = {
    case log :AllLog =>
      categoryLogStore ! CategoryLog(log)
      groupLogStore ! GroupLog(log)
      itemLogStore ! ItemLog(log)
  }
}