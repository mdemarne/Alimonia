package controllers

import models.User
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

/**
 * This Controller takes care of all the actions related to the authentications and user manipulation.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
object Security extends Controller {
  //Redefinition of requests and actions : --------------------------------------------------------------------------------
  /**
   * New kind of request, containing a user. It is used when a user is authenticated.
   */
  case class AuthRequest(user: User, request: Request[AnyContent]) extends WrappedRequest(request)
  /**
   * Action-type used for authenticated actions.
   */

  def authenticated(f: AuthRequest => Result) = {
    Action { request =>
      request.session.get("user").flatMap(u => User.find(u)).map { user =>
        f(AuthRequest(user, request))
      }.getOrElse(Redirect(routes.Application.index()).withNewSession)
    }
  }
  //Definition of the various forms & actions specific to the security  : --------------------------------------------------
  /**
   * Form of authentication.
   */
  val loginForm = Form(
    tuple(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText) verifying ("Invalid email or password", result => result match {
        case (email, password) => User.authenticate(email, password).isDefined
      }))
  /**
   * Action of authentication, redirect to the main page with an error if not working, or to the fridge O/W.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => BadRequest(views.html.index(errorForm, Application.registerForm)),
      args => Redirect(routes.Fridges.fridge(null)).withSession("user" -> args._1))
  }
  /**
   * Erase the session and redirect to the index.
   */
  def logout = Action { implicit request =>
    Redirect(routes.Application.index()).withNewSession
  }
    //Definition of other security functions : ------------------------------------------------------------------------------
  /**
   * Just a little function to ensure no break in the system... (We never know what the testers could do !)
   */
  def isPositiveDouble(txt : String)(canBeEmpty : Boolean) : Boolean = {
    if (canBeEmpty && txt == "") return true
    try txt.toDouble > 0
    catch {case _: Throwable => false}
  }
  /**
   * Same thing for the lists.
   */
  def isListOfPositiveDouble(txts : List[String])  : Boolean = txts.map(v => isPositiveDouble(v)(false)).fold(true)((x,y) => x && y)
}
