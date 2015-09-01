package ch1p1.producers

import scala.util.Random
import scala.annotation.tailrec
import akka.actor.Actor
import scala.concurrent.duration._

/**
 * ログを生成するMock
 */
object ItemProducer {
  case class Category(id: Long)
  case class Group(id: Long, category: Category)
  case class Item(id: Long, group: Group, price: Int)

  def createItems(): Seq[Item] =
    for {
      category <- for (i <- 1 to 10) yield Category(i)
      group <- for (i <- 1 to 10) yield Group((category.id - 1) * 10 + i, category)
      item <- for (i <- 1 to 10) yield Item((group.id - 1) * 100 + i, group, createPrice(5000))
    } yield item

  val items = createItems()
  def getOne: Item = {
    val id = Random.nextInt(10000) + 1
    items.find(_.id == id) getOrElse getOne
  }

  @tailrec
  def createPrice(n: Int): Int = {
    val price = Random.nextInt(n) + 1
    if (price >= 100 && price <= n) price
    else createPrice(n)
  }
}
