package controllers

import java.io.File
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.Recipe
import models.Shot
import models.User
import models.Ingredient
import java.io.DataInputStream
import java.io.FileInputStream
import models.Product
import models.TempProduct
import play.api.cache.Cache
import play.api.Play.current
import controllers.Security.AuthRequest
/**
 * This controller handles all the action related to the recipes, such as search and creation, and detail retrieving.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
object Recipes extends Controller {

  //search recipes actions : -----------------------------------------------------------------------------------
  /**
   * Simple index page with no form sent, retrieving the list of temporary products.
   */
  def recipes = Security.authenticated { implicit request =>
    Ok(partialView(request)(null)(addItemForm, searchRecipeForm))
  }
  /**
   * Action to research the recipes based only on the fridge.
   */
  def searchRecipesFromFridge = Security.authenticated { implicit request =>
    val recipes = Recipe.searchByIngredients(Product.findByUser(request.user))
    Ok(partialView(request)(recipes)(addItemForm, searchRecipeForm))
  }
  /**
   * Search the recipes based on the temporary list. Possibility to add the the fridge too.
   */
  def searchRecipes(useFridge: String) = Security.authenticated { implicit request =>
    val nbProducts = if (request.session.get("nbProducts").isDefined) request.session.get("nbProducts").get.toInt else 0
    val tempProducts = findTempProducts(request.user, nbProducts)
    val products = tempProducts.map(pr => pr.AsProduct()) ++ (if (useFridge == "yes") (Product.findByUser(request.user)) else Nil)
    val recipes = Recipe.searchByIngredients(products)
    Ok(views.html.recipe(request.user)(findTempProducts(request.user, nbProducts))(recipes)(addItemForm, searchRecipeForm))
  }
  /**
   * Simple search by recipe name.
   */
  def searchRecipesByName = Security.authenticated { implicit request =>
    searchRecipeForm.bindFromRequest().fold(
      errorForm => BadRequest(partialView(request)(null)(addItemForm, errorForm)),
      arg => {
        val lst = Recipe.searchByName(arg).map(rec => (rec.getBalancedLikes(), rec)).sortBy(-_._1).map(tup => tup._2)
        Ok(partialView(request)(lst)(addItemForm, searchRecipeForm))
      })
  }
  //Temporary list actions : -----------------------------------------------------------------------------------
  /**
   * Add a product to the temporary list.
   */
  def addProduct = Security.authenticated { implicit request =>
    addItemForm.bindFromRequest().fold(
      errorForm => BadRequest(partialView(request)(null)(errorForm, searchRecipeForm)),
      args => {
        //Note that we use the session and the cache, which allow to have less data passed in the form when we do a search.
        //The data will remain in the cache for 20 minutes only.
        val x = if (request.session.get("nbProducts").isDefined) request.session.get("nbProducts").get.toInt + 1 else 0
        Cache.set("Product_" + request.user.email + "_" + x, TempProduct(args._1, args._2.toDouble, args._3, x), 1200)
        Redirect(routes.Recipes.recipes)
      })
  }
  /**
   * Remove a product to a temporary list.
   */
  def removeProduct(id: Int) = Security.authenticated { implicit request =>
    //We simply set the value in the cache to none, with the smallest time of life possible : 1 second.
    Cache.set("Product_" + request.user.email + "_" + id, None, 1)
    Redirect(routes.Recipes.recipes)
  }
  //Diverse related action & form --------------------------------------------------------------------------------------------------------------
  /**
   * Retrieve a partially set html page, with the list of temporary product already added.
   */
  def partialView(request: AuthRequest) = {
    val nbProducts = if (request.session.get("nbProducts").isDefined) request.session.get("nbProducts").get.toInt else 0
    views.html.recipe(request.user)(findTempProducts(request.user, nbProducts))_
  }
  /**
   * Find the list of temporary products in the cache for a user.
   */
  def findTempProducts(user: User, nb: Int): Seq[TempProduct] = {
    var seq = Seq[TempProduct]()
    for (i <- 0.until(nb + 1)) {
      val pro = Cache.getAs[TempProduct]("Product_" + user.email + "_" + i)
      if (pro.isDefined && pro.get != null) seq = seq :+ pro.get
    }
    return seq
  }
  /**
   * Simple form to add an item to the temporary products.
   */
  val addItemForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "quantity" -> nonEmptyText.verifying("The quantity must be a positive double !", v => Security.isPositiveDouble(v)(false)),
      "units" -> nonEmptyText))
  /**
   * Simple form of research : the field cannot be null.
   */
  val searchRecipeForm = Form("name" -> nonEmptyText)
  //Recipe detail and related actions : -----------------------------------------------------------------------------------
  /**
   * Retrieve the information of a specific recipe, given by its Id.
   * If no recipe correspond to this Id, we redirect the browser to the recipes page.
   */
  def recipeDetail(id: Int) = Security.authenticated { implicit request =>
    val recipe = Recipe.findById(id)
    if (recipe.isDefined) Ok(views.html.recipe_detail(recipe.get)(request.user))
    else Redirect(routes.Recipes.recipes)
  }
  /**
   * Retrieve the image of a recipe. If the Id correspond to no recipe, the image is white.
   */
  def recipeShot(recipeId: Int) = Security.authenticated { implicit request =>
    val shot = Shot.findByRecipe(recipeId)
    if (shot.headOption.isDefined) Ok(shot.head.data).as("image/jpg")
    else Ok("").as("image/jpg")
  }
  /**
   * Action to like a recipe. Redirect to the detail again.
   */
  def like = Security.authenticated { implicit request =>
    val recipeId = likeForm.bindFromRequest().get
    likeOrDislike(recipeId, 1, request.user)
    Redirect(routes.Recipes.recipeDetail(recipeId))
  }
  /**
   * Action to like a recipe. Redirect to the detail again.
   */
  def dislike = Security.authenticated { implicit request =>
    val recipeId = likeForm.bindFromRequest().get
    likeOrDislike(recipeId, -1, request.user)
    Redirect(routes.Recipes.recipeDetail(recipeId))
  }
  /**
   * Simple function to handle like/dislike, to avoid code duplication.
   */
  def likeOrDislike(id: Int, act: Int, user: User) {
    val rec = Recipe.findById(id)
    if (rec.isDefined) rec.get.setLike(user, act)
  }
  /**
   * Action to cook a recipe !
   * Will remove the quantities of the ingredients of the recipes from the fridge, or do nothing if the recipe is not removable.
   */
  def cook(id: Int) = Security.authenticated { implicit request =>
    val recipe = Recipe.findById(id)
    if (recipe.isDefined) {
    request.user.subtractQuantites(Recipe.findById(id).get) //This method will check if the ingredients are truly removable
    Redirect(routes.Recipes.recipeDetail(id))
    } else Redirect(routes.Recipes.recipes)
    
  }
  /**
   * Simple form for "(dis)like", based on a simple Id.
   */
  val likeForm = Form("recipeId" -> number)
  // Create a recipe actions-----------------------------------------------------------------------------------
  /**
   * Simply display the create page.
   */
  def create = Security.authenticated { implicit request => Ok(views.html.create(createForm)(null)) }
  /**
   * Retrieve the data from the request, test if the recipe is OK and create it.
   */
  def createAct = Security.authenticated { implicit request =>
    createForm.bindFromRequest.fold(
      errorForm => BadRequest(views.html.create(errorForm)(null)),
      args => {
        //There is no error in the form. Let's check the image !
        val img = request.body.asMultipartFormData.get.file("image").getOrElse(null)
        //If the image is empty, we redirect with an error message.
        if (img == null) BadRequest(views.html.create(createForm.fill(args))("You need to add an image !"))
        else {
          //We then check the image content type.
          if (img.contentType.isEmpty || !img.contentType.get.contains("image")) BadRequest(views.html.create(createForm.fill(args))("The image doesn't look like an image !"))
          else {
            //Everything seems now Ok. Let's create the recipe !
            val newRecipe = Recipe.create(Recipe(args._1, request.user.email, args._2, args._3))
            //The image is resized and created to fit the website.
            Shot.create(Shot(Shot.resize(new DataInputStream(new FileInputStream(img.ref.file)), 250, 250), newRecipe.id))
            //And finally, we create the ingredients using a recursion. 
            def createIngredients(names: List[String], quantities: List[String], units: List[String]): Int = (names, quantities, units) match {
              case (x :: xs, y :: ys, z :: zs) => {
                Ingredient.create(Ingredient(x, if (z == "l" || z == "kg") y.toDouble * 1000 else y.toDouble, z, newRecipe.id))
                createIngredients(xs, ys, zs)
              }
              case _ => 0
            }
            createIngredients(args._4, args._5, args._6) //The call to the method to create the ingredients !
            Redirect(routes.Recipes.recipeDetail(newRecipe.id))
          }
        }

      })
  }
  /**
   * Form of recipe creation. The names, quantity and units of the ingredients are stored as lists.
   * We need at least two ingredients for a recipe.
   */
  def createForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "preparation" -> nonEmptyText,
      "nb" -> number(min = 1, max = 6),
      "ingr_name" -> list(nonEmptyText),
      "ingr_quantity" -> list(nonEmptyText).verifying("The quantities must be a positive double !", vr => Security.isListOfPositiveDouble(vr)),
      "ingr_units" -> list(text)) verifying ("You need to add at least two ingredients !", result => result match {
        case (name, prep, nb, ingr_name, ingr_quan, ingr_unit) => {
          ingr_name.length > 1 && ingr_quan.length > 1 && ingr_unit.length > 1
        }
      }))

}
