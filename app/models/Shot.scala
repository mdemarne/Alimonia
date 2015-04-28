package models

import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import java.sql.Blob
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.awt.AlphaComposite
import java.io.ByteArrayOutputStream

/**
 * The class used to store the informations of a shot.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
case class Shot(data: Array[Byte], recipe: Int, id: Int = 0)

object Shot {
  /**
   * The name of the table of shots in the database.
   */
  val table = "shots"
  /**
   * Create a shot in the database. Returns it with proper id.
   */
  def create(shot: Shot): Shot = {
    DB.withConnection { implicit connection =>
      SQL("INSERT INTO " + this.table + " values (NULL, {data}, {recipe})").on(
        'recipe -> shot.recipe,
        'data -> shot.data.toString).executeUpdate()
      shot.copy(id = SQL("SELECT MAX(id) as max FROM " + this.table).as(scalar[Int].single))
    }
  }
  /**
   * Find the shots of a recipe from the database.
   */
  def findByRecipe(recipe: Int): Seq[Shot] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM " + this.table + " WHERE recipe = {recipe}").on('recipe -> recipe)().map {
        case Row(id: Int, data: Array[Byte], recipe: Int) => Shot(data, recipe, id)
        case Row(id: Int, data: Blob, recipe: Int) => Shot(data.getBytes(1, data.length().toInt), recipe, id)
      }
    }
  }
  /**
   * Delete a specific shot form the database.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def delete(id: Int) = {
    DB.withConnection(implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE id = {id} LIMIT 1").on('id -> id).executeUpdate())
  }
  /**
   * Delete all shots from a specific recipe.
   * NOT USED : currently not used. Synchronization maybe to be implemented if called in a future action.
   */
  def deleteByRecipe(recipe: Int) = {
    DB.withConnection(implicit connection =>
      SQL("DELETE FROM " + this.table + " WHERE recipe = {recipe}").on('recipe -> recipe).executeUpdate())
  }
  /**
   * Function to resize a shot, based on Java. It returns an array of bytes ready to be stored in the database.
   */
  def resize(is: java.io.InputStream, maxWidth: Int, maxHeight: Int): Array[Byte] = {
    def imgToBytes(img: BufferedImage): Array[Byte] = {
      val baos = new ByteArrayOutputStream()
      ImageIO.write(img, "jpg", baos)
      baos.flush()
      val imageInByte = baos.toByteArray()
      baos.close()
      imageInByte
    }
    val originalImage = ImageIO.read(is)
    val scaledImage = new BufferedImage(250, 250, BufferedImage.TYPE_INT_RGB)
    val g = scaledImage.createGraphics
    g.setComposite(AlphaComposite.Src)
    g.drawImage(originalImage, 0, 0, 250, 250, null);
    g.dispose
    imgToBytes(scaledImage)
  }
}
