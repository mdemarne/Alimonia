import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import controllers.Application
import controllers.Security
/**
 * Global settings of the app concerning actions.
 * 
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
object Global extends GlobalSettings {
  
  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String) = {
    Ok(views.html.error("Bad request", "With the following message : " +error))
  } 

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {
    Ok(views.html.error("Error", "A little problem has occur internaly. We will solve it shortly."))
  }

  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader): Result = {
    Ok(views.html.error("Page not found", "This page doesn't exists !"))
  }

}
