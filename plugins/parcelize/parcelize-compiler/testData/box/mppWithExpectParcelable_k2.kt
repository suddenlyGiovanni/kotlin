// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB
// DIAGNOSTICS: -EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE

// Metadata compilations do not see through the expect/actuals and therefore,
// there are parcelize errors in metadata compilations.

// MODULE: m1-common
// FILE: common.kt

package test

expect interface MyParcelable

annotation class TriggerParcelize

@TriggerParcelize
data class <!NO_PARCELABLE_SUPERTYPE{METADATA}!>User<!>(val name: String) : MyParcelable

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt

@file:JvmName("TestKt")
package test

import kotlinx.parcelize.*

actual typealias MyParcelable = android.os.Parcelable

fun box() = parcelTest { parcel ->
    val user = User("John")
    user.writeToParcel(parcel, 0)

    val bytes = parcel.marshall()
    parcel.unmarshall(bytes, 0, bytes.size)
    parcel.setDataPosition(0)

    val user2 = parcelableCreator<User>().createFromParcel(parcel)
    assert(user == user2)
}
