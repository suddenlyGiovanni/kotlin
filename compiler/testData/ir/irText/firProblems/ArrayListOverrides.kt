// FULL_JDK
// TARGET_BACKEND: JVM_IR

// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ Value parameters in fake overrides generated by K1 and K2 are different

class A1 : java.util.ArrayList<String>()

class A2 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
}
