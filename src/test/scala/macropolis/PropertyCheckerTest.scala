package macropolis

object PropertyCheckerTest extends App {
  import PropertyChecker._

  val ok = p"my.property.key"
  println(ok)
  val nok = p"my.property.hey"
  println(nok)
}
