package models

import java.util.Date
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

/**
 * Case class of a recipe. Contains also some methods specific to one instance of a recipe.
 * Note that a case class is final to avoid border effects.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 *
 */
case class Recipe(name: String, author: String, preparation: String, nb: Int, date: Date = new Date(), id: Int = 0) {
  /**
   * return a sequence of all the ingredients of a recipe.
   */
  def getIngredients(): Seq[Ingredient] = { Ingredient.findByRecipe(this) }
  /**
   * Add a "like" on the recipe by the user passed in argument.
   */
  def like(user: User) { setLike(user, 1) }
  /**
   * Add a "dislike" on the recipe by the user passed in argument.
   */
  def dislike(user: User) { setLike(user, -1) }
  /**
   * Method used to implement like and dislike. its takes the value of like (1) and dislike (-1) in parameters.
   */
  def setLike(user: User, value: Int) {
    val like = (getLikeFromUser(user))
    if (like.isEmpty || getLikeFromUser(user).get != value) {
      DB.withConnection { implicit connection =>
        if (like.isEmpty) {
          SQL("INSERT INTO " + Recipe.likeTable + " values (NULL, {val}, {user}, {recipe})").on(
            'val -> value,
            'user -> user.email,
            'recipe -> this.id).executeUpdate()
        } else {
          SQL("update " + Recipe.likeTable + " SET grade = {val} WHERE user = {user} AND recipe = {recipe}").on(
            'val -> value,
            'user -> user.email,
            'recipe -> this.id).executeUpdate()
        }
      }
    }
  }
  /**
   * Return the like value from the user on this recipe.
   */
  def getLikeFromUser(user: User): Option[Int] = {
    DB.withConnection { implicit connection =>
      SQL(" SELECT (grade) FROM " + Recipe.likeTable + " WHERE recipe = {recipe} and user = {user} LIMIT 1").on(
        'user -> user.email,
        'recipe -> this.id).as(scalar[Int].singleOpt)
    }
  }
  /**
   * Get the number of likes among all the likes of the users.
   */
  def getLikes(): Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT COUNT(*) FROM " + Recipe.likeTable + " WHERE recipe = {recipe} AND grade = 1").on(
        'recipe -> this.id).as(scalar[Long].singleOpt)
    }.get
  }
  /**
   * Get the number of dislikes among all the likes of the users.
   */
  def getDislikes(): Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT COUNT(*) FROM " + Recipe.likeTable + " WHERE recipe = {recipe} AND grade = -1").on(
        'recipe -> this.id).as(scalar[Long].singleOpt)
    }.get
  }
  /**
   * return the like value among all the likes of the users.
   */

  def getBalancedLikes(): Long = this.getLikes() - this.getDislikes()
  /**
   * Return the author's user case.
   */
  def getAuthor: Option[User] = { User.find(this.author) }
  /**
   * Add an ingredient. Return true if the action was well done, false otherwise.
   */
  def AddIngredient(ingr: Ingredient, unities: String = null): Boolean = {
    if (!Ingredient.existsInRecipe(this, ingr.name)) {
      Ingredient.create(Ingredient(ingr.name, ingr.quantity, unities, this.id))
      true
    }
    false

  }

}

object Recipe {
  /**
   * Table's name of the recipe's tables.
   */
  val table = "recipes"
  /**
   * Table's name of the grades (like / dislike) tables.
   */
  val likeTable = "grades"
  /**
   * maximum of recipes retrieved from a list.
   */
  val maxList = 30
  /**
   * Level of tolerance in the search of recipes.
   * 0.4 means that we will retrieves recipes that match until the half of the arguments of the best matching one.
   */
  val levelOfToleranceInSearch = 0.4

