package jobs
import play.libs.Akka
import java.net.URL
import java.io.DataInputStream
import models.Recipe
import models.Shot
import models.Ingredient
import scala.actors.Actor

/* DEPRECATED : Punchfork is closed. */

/**
 *
 * Basic script to extract data from the site Punchfork.com, with name, ingredients (names and quantities), image, description.
 * Only the retrieved recipes in HTML will be get. It is too complicated and time-eating to implement a script to hack Ajax queries.
 *
 * @author Dev Gurjar
 * @author Mathieu Demarne
 *
 * NOTES :
 * 	Extraction of the description : Too hard. Use a link to the desk instead.
 *  Extraction of the image : Stored in a array of Byte.
 *
 */
object PunchforkUpdater {
  /**
   * Base of the recipes URLs. Used to complete relative URLs extracted from the website.
   */
  val recipesUrl = "http://punchfork.com";
  /**
   * The list of source pages used to retrieve the recipes.
   */
  val sources = List("http://punchfork.com/",
    "http://punchfork.com/vegetarian",
    "http://punchfork.com/vegan",
    "http://punchfork.com/glutenfree",
    "http://punchfork.com/paleo",
    "http://punchfork.com/new/vegetarian",
    "http://punchfork.com/new/vegan",
    "http://punchfork.com/new/glutenfree",
    "http://punchfork.com/new/paleo",
    "http://punchfork.com/new",
    "http://punchfork.com/top/vegetarian",
    "http://punchfork.com/top/vegan",
    "http://punchfork.com/top/glutenfree",
    "http://punchfork.com/top/paleo",
    "http://punchfork.com/top",
    "http://punchfork.com/mostliked/vegetarian",
    "http://punchfork.com/mostliked/vegan",
    "http://punchfork.com/mostliked/glutenfree",
    "http://punchfork.com/mostliked/paleo",
    "http://punchfork.com/mostliked",
    "http://punchfork.com/recipes/fondue",
    "http://punchfork.com/recipes/shrimp-cocktail",
    "http://punchfork.com/recipes/tartiflette")
    val alphabet = List('a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z')
  def generateSources(sources : List[String], alp : List[Char]) : List[String] = alp match {
    case x :: xs =>
    	generateSources(("http://punchfork.com/recipes/" + x) :: sources, xs)
    case Nil => sources
  }
  //-----------------------------------------------------------------------
  /**
   * Return the list of the recipe's addresses, ready to be extracted.
   */
  def getRecipeAddress(sources: List[String]): List[String] = sources match {
    case x :: xs => extractRecipeURls(page(x)).map(ret => recipesUrl + ret) ::: getRecipeAddress(xs)
    case Nil => Nil
  }
  /**
   * Function using regex to extract the recipe's URLs from the page.
   */
  def extractRecipeURls(x: String): List[String] = {
    val reg = """<a href="(/recipe/.[^/"]*)""".r;
    return reg.findAllIn(x).matchData.map(vr => vr.group(1)).toList;
  }
  //-----------------------------------------------------------------------
  /**
   * Method used to extract the recipe's core : name, description, list of ingredients, pictures.
   */
  def getRecipeCores(sources: List[String]) {
    sources.map(src => extractRecipeCore(page(src)));
  }
  /**
   * The number of extracted recipes
   */
  var nbExtracted = 0
  /**
   * Here we go !
   * Method extracting the core of a recipe and storing it in the database.
   */
  def extractRecipeCore(x: String) {
    //Frist, we define the regex for the title, ingredients, description and image.
    val titleReg = """<h1 itemprop="name" class="title">(.[^<]*)</h1>""".r
    val ingrReg = """<span class="ingredient-name">(.[^<]*)</span> \(<span class="ingredient-n">(.[^<]*)</span>(?: <span class="ingredient-unit">(.[^<]*)</span>)*\)""".r
    val descReg = """<a class="source" href="/r\?url=(.[^"]*)" rel="nofollow" onClick="return gaTrackOutboundLink\('Outbound Click', '(?:.[^']*)', true\);"><img class="favicon" src="(?:.[^"]*)"/>(Read (?:.*?))</a>""".r
    val imgReg = """<img class="thumb" src="(.[^"]*)"""".r
    //We then get the potential names.
    val potentialName = titleReg.findAllIn(x).matchData.map(vr => vr.group(1))
    //And the potential descriptions, with adding html for research.
    val potentialDesc = descReg.findAllIn(x).matchData.map(vr => "<a href=\"" + vr.group(1).replaceAll("%2F", "/").replaceAll("%3A", ":") + "\" target=\"blank\">" + vr.group(2) + "</a>")
    //And the potential images.
    val potentialImg = imgReg.findAllIn(x).matchData.map(vr => vr.group(1))
    //We then check if none of them is empty.
    if (!potentialName.isEmpty && !potentialDesc.isEmpty && !potentialImg.isEmpty) {
      // We then just take care of some specific cases in the name string, due to encoding. It was in Html, it has to be transformed in plain text.
      val name = potentialName.next.replaceAll("&amp;", "&").replaceAll("&#39;", "\"")
      // We then check if the recipe already exists. If not, we create it !
      if (Recipe.findByName(name).isEmpty) {
        var recipe = Recipe.create(Recipe(name, "Anonymous", potentialDesc.next, 4))
        var img = Shot.create(Shot(getData(potentialImg.next), recipe.id)) //The images of Punchfork are already resized.
        var ingrLst = Map[String, (String, Double, String)]()
        //We then do an iteration to create all the ingredients.
        ingrReg.findAllIn(x).matchData.foreach(vr => {
          val converted = convert(trQuantity(vr.group(2)), if (vr != null) vr.group(3) else "units")
          if (!ingrLst.contains(vr.group(1))) {
            ingrLst = ingrLst.updated(vr.group(1).replaceAll("&#39;", "\""), (vr.group(1), converted._1, converted._2))
          } else {
            ingrLst = ingrLst.updated(vr.group(1).replaceAll("&#39;", "\""), (vr.group(1), converted._1 + ingrLst.get(vr.group(1)).get._2, converted._2))
          }
        })
        ingrLst.values.map(vl => Ingredient.create(Ingredient(vl._1, vl._2, vl._3, recipe.id)))
        nbExtracted += 1
      }
    }
  }
  //-----------------------------------------------------------------------
  /**
   * The quantity is generally stored as fractions, such as 2 1/12. This had to be transformed in double.
   * This method takes the string and return the corresponding double.
   */
  def trQuantity(src: String): Double = {
    return trSplit(src.split(" ").toList)
  }
  /**
   * We addition each part of the splitted string.
   */
  def trSplit(lst: List[String]): Double = lst match {
    case x :: xs => trPart(x) + trSplit(xs)
    case Nil => 0.0
  }
  /**
   * Transform the string into a double, even if it's a fraction.
   */
  def trPart(str: String): Double = {
    var ret = 0.0;
    try {
      ret = str.toDouble
    } catch {
      case _ =>
        var lst = str.split("/")
        ret = lst(0).toDouble / lst(1).toDouble
    }
    return ret;
  }
  //-----------------------------------------------------------------------
  /**
   * Method of unit conversion. It is partially correct, as american mesurments are difficult to handle.
   * Form example, a cup could be liquid or solid...
   */
  def convert(qu: Double, units: String): (Double, String) = (if (units != null) units.toLowerCase() else "") match {
    case "tsp" => tsp(qu)
    case "tsp." => tsp(qu)
    case "teaspoon" => tsp(qu)
    case "teaspoons" => tsp(qu)
    case "tsps" => tsp(qu)
    case "tsps." => tsp(qu)

    case "T" => tbsp(qu)
    case "tablespoon" => tbsp(qu)
    case "tablespoons" => tbsp(qu)
    case "tbsp" => tbsp(qu)
    case "tbsp." => tbsp(qu)
    case "tbsps" => tbsp(qu)
    case "tbsps." => tbsp(qu)

    case "drops" => tsp(qu / 2)
    case "drop" => tsp(qu / 2)

    case "cups" => cup(qu)
    case "cup" => cup(qu)
    case "pint" => pint(qu)
    case "pints" => pint(qu)

    case "splash" => tsp(qu / 4)
    case "splashes" => tsp(qu / 4)
    case "quart" => cup(4 * qu)
    case "quarts" => cup(4 * qu)

    case "gal" => gal(qu)
    case "gal." => gal(qu)
    case "gals." => gal(qu)
    case "gallon" => gal(qu)
    case "gallons" => gal(qu)

    case "ounce" => oz(qu)

    case "ounces" => oz(qu)
    case "oz" => oz(qu)
    case "pound" => lb(qu)
    case "pounds" => lb(qu)
    case "lb" => lb(qu)
    case "lb." => lb(qu)
    case "lbs" => lb(qu)
    case "lbs." => lb(qu)

    case "pinch" => pinch(qu)

    case "package" => pack(qu)

    case _ => (qu, "units")
  }
  //Converters : 
  def tsp(qu: Double): (Double, String) = (10 * qu, "g")
  def tbsp(qu: Double): (Double, String) = (15 * qu, "ml")
  def gal(qu: Double): (Double, String) = (15 * 3.78 * 1000, "ml")
  def cup(qu: Double): (Double, String) = (300 * qu, "g")
  def pint(qu: Double): (Double, String) = (500 * qu, "ml")
  def oz(qu: Double): (Double, String) = (30 * qu, "g")
  def pinch(qu: Double): (Double, String) = (0.25 * qu, "g")
  def lb(qu: Double): (Double, String) = (450 * qu, "g")
  def pack(qu: Double): (Double, String) = (20 * qu, "units")

