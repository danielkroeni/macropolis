macropolis
==========

Scala macro playground

## Equalizer

Automatically generates proper `equals` and `hashCode` methods.

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


## PropertyChecker

Checks whether property keys are defined in a config file and warns at compile time:

config.properties
```
my.property.key = hello
```

```
import PropertyChecker._

val okKey = p"my.property.key"
println(okKey)                   // prints my.property.key

val nokKey = p"my.property.hey"
println(nokKey)                  // prints my.property.key

```

Compiler output:
```
[warn] /home/dk/macropolis/src/test/scala/macropolis/PropertyCheckerTest.scala:8: Key my.property.hey not found in config.properties
[warn]   val nok = p"my.property.hey"
[warn]             ^
[warn] one warning found
```

If you get the following warning it means that the compiler can not find the file `config.properties` on the classpath:
```
[warn] /home/dk/macropolis/src/test/scala/macropolis/PropertyCheckerTest.scala:6: Unable to load property file config.properties. Be sure the config file is on the compile classpath.
[warn]   val ok = p"my.property.key"
[warn]            ^
```

To make this working in xsbt the classpath needs to be extend to contain the config file.
In this project I added the following line to its build.sbt:
```
unmanagedClasspath in Test <+= (baseDirectory) map { bd => Attributed.blank(bd/"src"/"test"/"resources") }
```

### Notes
* Currently the name of the property file is hardcoded to `config.properties`.
* Would it make sense to have `v"my.property.key"` which statically resolves the `value` associated with the given key?


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
This example demonstrates multiple semantic errors at compile time:

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

Just for fun the implementation avoids parsing the string again at runtime:
It serializes the constructed AST at compile time into a string literal which is then deserialized at runtime.