  /**
   * Parser of the case class from the MySQL result.
   */
  val simple = {
    get[Int](this.table + ".id") ~
      get[String](this.table + ".name") ~
      get[String](this.table + ".author") ~
      get[String](this.table + ".preparation") ~
      get[Date](this.table + ".date") ~
      get[Int](this.table + ".nb") map {
        case id ~ name ~ author ~ preparation ~ date ~ nb => Recipe(name, author, preparation, nb, date, id)
      }
  }
  /**
   * Create a recipe in the database.
   */
  def create(recipe: Recipe): Recipe = {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO " + this.table + " VALUES (NULL, {name}, {author}, {preparation}, {date}, {nb})").on(
        'name -> recipe.name,
        'author -> recipe.author,
        'preparation -> recipe.preparation,
        'date -> recipe.date,
        'nb -> recipe.nb).executeUpdate()

      recipe.copy(id = SQL("SELECT MAX(id) as max FROM " + this.table).as(scalar[Int].single))
    }
  }
  /**
   * Find a recipe based on the ID.
   */
  def findById(id: Int): Option[Recipe] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE id = {id} LIMIT 1").on('id -> id).as(Recipe.simple.singleOpt)
    }
  }
  /**
   * Find a list of recipes based on the name.
   */
  def findByName(name: String): Seq[Recipe] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE name LIKE {name}").on('name -> name).as(Recipe.simple *)
    }
  }
  /**
   * Retrieves a list of latest recipes.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def findLastestRecipes(start: Int = 0, limit: Int = this.maxList): Seq[Recipe] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " ORDER BY date LIMIT {start},{limit}").on(
        'start -> start,
        'limit -> limit).as(Recipe.simple *)
    }
  }
  /**
   * Retrieve a list of the bests recipes.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def findBestRecipes(start: Int = 0, limit: Int = this.maxList): Seq[Recipe] = {
    DB.withConnection { implicit connection =>
      val names = SQL("SELECT (recipe) FROM " + this.likeTable + " GROUP BY recipe ORDER BY SUM(grade) DESC LIMIT {start},{limit}").on(
        'start -> start,
        'limit -> limit).as(scalar[Int] *)
      names.map(nm => Recipe.findById(nm).get)
    }
  }
  /**
   * Retrieve an ordered list of the recipes that  a user like.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def findLikedRecipes(user: User, start: Int = 0, limit: Int = this.maxList): Seq[Recipe] = {
    DB.withConnection { implicit connection =>
      val names = SQL("SELECT (recipe) FROM " + this.likeTable + " WHERE user = {user} GROUP BY recipe ORDER BY grade DESC LIMIT {start},{limit}").on(
        'user -> user.email,
        'start -> start,
        'limit -> limit).as(scalar[Int] *)
      names.map(nm => Recipe.findById(nm).get)
    }
  }
  def count: Long = {
    DB.withConnection { implicit connection =>
      SQL("SELECT COUNT(*) FROM " + this.table).as(scalar[Long].single)
    }
  }
  /**
   * Retrieve a sequence of all the Ids in the database.
   */
  def findAllIds() : Seq[Int] = {
     DB.withConnection { implicit connection =>
       SQL("SELECT (id) FROM " + this.table).as(scalar[Int]*)
     }
  }
  /**
   * Retrieve a list of recipes based on a partial name.
   */
  def searchByName(name: String): Seq[Recipe] = { findByName("%" + name + "%") }

  /**
   * Retrieves a list of recipes based on a ingredients list. Is non-close : an ingredient will give a list of suggestions.
   * By adding ingredients, the user will restrict its research.
   */
  def searchByIngredients(pro: Seq[Product]): Seq[Recipe] = {
    /**
     * Method to group the different recipes, while counting the number of ingredients found in the fridge for each one.
     */
    def group(recs: Map[Int, Int], ingrs: Seq[(Ingredient, Boolean)]): Map[Int, Int] = ingrs match {
      case Nil => recs
      case x :: xs => {
        if (x._2) {
          if (recs.contains(x._1.recipe)) {
            val counted = recs.get(x._1.recipe).get
            group(recs.updated(x._1.recipe, recs.get(x._1.recipe).get + 1), xs)
          } else group(recs + (x._1.recipe -> 1), xs)
        } else group(recs, xs)
      }
    }
    /**
     * Recursive method to iterate on the products to find the possible recipes.
     */
    def search(recs: Map[Int, Int], pros: Seq[Product]): Map[Int, Int] = pros match {
      case Nil => recs
      case x :: xs => {
        search(group(recs, Ingredient.searchByNameAndMaxQuantity(x.name, x.quantity)), xs)
      }
    }
    //First, we generate an ordered list of recipes tuppled with an average of matching and ordered.
    val lstNotFiltered = search(Map(), pro).toSeq.map(tup => (Recipe.findById(tup._1).get, tup._2)).map(tup => (tup._1, tup._2.toDouble / tup._1.getIngredients().length)).sortBy(-_._2)
    //We then take only the better matching ones with a % of tolerance, if many have the same average. We then order that, again, using the balanced like. We then just retrieves the list of recipies.
    lstNotFiltered.filter(tup => tup._2 >= lstNotFiltered.head._2 * this.levelOfToleranceInSearch).map(tup => (tup._1, tup._1.getBalancedLikes())).sortBy(-_._2).map(tup => tup._1)
  }
   def delete(recipe: Recipe) {
    delete(recipe.id)
  }
  /**
   * Delete a recipe in the database.
   * Delete also all the ingredients of the recipe.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def delete(recipeId : Int) {
    DB.withConnection { implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE id = {id} LIMIT 1").on('id -> recipeId).executeUpdate()
    }
    Ingredient.deleteByRecipe(recipeId)
    Shot.deleteByRecipe(recipeId)
  }
}
