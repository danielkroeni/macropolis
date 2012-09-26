package macropolis

import language.experimental.macros
import language.implicitConversions
import scala.reflect.macros.Context
import collection.mutable.ListBuffer
import collection.mutable.Stack
import scala.reflect._

object Macropolis {

  def mequals[T <: AnyRef](me: T, any: Any, firstField: Any, fields: Any*): Boolean = macro mequals_impl[T]

  def mequals_impl[T <: AnyRef: c.AbsTypeTag](c: Context)(me: c.Expr[T], any: c.Expr[Any], firstField: c.Expr[Any], fields: c.Expr[Any]*): c.Expr[Boolean] = {
    import c.universe._


    def meType = implicitly[c.AbsTypeTag[T]].tpe

    def compareFields: c.Expr[Boolean] = {
      val otherAsT = c.universe.reify { val a = any.splice.asInstanceOf[T]; a}

      def compareSingleField(field: c.Expr[Any]): c.Expr[Boolean] = {
        val otherField = field.tree match {
          case Select(This(_), fieldTerm) => c.Expr[Boolean](Select(otherAsT.tree, fieldTerm)) //  case Select(owner, fieldTerm) if owner == me.tree => ...
          case i =>  c.abort(c.enclosingPosition, "Only simple field access is allowed! " + showRaw(i) + "      " + showRaw(me.tree))
        }
        reify { field.splice == otherField.splice }
      }

      fields.foldRight(compareSingleField(firstField)){ (f, expr) =>
        reify { expr.splice && compareSingleField(f).splice } }
    }

    val e = reify {
     if(any.splice.isInstanceOf[AnyRef]) {
        val anyRef = any.splice.asInstanceOf[AnyRef]
        if(me.splice.getClass.isAssignableFrom(anyRef.getClass)) {
          compareFields.splice
        } else {
          false
        }
      } else {
        false
      }
    }
    c.Expr[Boolean](c.resetAllAttrs(e.tree))
  }

  def mhash(firstField: Any, fields: Any*): Int = macro mhash_impl

  def mhash_impl(c: Context)(firstField: c.Expr[Any], fields: c.Expr[Any]*): c.Expr[Int] = {
    import c.universe._
    fields.foldRight(reify(41 + firstField.splice.hashCode)){ (f, expr) => reify((41 * expr.splice) + f.splice.hashCode) }
  }
}
