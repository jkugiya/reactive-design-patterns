package ch1p1.models


/**
 * Actor間でやり取りするメッセージ
 */
package object protocols {
 
  /**
   * 価格 / 割引額 / 数量のログ
   */
  trait Log {
    val id: Long
    val time: Long
    val price: Int
    val discount: Int
    val number: Int
  }
  case class AllLog(id: Long, time: Long, categoryId: Long, groupId: Long, itemId: Long, price: Int, discount: Int, number: Int) extends Log
 
  /**
   * 商品カテゴリごとのログ
   */
  case class CategoryLog(id: Long, time: Long, price: Int, discount: Int, number: Int) extends Log
  
  object CategoryLog {
    def apply(log: AllLog): CategoryLog = CategoryLog(
            id = log.categoryId,
            time = log.time,
            price = log.price,
            discount = log.discount,
            number = log.number
          )
  }

  /**
   * 商品グループごとのログ(Category > Group)
   */
  case class GroupLog(id: Long, time: Long, categoryId: Long, price: Int, discount: Int, number: Int) extends Log
  object GroupLog {
    def apply(log: AllLog): GroupLog = GroupLog(
            id = log.groupId,
            categoryId = log.categoryId,
            time = log.time,
            price = log.price,
            discount = log.discount,
            number = log.number
          )
  }
 
  /**
   * 商品ごとのログ
   */
  case class ItemLog(id: Long, time: Long, categoryId: Long, groupId: Long, price: Int, discount: Int, number: Int) extends Log
  object ItemLog {
    def apply(log: AllLog): ItemLog = ItemLog(
            id = log.itemId,
            groupId = log.groupId,
            categoryId = log.categoryId,
            time = log.time,
            price = log.price,
            discount = log.discount,
            number = log.number
          )
  }
}
