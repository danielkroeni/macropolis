package macropolis

import language.experimental.macros
import language.implicitConversions

object InternalExternalDSLTest extends App {
  import InternalExternalDSL._

  val a = schema"""
    graph ( MyGraph ) {
      node( A ) {
        prop( P1 )
      }
      edge( AtoB ){
        prop( P2 )
      }
    }
  """
  println(a)
}
