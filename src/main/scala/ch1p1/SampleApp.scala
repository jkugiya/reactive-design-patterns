package ch1p1

import akka.actor.ActorSystem
import scala.concurrent.duration._
import ch1p1.consumers.SummarizerSupervisor
import ch1p1.services.FrontEnd
import ch1p1.services.LogStoreSupervisor
import ch1p1.models.protocols.AllLog
import com.typesafe.config.ConfigFactory
import ch1p1.producers.ItemProducer
import scala.util.Random

object SampleApp extends App {
  val system = ActorSystem("sample", ConfigFactory.load())
  
  val frontEnd = system.actorOf(FrontEnd.props, "front-end")
  val logStore = system.actorOf(LogStoreSupervisor.props, "log-store")
  val consumer = system.actorOf(SummarizerSupervisor.props, "consumer")

  implicit val ec = system.dispatcher
  var logid = 1
  // 一定間隔で適当なログを作って処理させる
  system.scheduler.schedule(0.second, 5.millisecond) {
    val item = ItemProducer.getOne
    frontEnd ! AllLog(
        id = logid,
        time = System.currentTimeMillis(),
        categoryId = item.group.category.id,
        groupId = item.group.id,
        itemId = item.id,
        price = item.price,
        discount = 0,
        number = Random.nextInt(10) + 1
    )
    logid += 1
  }
 
  // OutOfMemoryとか落ちたりしないはず・・・。
  system.awaitTermination()
}