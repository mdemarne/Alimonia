package controllers
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.Ingredient
import models.Product
import models.User
/**
 * This controller takes care of all the actions related to the fridge.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
object Fridges extends Controller {
  //Definition of the various forms  : --------------------------------------------------------------------------------
  /**
   * The search form : a simple non empty text.
   */
  val searchFridgeForm = Form("query" -> nonEmptyText)
  /**
   * The form to add a product : a product has a name, a quantity, and a units, the three of them non empty.
   */
  val addItemToFridgeForm = Form(
    tuple(
      "name" -> nonEmptyText,
      "quantity" -> nonEmptyText.verifying("The quantity must be a positive double !", v => Security.isPositiveDouble(v)(false)),
      "units" -> nonEmptyText))
  /**
   * The editItem form. This form allow the user to edit the name and the quantity. If empty, the old values will remain.
   */
  val editItemForm = Form(
    tuple(
      "id" -> number,
      "name" -> text,
      "quantity" -> text.verifying("The quantity must be a positive double !", v => Security.isPositiveDouble(v)(true))))
  /**
   * Simple form, taking a non empty number which correspond to the id of the product to be removed.
   */
  val removeItemForm = Form("id" -> number)
  //Diverse functions  : ----------------------------------------------------------------------------------------------
  /**
   * Retrieve a list of products based on a user and a category name, which could be null.
   * In this specific case, all the products of the user are retrieved.
   * The order of the product is first-created first-listed.
   */
  def retrieveProducts(user: User, category: String = null): Seq[Product] = {
    if (category == null || category == "null") Product.findByUser(user)
    else Product.findByUserAndCategory(user, category)
  }
  //Definition of the various actions  : -----------------------------------------------------------------------------
  /**
   * Simplest action, displaying the fridge depending of a choosen category.
   */
  def fridge(category: String = null) = Security.authenticated { implicit request =>
    Ok(views.html.fridge(category, retrieveProducts(request.user, category), searchFridgeForm, addItemToFridgeForm, editItemForm))
  }
  /**
   * Add an item to a fridge. The category in parameter will be the item's category.
   */
  def addItemToFridge(category: String = null) = Security.authenticated { implicit request =>
    addItemToFridgeForm.bindFromRequest().fold(
      errorForm => BadRequest(views.html.fridge(category, retrieveProducts(request.user, category), searchFridgeForm, errorForm, editItemForm)),
      args => {
        //We first check to know if a similar product isn't already in the fridge.
        if (!Product.findByUserAndName(request.user, args._1).isDefined) {
          Product.create(Product(args._1, args._2.toDouble, args._3, category, request.user.email))
          Redirect(routes.Fridges.fridge(category))
        } else BadRequest(views.html.fridge(category, retrieveProducts(request.user, category), searchFridgeForm, addItemToFridgeForm.fill(args), editItemForm, "This product already exists"))
      })
  }
  /**
   * Search diverse product in the fridge. Doesn't take care of the category.
   */
  def searchFridge = Security.authenticated { implicit request =>
    val query = searchFridgeForm.bindFromRequest()
    if (!query.hasErrors && query.get != "") Ok(views.html.fridge(null, Product.searchByUserAndName(request.user, query.get), searchFridgeForm, addItemToFridgeForm, editItemForm))
    else Redirect(routes.Fridges.fridge(null))
  }
  /**
   * Edit an item in a specific category, which is passed in argument just to retrieve properly the fridge after use.
   */
  def editItem(category: String = null) = Security.authenticated { implicit request =>
    editItemForm.bindFromRequest().fold(
      errorForm => BadRequest(views.html.fridge(category, retrieveProducts(request.user, category), searchFridgeForm, addItemToFridgeForm, errorForm)),
      args => {
        val product = Product.findByUserAndId(request.user, args._1)
        if (product.isDefined) {
          if (!Product.findByUserAndName(request.user, args._2).isDefined) {
            //Note that we edit one field only if this one is empty.
            if (args._2 != null && args._2 != "") product.get.updateName(args._2)
            if (args._3 != null && args._3 != "") product.get.updateQuantity(args._3.toDouble)
          } else BadRequest(views.html.fridge(category, retrieveProducts(request.user, category), searchFridgeForm, addItemToFridgeForm, editItemForm.fill(args), null, "This name is alreay used"))
        }
        Redirect(routes.Fridges.fridge(category))
      })
  }
  /**
   * Remove an item and redirect to the fridge category passed in parameter.
   */
  def removeItem(category: String = null) = Security.authenticated { implicit request =>
    Product.delete(removeItemForm.bindFromRequest().get)
    Redirect(routes.Fridges.fridge(category))
  }
}
