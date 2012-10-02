package macropolis

import language.experimental.macros
import language.implicitConversions

import util.parsing.input.{OffsetPosition, Position => LocalPosition, Positional}
import reflect.macros.Context
import util.parsing.combinator.RegexParsers


object InternalExternalDSL {

  implicit def schemaStringContext(sc: StringContext) = new SchemaStringContext

  class SchemaStringContext {
    def schema(): Schema = macro schemaImpl
  }

  def schemaImpl(c: Context)(): c.Expr[Schema] = {
    import c.universe._

    def extractString: String = c.prefix.tree match {
      /* Stolen from retronym's macrocosm */
      case Apply(_, List(Apply(_, List(Literal(Constant(string: String)))))) => string
      case x => c.abort(c.enclosingPosition, "unexpected tree: " + show(x))
    }

    def adjustPosition(p: LocalPosition): c.Position = {
      val pos = c.enclosingPosition
      p match {
        case o: OffsetPosition => pos.withPoint(pos.point + "schema\"\"\"".size + o.offset)
        case _ => pos
      }
    }

    def toStringLiteral[T <: Serializable](t: T): c.Expr[String] = {
      import java.io.ObjectOutputStream
      import java.io.ByteArrayOutputStream
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(t)
      val byteArray = baos.toByteArray
      c.Expr[String](Literal(Constant(byteArray.map(_.toChar).mkString))) // size of string literals in byte code is limited.
    }

    def fromStringLiteral[T: c.AbsTypeTag](s: c.Expr[String]): c.Expr[T] = {
      reify {
        import java.io.ObjectInputStream
        import java.io.ByteArrayInputStream
        val bais = new ByteArrayInputStream(s.splice.map{_.toByte}.toArray)
        val ois = new ObjectInputStream(bais)
        val res = ois.readObject()
        res.asInstanceOf[T]
      }
    }

    val tree = for {
      tree <- SchemaParser.parse(extractString).right
      checked <- SchemaChecker.check(tree).right
    } yield checked

    tree match {
      case Left(errors) =>
        errors.init.foreach{ case (msg, pos) => c.error(adjustPosition(pos), msg)}
        val (msg, pos) = errors.last
        c.abort(adjustPosition(pos), msg)
      case Right(t) =>
        // avoid parsing again
        val treeAsString = toStringLiteral(t)
        fromStringLiteral[Schema](treeAsString)
        // just parse again (but don't check again) at runtime
        // val string = c.Expr[String](Literal(Constant(extractString)))
        // reify[Schema] { SchemaParser.parse(string.splice).fold(_ => sys.error("Should not happen"), x => x) }
    }
  }
}

object SchemaChecker {
  def check(s: Schema): Either[Seq[(String, LocalPosition)], Schema] = {

    def recCheck(elem: AST): Seq[(String, LocalPosition)] = {
      elem match {
        case Schema(name, elems) => recCheck(name) ++ elems.flatMap(recCheck)
        case Node(name, props)   => recCheck(name) ++ props.flatMap(recCheck)
        case Edge(name, props)   => recCheck(name) ++ props.flatMap(recCheck)
        case Property(name)      => recCheck(name)
        case n@Name(name)        => if (!name.charAt(0).isUpper) Seq((s"Name $name must start with uppercase", n.pos)) else Seq()
      }
    }
    recCheck(s) match {
      case Seq() => Right(s)
      case a => Left(a)
    }
  }
}

object SchemaParser extends RegexParsers {
  def parse(in: String): Either[Seq[(String, LocalPosition)], Schema] = parseAll(schema, in) match {
    case Success(res, _) => Right(res)
    case NoSuccess(msg,next) => Left(Seq((msg, next.pos)))
  }

  def schema     = positioned("graph"~"("~> name ~")"~body ^^ { case name~_~ body => Schema(name, body)})
  def body       = "{"~>rep(elem)<~"}"
  def elem       = node | edge
  def node       = positioned("node"~"("~>name~")"~properties ^^ { case name~_~properties => Node(name, properties)})
  def edge       = positioned("edge"~"("~>name~")"~properties ^^ { case name~_~properties => Edge(name, properties)})
  def properties = "{"~>rep(property)<~"}"
  def property   = positioned("prop"~"("~>name<~")" ^^ { case name => Property(name)})
  def name       = positioned(ident ^^ { case name => Name(name)})
  def ident      = """[a-zA-Z_]\w*""".r
}

sealed trait AST extends Positional
case class Schema(name: Name, elems: List[NodeEdge]) extends AST
sealed trait NodeEdge extends AST
case class Node(name: Name, props: List[Property]) extends NodeEdge
case class Edge(name: Name, props: List[Property]) extends NodeEdge
case class Property(name: Name) extends AST
case class Name(name: String) extends AST
