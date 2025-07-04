// RUN_PIPELINE_TILL: FRONTEND
package kt_250_617_10

import java.util.ArrayList
import java.util.HashMap

//KT-250 Incorrect variable resolve in constructor arguments of superclass
open class A(val x: Int)
class B(y: Int) : A(<!UNRESOLVED_REFERENCE!>x<!>)  //x is resolved as a property in a, so no error is generated

//KT-617 Prohibit dollars in call to superclass constructors
open class M(p: Int)
class N(val p: Int) : A(<!SYNTAX!><!SYNTAX!><!>$p<!><!SYNTAX!>)<!>

//KT-10 Don't allow to use properties in supertype initializers
open class Element()
class TextElement(name: String) : Element()

abstract class Tag(val name : String) {
  val children = ArrayList<Element>()
  val attributes = HashMap<String, String>()
}

abstract class TagWithText(name : String) : Tag(name) {
  operator fun String.unaryPlus() {
    children.add(TextElement(this))
  }
}

open class BodyTag(name : String) : TagWithText(name) {
}

class Body() : BodyTag(<!UNRESOLVED_REFERENCE!>name<!>) { // Must be an error!
}
class Body1() : BodyTag(<!NO_THIS!>this<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>name<!>) { // Must be an error!
}

//more tests

open class X(p: Int, r: Int) {
    val s = "s"
}

class Y(i: Int) : X(i, <!UNRESOLVED_REFERENCE!>rrr<!>) {
    val rrr = 3
}

class Z(val i: Int) : X(<!UNRESOLVED_REFERENCE!>s<!>, <!UNRESOLVED_REFERENCE!>x<!>) {
    val x = 2
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, integerLiteral,
javaFunction, operator, primaryConstructor, propertyDeclaration, stringLiteral, thisExpression */
