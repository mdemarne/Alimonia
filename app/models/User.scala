package models
import java.util.Date
import play.api.db._
import play.api.Play.current
import play.api.libs.Crypto
import anorm._
import anorm.SqlParser._
import scala.util.Random

/**
 * The case class used to store the informations of an user.
 * It contains some methods specific to an instance.
 * Note that a case class is final to avoid border effects.
 *
 * The password is encrypted in the database in order to not be readable.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
case class User(email: String, firstName: String, lastName: String, password: String, lastFridgeUpdate: Date = new Date()) {
  /**
   * Update the password.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def updatePassword(password: String): User = {
    DB.withConnection { implicit connection =>
      SQL("UPDATE " + User.table + " SET password = {password} WHERE email = {email}").on(
        'email -> email, 'password -> Crypto.sign(password)).executeUpdate()
    }
    this.copy(password = password)
  }
  /**
   * update the name (both first and last) for convenience.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def updateName(firstName: String, lastName: String): User = {
    DB.withConnection { implicit connection =>
      SQL("UPDATE " + User.table + " SET firstName = {firstName}, lastName = {lastName}  WHERE email = {email}").on(
        'email -> email, 'firstName -> firstName, 'lastName -> lastName).executeUpdate()
    }
    this.copy(firstName = firstName, lastName = lastName)
  }
  /**
   * Update the last fridge modification's date.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def updateLastFridgeUpdate(date: Date): User = {
    DB.withConnection { implicit connection =>
      SQL("UPDATE " + User.table + " SET lastFridgeUpdate = {date} WHERE email = {email}").on(
        'email -> email, 'date -> date).executeUpdate()
    }
    this.copy(lastFridgeUpdate = date)
  }
  /**
   * Simple method used to split text in ingredient's name.
   */
  def keywordsSplitter(txt: String): Array[String] = txt.replace(",", "").replaceAll("s ", " ").replaceAll("es ", " ").split(" ")
  /**
   * Return true if the ingredient's are present in the fridge's user.
   */
  def isSubtractable(recipe: Recipe): Boolean = {
    recipe.getIngredients().foreach { ingr =>
      var flag = false
      keywordsSplitter(ingr.name).foreach { part =>
        if (part.toLowerCase() == "pepper" || part.toLowerCase() == "salt" || part.toLowerCase() == "sugar") flag = true
        Product.searchByUserAndName(this, part).foreach { pro =>
          val quantityOfProduct = if (pro.unities == "l" || pro.unities == "kg") pro.quantity * 1000 else pro.quantity
          if (quantityOfProduct >= ingr.quantity) flag = true
        }
      }
      if (flag == false) return false
    }
    return true
  }
  /**
   * Subtract all the recipe's ingredients from the products in the fridge's user.
   * If it's not subtractable, nothing is done.
   */
  def subtractQuantites(recipe: Recipe) : Boolean = {
    Product.synchronized { //SYNC : A user could always want to delete an ingredient while he cook a recipe !
      val substr = isSubtractable(recipe)
      if (substr) recipe.getIngredients().foreach(ingr => removeFromIngredient(ingr))
      return substr
    }
  }
  /**
   * Delete the quantity of a specific ingredient of a product. We do a double foreach for this, as we are not sure of the total match of the names.
   * This makes the algorithm less accurate, but more flexible. Some errors could occur.
   */
  def removeFromIngredient(ingr: Ingredient): Boolean = {
    def isCompatibleUnities(ProductU : String, ingrU : String) : Boolean = ProductU == ingrU || (ProductU == "kg" && ingrU == "g") || (ProductU == "l" && ingrU == "ml")  
    DB.withConnection { implicit connection =>
      keywordsSplitter(ingr.name).foreach { part =>
          Product.searchByUserAndName(this, part).foreach { foundPart =>
            val foundQuantity = if (foundPart.unities == "kg" || foundPart.unities == "l") foundPart.quantity*1000 else foundPart.quantity
            if (foundQuantity >= ingr.quantity) {
              foundPart.substractQuantity(ingr.quantity)
              return true
          }
        }
      }
      return false
    }
  }
  /**
   * Return the full name. Used to have a better printing !
   */
  def getFullName: String = this.firstName + " " + this.lastName
}
/**
 * Object implementing the statics methods of User accesses.
 */
object User {
  /**
   * Default table of the user inside the database.
   */
  val table = "users"
  /**
   * Parser of the case class from the MySQL result.
   */
  val simple = {
    get[String](this.table + ".email") ~
      get[String](this.table + ".firstName") ~
      get[String](this.table + ".lastName") ~
      get[String](this.table + ".password") ~
      get[Date](this.table + ".lastFridgeUpdate") map {
        case email ~ firstName ~ lastName ~ password ~ date => User(email, firstName, lastName, password, date)
      }
  }
  /**
   * Create a row corresponding to the user in argument in the database.
   */
  def create(user: User): User = {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO " + this.table + " VALUES ({email}, {firstName}, {lastName}, {password}, {lastFridgeUpdate})").on(
        'email -> user.email,
        'firstName -> user.firstName,
        'lastName -> user.lastName,
        'password -> Crypto.sign(user.password),
        'lastFridgeUpdate -> user.lastFridgeUpdate).executeUpdate()
    }
    user
  }
  /**
   * Find a selected user in the database.
   */
  def find(email: String): Option[User] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE email = {email}").on('email -> email).as(User.simple.singleOpt)
    }
  }
  /**
   * Authenticate a User for email and password.
   */
  def authenticate(email: String, password: String): Option[User] = {
    val userRtr = DB.withConnection(implicit Connection =>
      SQL("SELECT * FROM " + this.table + " WHERE email = {email}").on('email -> email).as(User.simple.singleOpt))
    if (userRtr.isDefined && userRtr.get.password == Crypto.sign(password)) userRtr
    else None
  }
  /**
   * Delete the user in the database.
   * Delete also all the products in the user's fridge.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def delete(user: User) = {
    DB.withConnection(implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE email={email} LIMIT 1").on('email -> user.email).executeUpdate())
    Product.deleteByUser(user)
  }
}
