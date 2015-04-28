package controllers

import models.User
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

/**
 * This controller handles the welcome page and the actions that doesn't need authentication, such as user creation.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
object Application extends Controller {
  /**
   * Main action of the website, index page !
   */
  def index = Action {
    Ok(views.html.index(Security.loginForm, registerForm))
  }

  //Registration actions & form : ---------------------------------------------------------------------------------

  /**
   * Declaration of the registration form and its test of consistency.
   */
  val registerForm = Form(
    tuple(
      "first_name" -> nonEmptyText,
      "last_name" -> nonEmptyText,
      "email" -> email,
      "confirm_email" -> email,
      "password" -> nonEmptyText,
      "confirm_password" -> nonEmptyText)
      verifying ("You must enter the same email !", result => result match {
        case (f, l, e, ce, p, cp) => e == ce
      })
      verifying ("You must enter the same password !", result => result match {
        case (f, l, e, ce, p, cp) => p == cp
      })
      verifying ("This email is already registered", result => result match {
        case (f, l, e, ce, p, cp) => User.find(e).isEmpty
      }))

  /**
   * Register action itself. If works, redirect to the fridge's page and set the session.
   */
  def register = Action { implicit request =>
    registerForm.bindFromRequest.fold(
      errorForm => BadRequest(views.html.index(Security.loginForm, errorForm)),
      args => {
        User.create(User(args._3, args._1, args._2, args._5))
        Redirect(routes.Fridges.fridge(null)).withSession("user" -> args._3)
      })
  }
}
