package models
import play.api.db._
import play.api.Play.current
import play.api.libs.Crypto
import anorm._
import anorm.SqlParser._
/**
 * Class of an ingredient.
 * Note that a case class is final to avoid border effects.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
case class Ingredient(name: String, quantity: Double, unities: String = null, recipe: Int, id: Int = 0)

object Ingredient {
  /**
   * table of the ingredients in the database.
   */
  val table = "ingredients"
  /**
   * Parser of the case class from the MySQL result.
   */
  val simple = {
    get[Int](this.table + ".id") ~
      get[String](this.table + ".name") ~
      get[Double](this.table + ".quantity") ~
      get[String](this.table + ".unities") ~
      get[Int](this.table + ".recipe") map {
        case id ~ name ~ quantity ~ unities ~ recipe => Ingredient(name, quantity, unities, recipe, id)
      }
  }
  /**
   * Create a new ingredient.
   */
  def create(ingr: Ingredient): Ingredient = {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO " + this.table + " values (NULL, {name}, {quantity}, {unities}, {recipe})").on(
        'name -> ingr.name,
        'quantity -> ingr.quantity,
        'unities -> ingr.unities,
        'recipe -> ingr.recipe).executeUpdate()
      ingr.copy(id = SQL("SELECT MAX(id) as max FROM " + this.table).as(scalar[Int].single))
    }
  }
  /**
   * Find a list of ingredient corresponding to a recipe Id.
   */
  def findByRecipe(recipe: Recipe): Seq[Ingredient] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE recipe = {recipe}").on('recipe -> recipe.id).as(Ingredient.simple *)
    }
  }
  /**
   * Retrieve a list of ingredients based on a name, and a boolean flag. true if there is enough, false otherwise.
   */
  def findByNameAndMaxQuantity(name: String, quantity: Double): Seq[(Ingredient, Boolean)] = {
    DB.withConnection { implicit connection =>
      val ingrs = SQL("SELECT * FROM " + this.table + " WHERE name LIKE {name}").on('name -> name).as(Ingredient.simple *)
      ingrs.map(ingr => {
        if (ingr.quantity > quantity) (ingr, false) else (ingr, true)
      })
    }
  }
  /**
   * Retrieve a list of ingredients based on a name, and a boolean flag. true if there is enough, false otherwise.
   */
  def searchByNameAndMaxQuantity(name: String, quantity: Double): Seq[(Ingredient, Boolean)] = findByNameAndMaxQuantity("%" + name.replaceAll(" ", "%") + "%", quantity)
  /**
   * return true is a ingredient's name exists in a specific recipe.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action. 
   */
  def existsInRecipe(recipe: Recipe, name: String): Boolean = {
    val ret = DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE recipe = {recipe} AND name = {name}").on('recipe -> recipe.id, 'name -> name).as(Ingredient.simple.singleOpt)
    }
    ret.isDefined
  }
  /**
   * Delete the ingredient passed in argument.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def delete(id: Int) = {
    DB.withConnection(implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE id = {id} LIMIT 1").on('id -> id).executeUpdate())
  }
  /**
   * Delete all the ingredient of a specific recipe.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def deleteByRecipe(recipeId: Int) = {
    DB.withConnection(implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE recipe = {recipe}").on('recipe -> recipeId).executeUpdate())
  }
}