package jobs
import models.Recipe
import scala.actors.Actor

object RecipeCleaner {
  var nbCleaned = 0
	def cleaner {
	  Recipe.findAllIds().foreach { id =>
	    if (Recipe.findById(id).get.getIngredients().length == 0) {
	      Recipe.delete(id)
	      nbCleaned +=1
	    }
	  }
	}
  def startCleaner {
    new Actor { override def act() = { cleaner } }.start
  }
  def getNbCleaned() : Int = nbCleaned
	
}