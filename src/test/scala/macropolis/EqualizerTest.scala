package macropolis

import language.experimental.macros

object EqualizerTest extends App {
  import Equalizer._

  class A(val b: Boolean, val s: String) {
    override def equals(other: Any): Boolean = mequals(this, other, b, s)
    override def hashCode: Int = mhash(b, s)
  }

  val a = new A(false, "hello")
  val b = new A(true, "hello")
  val c = new A(false, "ciao")
  println(a == a)
  println(a == b)
  println(a == c)
  println(a == 3)

  println(a.hashCode)
  println(b.hashCode)
  println(c.hashCode)
}
