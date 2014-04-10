package models
import play.api.db._
import play.api.Play.current
import play.api.libs.Crypto
import anorm._
import anorm.SqlParser._
import java.util.Date
/**
 * Case class of a product.
 * It contains also some specific methods to one instance of a product.
 * Note that a case class is final to avoid border effects.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
case class Product(name: String, quantity: Double, unities: String, category: String, user: String, date: Date = new Date(), id: Int = 0) {
  /**
   * Update the quantity of a product. Is mostly used by the substractQuantity method.
   * Note that this action doesn't check if the new quantity is valid or not !
   */
  def updateQuantity(quantity: Double): Option[Product] = {
    Product.synchronized { //SYNC: Because we don't want a user to change the name while he start the cooking operation !
      if (quantity != 0) {
        DB.withConnection(implicit connection =>
          SQL("UPDATE " + Product.table + " SET quantity = {quantity}, date={date} WHERE id = {id}").on(
            'quantity -> quantity,
            'date -> new Date(),
            'id -> this.id).executeUpdate())
        Option(this.copy(quantity = quantity))
      } else {
        Product.delete(this.id)
        None
      }
    }
  }
  /**
   * Update the name of the product in the database, only if this one isn't null or empty.
   */
  def updateName(name: String): Option[Product] = {
    Product.synchronized { //SYNC: Because we don't want a user to change the name while he start the cooking operation !
      if (name != null && name != "") {
        DB.withConnection(implicit connection =>
          SQL("UPDATE " + Product.table + " SET name = {name}, date = {date} WHERE id = {id}").on(
            'name -> name,
            'date -> new Date,
            'id -> this.id).executeUpdate())
        Option(this.copy(name = name))
      } else Option(this)
    }
  }
  /**
   * Subtract the quantity in argument from the quantity of the product.
   * Return a None argument if the action has deleted all the content (there is no more of the product !)
   * Note that this action doesn't check if the quantity can be subtract or not !
   */
  def substractQuantity(quantity: Double): Option[Product] = {
    val quantityToRemove = if (this.unities == "l" || this.unities == "kg") quantity / 1000 else quantity
    updateQuantity(this.quantity - quantityToRemove) //We round this, as the subtraction is not always perfect...
  }
  /**
   * Change the category of the product.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def updateCategory(cat: String): Product = {
    DB.withConnection(implicit connection =>
      SQL("UPDATE " + Product.table + " SET category = {category} WHERE id = {id}").on(
        'category -> cat,
        'id -> this.id).executeUpdate())
    this.copy(category = cat)
  }

}
object Product {
  /**
   * Table containing all the products.
   */
  val table = "fridge"
  /**
   * Parser of the case class from the MySQL result.
   */
  val simple = {
    get[Int](this.table + ".id") ~
      get[String](this.table + ".name") ~
      get[Double](this.table + ".quantity") ~
      get[String](this.table + ".unities") ~
      get[String](this.table + ".category") ~
      get[Date](this.table + ".date") ~
      get[String](this.table + ".user") map {
        case id ~ name ~ quantity ~ unities ~ category ~ date ~ user => Product(name, quantity, unities, category, user, date, id)
      }
  }
  /**
   * Create a product in the database.
   */
  def create(pro: Product): Product = {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO " + this.table + " values (NULL, {name}, {quantity}, {unities}, {category}, {user}, {date})").on(
        'name -> pro.name,
        'quantity -> pro.quantity,
        'unities -> pro.unities,
        'category -> pro.category,
        'user -> pro.user,
        'date -> pro.date).executeUpdate()
      pro.copy(id = SQL("SELECT MAX(id) as max FROM " + this.table).as(scalar[Int].single))
    }
  }
  /**
   * Find all the products of one user.
   */
  def findByUser(user: User): Seq[Product] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE user = {user}").on('user -> user.email).as(Product.simple *)
    }
  }
  /**
   * Find all the products of a user and of a specific category.
   */
  def findByUserAndCategory(user: User, category: String): Seq[Product] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE user = {user} AND category = {category}").on('user -> user.email, 'category -> category).as(Product.simple *)
    }
  }
  /**
   * Find a product of a user with a specific name.
   */
  def findByUserAndName(user: User, name: String): Option[Product] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE user = {user} AND name = {name} LIMIT 1").on('user -> user.email, 'name -> name).as(Product.simple.singleOpt)
    }
  }
  /**
   * Find a product based on its user and specific id.
   */
  def findByUserAndId(user: User, id: Int): Option[Product] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE user = {user} AND id = {id} LIMIT 1").on('user -> user.email, 'id -> id).as(Product.simple.singleOpt)
    }
  }
  /**
   * Search products based on names and users.
   */
  def searchByUserAndName(user: User, name: String): Seq[Product] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE user = {user} AND name LIKE {name} LIMIT 1").on('user -> user.email, 'name -> ("%" + name + "%")).as(Product.simple *)
    }
  }
  /**
   * Test if a product exists in the fridge.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def existsInFridge(user: User, name: String): Boolean = { findByUserAndName(user, name).isDefined }
  /**
   * Delete the product instance from the fridge.
   */
  def delete(id: Int) = {
    Product.synchronized { //SYNC: Because we don't want a user to change the name while he start the cooking operation !
      DB.withConnection(implicit connection =>
        SQL("DELETE FROM " + this.table + " WHERE id = {id} LIMIT 1").on('id -> id).executeUpdate())
    }
  }
  /**
   * delete all the product of a specific user.
   */
  def deleteByUser(user: User) = {
    Product.synchronized { //SYNC: Because we don't want a user to change the name while he start the cooking operation !
      DB.withConnection(implicit connection =>
        SQL("DELETE FROM " + this.table + " WHERE user = {user}").on('user -> user.email).executeUpdate())
    }
  }
}