  //-----------------------------------------------------------------------
  var run = 0 //Counter of requests
  /**
   * Transform the data get from a source to a string.
   */
  def page(source: String): String = {
    return new String(getData(source))
  }
  /**
   * Retrieve the data of a webpage in an array of bytes.
   */
  def getData(source: String): Array[Byte] = {
    run = run + 1
    val co = new URL(source).openConnection()
    val src = new DataInputStream(co.getInputStream())
    val ret = new Array[Byte](co.getContentLength())
    for (x <- 0 to ret.length - 1) ret.update(x, src.readByte())
    return ret
  }
  //-----------------------------------------------------------------------
  /**
   * To know if we have to start a new extractor or not !
   */
  var started = false
  /**
   * Will update the database
   */
  def update = {
    run = 0
    nbExtracted = 0
    started = false
    start
  }
  /**
   * start over the update if this one wasn't already started.
   */
  def start = {
    if (!started) {
      started = true
      new Actor {
        override def act() = {
          val srcs = getRecipeAddress(generateSources(sources,alphabet))
          srcs.map(src => new Actor { override def act() = { getRecipeCores(src :: Nil) } }.start)
        }
      }.start
    }
  }
  /**
   * return the number of request run.
   */
  def getRun: Int = run
  /**
   * return the number of recipe extracted and added to the database.
   */
  def getNbExtracted: Int = nbExtracted
}
