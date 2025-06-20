// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_EXTRA_CHECKERS
// JET-72 Type inference doesn't work when iterating over ArrayList

import java.util.ArrayList

abstract class Item(val room: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) {
   abstract val name : String
}

val items: ArrayList<Item> = ArrayList<Item>()

fun test(room : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>Object<!>) {
  for(item: Item? in items) {
    if (item?.room === room) {
      // item?.room is not null
      System.out.println("You see " + item<!UNNECESSARY_SAFE_CALL!>?.<!>name)
    }
  }
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, equalityExpression, flexibleType, forLoop,
functionDeclaration, ifExpression, javaFunction, javaProperty, localProperty, nullableType, primaryConstructor,
propertyDeclaration, safeCall, smartcast, stringLiteral */
