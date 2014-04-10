package models

/**
 * This is a temporary product, not stored in the database. It is used in the case of the recipe searching for the temporary list.
 * @author Dev Gurjar
 * @author Mathieu Demarne
 */
case class TempProduct(name: String, quantity: Double, unities: String,id: Int) {
  /**
   * This method is only used to retrieve a temporary product as a product, in order to put it in the same list as the product issued from the fridge.
   */
  def AsProduct() : Product = Product(name, quantity, unities, "", "")
}