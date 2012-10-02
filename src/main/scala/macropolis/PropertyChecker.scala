package macropolis

import language.experimental.macros
import language.implicitConversions

import reflect.macros.Context
import util.Properties
import java.util.Properties
import java.net.URLClassLoader


object PropertyChecker {
  val fileName = "config.properties"

  implicit def propertyStringContext(sc: StringContext) = new PropertyStringContext

  class PropertyStringContext {
    def p(): String = macro pImpl
  }

  def pImpl(c: Context)(): c.Expr[String] = {
    import c.universe._

    lazy val key: String = c.prefix.tree match {
      /* Stolen from retronym's macrocosm */
      case Apply(_, List(Apply(_, List(Literal(Constant(string: String)))))) => string
      case x => c.abort(c.enclosingPosition, "unexpected tree: " + show(x))
    }

    val propStream =  c.libraryClassLoader.getResourceAsStream("config.properties")
    if (propStream == null) {
      c.warning(c.enclosingPosition, s"Unable to load property file $fileName. Be sure the config file is on the compile classpath.")
    } else {
      val props = new Properties
      props.load(propStream)

      if(!props.containsKey(key)) {
        c.warning(c.enclosingPosition, s"Key $key not found in $fileName")
      }
    }
    c.Expr(Literal(Constant(key)))
  }
}
