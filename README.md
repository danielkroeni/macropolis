macropolis
==========

Scala macro playground

## Equalizer

Automatically generates proper equals and hashCode methods.

```
import Equalizer._

class A(val b: Boolean, val s: String) {
  override def equals(other: Any): Boolean = mequals(this, other, b, s)
  override def hashCode: Int = mhash(b, s)
}
```

The following methods will be generated at compile time:

```
class A(val b: Boolean, val s: String) {
  override def equals(other: Any): Boolean = other match {
    case that: A => (this eq that) || (this.b == that.b) && (this.s == that.s)
    case _ => false
  }

  override def hashCode: Int =
    41 * (
      41 + b.hashCode
    ) + s.hashCode
}
```


## ExternalInternalDSL

Example for an external DSL embedded in a String and checked at compile time.
The syntax is checked by a Scala combinator parser. An additional function checks whether all names start with an uppercase letter.
Here is a valid example:

```
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

println(a) // prints Schema(Name(MyGraph),List(Node(Name(A),List(Property(Name(P1)))), Edge(Name(AtoB),List(Property(Name(P2))))))
```

### Syntactic checks
The following leads to a compile error showing a syntactic error:

```
import InternalExternalDSL._

val a = schema"""
  graph ( MyGraph ) {
    node( A ) {
      prop[ P1 )
    }
    edge( AtoB ){
      prop( P2 )
    }
  }
"""
```

Compiler error:
```
[info] Compiling 1 Scala source to /home/dk/macropolis/target/scala-2.10/test-classes...
[error] /home/dk/macropolis/src/test/scala/macropolis/InternalExternalDSLTest.scala:9: `(' expected but `[' found
[error]         prop[ P1 )
[error]             ^
[error] one error found
[error] (test:compile) Compilation failed
```

### Semantic Checks
This example shows multiple semantic errors at compile:

```
import InternalExternalDSL._

val a = schema"""
graph ( MyGraph ) {
  node( a ) {
    prop( P1 )
  }
  edge( AtoB ){
    prop( p2 )
  }
}
"""
```

Compiler error:
```
[info] Compiling 1 Scala source to /home/dk/macropolis/target/scala-2.10/test-classes...
[error] /home/dk/macropolis/src/test/scala/macropolis/InternalExternalDSLTest.scala:8: Name a must start with upprecase
[error]       node( a ) {
[error]             ^
[error] /home/dk/macropolis/src/test/scala/macropolis/InternalExternalDSLTest.scala:12: Name p2 must start with upprecase
[error]         prop( p2 )
[error]               ^
[error] two errors found
[error] (test:compile) Compilation failed
```

Just for fun the implementation avoids parsing the string again at runtime but serializes the constructed AST into a string literal which is then deserialized at runtime.

