package ch1p2.cb.actor

import akka.actor.Actor
import akka.actor.ActorPath
import akka.routing.AdjustPoolSize
import akka.actor.Props
import akka.actor.ActorLogging

object PoolResizer {
  
  case object Up

  case object Down
  
  def props(routerPath: ActorPath) = Props(classOf[PoolResizer], routerPath)

}

class PoolResizer(routerPath: ActorPath) extends Actor with ActorLogging {
  import PoolResizer._
  private val router = context.actorSelection(routerPath)
  private var isOpen = false
  private var poolSize = 1
  
  def receive = {
    case Up if !isOpen =>
      isOpen = true
      router ! AdjustPoolSize(7)
      poolSize += 7
      log.info("Pool Size = " + poolSize)
    case Down =>
      router ! AdjustPoolSize(-6)
      isOpen = false
      poolSize -= 6
      log.info("Pool Size = " + poolSize)
    case _ => // nothing to do
  }
}