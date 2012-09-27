package macropolis

import language.experimental.macros
import language.implicitConversions
import scala.reflect.macros.Context
import collection.mutable.ListBuffer
import collection.mutable.Stack
import scala.reflect._

object Macropolis {

  def mequals[T <: AnyRef](me: T, other: Any, firstField: Any, fields: Any*): Boolean = macro mequals_impl[T]

  def mequals_impl[T <: AnyRef :c.AbsTypeTag](c: Context)(me: c.Expr[T], other: c.Expr[Any], firstField: c.Expr[Any], fields: c.Expr[Any]*): c.Expr[Boolean] = {
    import c.universe._
    // def meType = reify { implicitly[c.AbsTypeTag[T]].tpe }

    def compareFields: c.Expr[Boolean] = {
      val otherAsT = reify { other.asInstanceOf[T] }

      def compareSingleField(field: c.Expr[Any]): c.Expr[Boolean] = {
        val otherField = field.tree match {
          case Select(owner:c.Tree, fieldTerm) if owner.symbol == me.tree.symbol => c.Expr[Boolean](Select(otherAsT.tree, fieldTerm))
          case illegalField =>  c.abort(c.enclosingPosition, "Only simple field access is allowed! Illegal field: " + show(illegalField))
        }
        reify { field.splice == otherField.splice }
      }

      fields.foldRight(compareSingleField(firstField)) { (f, expr) =>
        reify { expr.splice && compareSingleField(f).splice }
      }
    }

    val e = reify {
      val runtimeMirror = scala.reflect.runtime.currentMirror
      val otherType = runtimeMirror.classSymbol(other.splice.getClass).typeSignature
      val meType = runtimeMirror.classSymbol(me.splice.getClass).typeSignature

      other.splice match {
        case that: T if otherType <:< meType => (me.splice eq that) || compareFields.splice // here I would like to pass 'that: T' to compareFields
        case _ => false
      }
    }

    c.Expr[Boolean](c.resetAllAttrs(e.tree)) // what am I doing here?
  }

  def mhash(firstField: Any, fields: Any*): Int = macro mhash_impl

  def mhash_impl(c: Context)(firstField: c.Expr[Any], fields: c.Expr[Any]*): c.Expr[Int] = {
    import c.universe._
    fields.foldRight(reify { 41 + firstField.splice.hashCode })( (f, expr) => reify { (41 * expr.splice) + f.splice.hashCode } )
  }
}
