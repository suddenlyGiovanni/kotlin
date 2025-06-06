/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class CompilerLightClassTestGenerated extends AbstractCompilerLightClassTest {
  private void runTest(String testDataFilePath) {
    KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
  }

  public void testAllFilesPresentInLightClassByFqName() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true, "local", "ideRegression");
  }

  @TestMetadata("AnnotatedParameterInEnumConstructor.kt")
  public void testAnnotatedParameterInEnumConstructor() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotatedParameterInEnumConstructor.kt");
  }

  @TestMetadata("AnnotatedParameterInInnerClassConstructor.kt")
  public void testAnnotatedParameterInInnerClassConstructor() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotatedParameterInInnerClassConstructor.kt");
  }

  @TestMetadata("AnnotatedPropertyWithSites.kt")
  public void testAnnotatedPropertyWithSites() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotatedPropertyWithSites.kt");
  }

  @TestMetadata("annotatedReturnType.kt")
  public void testAnnotatedReturnType() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/annotatedReturnType.kt");
  }

  @TestMetadata("annotationBinaryRetention.kt")
  public void testAnnotationBinaryRetention() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/annotationBinaryRetention.kt");
  }

  @TestMetadata("AnnotationClass.kt")
  public void testAnnotationClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationClass.kt");
  }

  @TestMetadata("AnnotationJavaRepeatable.kt")
  public void testAnnotationJavaRepeatable() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationJavaRepeatable.kt");
  }

  @TestMetadata("AnnotationJvmRepeatable.kt")
  public void testAnnotationJvmRepeatable() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationJvmRepeatable.kt");
  }

  @TestMetadata("AnnotationKotlinAndJavaRepeatable.kt")
  public void testAnnotationKotlinAndJavaRepeatable() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationKotlinAndJavaRepeatable.kt");
  }

  @TestMetadata("AnnotationKotlinAndJvmRepeatable.kt")
  public void testAnnotationKotlinAndJvmRepeatable() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationKotlinAndJvmRepeatable.kt");
  }

  @TestMetadata("AnnotationRepeatable.kt")
  public void testAnnotationRepeatable() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/AnnotationRepeatable.kt");
  }

  @TestMetadata("BackingFields.kt")
  public void testBackingFields() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/BackingFields.kt");
  }

  @TestMetadata("classAndCompanionDelegatedProperty.kt")
  public void testClassAndCompanionDelegatedProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/classAndCompanionDelegatedProperty.kt");
  }

  @TestMetadata("classAndCompanionJvmFieldProperty.kt")
  public void testClassAndCompanionJvmFieldProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/classAndCompanionJvmFieldProperty.kt");
  }

  @TestMetadata("classAndCompanionProperty.kt")
  public void testClassAndCompanionProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/classAndCompanionProperty.kt");
  }

  @TestMetadata("classJvmFieldAndCompanionJvmFieldProperty.kt")
  public void testClassJvmFieldAndCompanionJvmFieldProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/classJvmFieldAndCompanionJvmFieldProperty.kt");
  }

  @TestMetadata("classJvmFieldAndCompanionProperty.kt")
  public void testClassJvmFieldAndCompanionProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/classJvmFieldAndCompanionProperty.kt");
  }

  @TestMetadata("ClassTypeParameterAnnotation.kt")
  public void testClassTypeParameterAnnotation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/ClassTypeParameterAnnotation.kt");
  }

  @TestMetadata("CompanionObject.kt")
  public void testCompanionObject() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/CompanionObject.kt");
  }

  @TestMetadata("CompanionObjectWithAConflictingProperty.kt")
  public void testCompanionObjectWithAConflictingProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/CompanionObjectWithAConflictingProperty.kt");
  }

  @TestMetadata("constructorWithValueClass.kt")
  public void testConstructorWithValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/constructorWithValueClass.kt");
  }

  @TestMetadata("constructorWithValueClassAsProperty.kt")
  public void testConstructorWithValueClassAsProperty() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/constructorWithValueClassAsProperty.kt");
  }

  @TestMetadata("Constructors.kt")
  public void testConstructors() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/Constructors.kt");
  }

  @TestMetadata("contextParameters.kt")
  public void testContextParameters() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/contextParameters.kt");
  }

  @TestMetadata("DataClassWithCustomImplementedMembers.kt")
  public void testDataClassWithCustomImplementedMembers() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DataClassWithCustomImplementedMembers.kt");
  }

  @TestMetadata("dataClassWithValueClass.kt")
  public void testDataClassWithValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/dataClassWithValueClass.kt");
  }

  @TestMetadata("DefaultImplsWithTypeParameters.kt")
  public void testDefaultImplsWithTypeParameters() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DefaultImplsWithTypeParameters.kt");
  }

  @TestMetadata("DelegatedNested.kt")
  public void testDelegatedNested() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DelegatedNested.kt");
  }

  @TestMetadata("Delegation.kt")
  public void testDelegation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/Delegation.kt");
  }

  @TestMetadata("Deprecated.kt")
  public void testDeprecated() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/Deprecated.kt");
  }

  @TestMetadata("DeprecatedEnumEntry.kt")
  public void testDeprecatedEnumEntry() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DeprecatedEnumEntry.kt");
  }

  @TestMetadata("DeprecatedNotHiddenInClass.kt")
  public void testDeprecatedNotHiddenInClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DeprecatedNotHiddenInClass.kt");
  }

  @TestMetadata("DollarsInName.kt")
  public void testDollarsInName() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DollarsInName.kt");
  }

  @TestMetadata("DollarsInNameNoPackage.kt")
  public void testDollarsInNameNoPackage() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/DollarsInNameNoPackage.kt");
  }

  @TestMetadata("EnumClass.kt")
  public void testEnumClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/EnumClass.kt");
  }

  @TestMetadata("EnumClassWithEnumEntries.kt")
  public void testEnumClassWithEnumEntries() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/EnumClassWithEnumEntries.kt");
  }

  @TestMetadata("EnumEntry.kt")
  public void testEnumEntry() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/EnumEntry.kt");
  }

  @TestMetadata("ExtendingInterfaceWithDefaultImpls.kt")
  public void testExtendingInterfaceWithDefaultImpls() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/ExtendingInterfaceWithDefaultImpls.kt");
  }

  @TestMetadata("HiddenDeprecated.kt")
  public void testHiddenDeprecated() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/HiddenDeprecated.kt");
  }

  @TestMetadata("HiddenDeprecatedInClass.kt")
  public void testHiddenDeprecatedInClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/HiddenDeprecatedInClass.kt");
  }

  @TestMetadata("InheritingInterfaceDefaultImpls.kt")
  public void testInheritingInterfaceDefaultImpls() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InheritingInterfaceDefaultImpls.kt");
  }

  @TestMetadata("InlineReified.kt")
  public void testInlineReified() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InlineReified.kt");
  }

  @TestMetadata("InterfaceTypeParameterAnnotation.kt")
  public void testInterfaceTypeParameterAnnotation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InterfaceTypeParameterAnnotation.kt");
  }

  @TestMetadata("InterfaceWithDefaultMethodAndCompanion.kt")
  public void testInterfaceWithDefaultMethodAndCompanion() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InterfaceWithDefaultMethodAndCompanion.kt");
  }

  @TestMetadata("internalValueClass.kt")
  public void testInternalValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/internalValueClass.kt");
  }

  @TestMetadata("InvalidJavaIdentifierAsAnnotationArgument.kt")
  public void testInvalidJavaIdentifierAsAnnotationArgument() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InvalidJavaIdentifierAsAnnotationArgument.kt");
  }

  @TestMetadata("InvalidJavaIdentifierAsPropertyInitializer.kt")
  public void testInvalidJavaIdentifierAsPropertyInitializer() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/InvalidJavaIdentifierAsPropertyInitializer.kt");
  }

  @TestMetadata("JavaBetween.kt")
  public void testJavaBetween() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/JavaBetween.kt");
  }

  @TestMetadata("JavaClassWithAnnotation.kt")
  public void testJavaClassWithAnnotation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/JavaClassWithAnnotation.kt");
  }

  @TestMetadata("JvmNameOnMember.kt")
  public void testJvmNameOnMember() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/JvmNameOnMember.kt");
  }

  @TestMetadata("JvmStatic.kt")
  public void testJvmStatic() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/JvmStatic.kt");
  }

  @TestMetadata("LocalFunctions.kt")
  public void testLocalFunctions() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/LocalFunctions.kt");
  }

  @TestMetadata("MethodTypeParameterAnnotation.kt")
  public void testMethodTypeParameterAnnotation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/MethodTypeParameterAnnotation.kt");
  }

  @TestMetadata("NestedObjects.kt")
  public void testNestedObjects() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/NestedObjects.kt");
  }

  @TestMetadata("NestedTypeAnnotations.kt")
  public void testNestedTypeAnnotations() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/NestedTypeAnnotations.kt");
  }

  @TestMetadata("NonDataClassWithComponentFunctions.kt")
  public void testNonDataClassWithComponentFunctions() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/NonDataClassWithComponentFunctions.kt");
  }

  @TestMetadata("OnlySecondaryConstructors.kt")
  public void testOnlySecondaryConstructors() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/OnlySecondaryConstructors.kt");
  }

  @TestMetadata("PrivateObject.kt")
  public void testPrivateObject() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/PrivateObject.kt");
  }

  @TestMetadata("privateValueClass.kt")
  public void testPrivateValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/privateValueClass.kt");
  }

  @TestMetadata("PropertyTypeParameterAnnotation.kt")
  public void testPropertyTypeParameterAnnotation() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/PropertyTypeParameterAnnotation.kt");
  }

  @TestMetadata("PublishedApi.kt")
  public void testPublishedApi() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/PublishedApi.kt");
  }

  @TestMetadata("SimpleObject.kt")
  public void testSimpleObject() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SimpleObject.kt");
  }

  @TestMetadata("SimplePublicField.kt")
  public void testSimplePublicField() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SimplePublicField.kt");
  }

  @TestMetadata("SpecialAnnotationsOnAnnotationClass.kt")
  public void testSpecialAnnotationsOnAnnotationClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SpecialAnnotationsOnAnnotationClass.kt");
  }

  @TestMetadata("StubOrderForOverloads.kt")
  public void testStubOrderForOverloads() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/StubOrderForOverloads.kt");
  }

  @TestMetadata("SubstitutionOverride.kt")
  public void testSubstitutionOverride() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SubstitutionOverride.kt");
  }

  @TestMetadata("SuperTypeWithBound.kt")
  public void testSuperTypeWithBound() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SuperTypeWithBound.kt");
  }

  @TestMetadata("SuperTypeWithBound2.kt")
  public void testSuperTypeWithBound2() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SuperTypeWithBound2.kt");
  }

  @TestMetadata("SuperTypeWithBoundKotlinCollection.kt")
  public void testSuperTypeWithBoundKotlinCollection() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/SuperTypeWithBoundKotlinCollection.kt");
  }

  @TestMetadata("Throws.kt")
  public void testThrows() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/Throws.kt");
  }

  @TestMetadata("TypePararametersInClass.kt")
  public void testTypePararametersInClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/TypePararametersInClass.kt");
  }

  @TestMetadata("UnitAsTypeAlias.kt")
  public void testUnitAsTypeAlias() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/UnitAsTypeAlias.kt");
  }

  @TestMetadata("valueClassAsContextParameter.kt")
  public void testValueClassAsContextParameter() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassAsContextParameter.kt");
  }

  @TestMetadata("ValueClassInParametersWithJvmName.kt")
  public void testValueClassInParametersWithJvmName() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/ValueClassInParametersWithJvmName.kt");
  }

  @TestMetadata("valueClassInsideDefaultImpl.kt")
  public void testValueClassInsideDefaultImpl() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassInsideDefaultImpl.kt");
  }

  @TestMetadata("valueClassInsideDelegatedClass.kt")
  public void testValueClassInsideDelegatedClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassInsideDelegatedClass.kt");
  }

  @TestMetadata("valueClassInsideStaticMembers.kt")
  public void testValueClassInsideStaticMembers() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassInsideStaticMembers.kt");
  }

  @TestMetadata("valueClassMembers.kt")
  public void testValueClassMembers() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassMembers.kt");
  }

  @TestMetadata("valueClassWithDelegatedSuperInterface.kt")
  public void testValueClassWithDelegatedSuperInterface() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassWithDelegatedSuperInterface.kt");
  }

  @TestMetadata("valueClassWithInternalParam.kt")
  public void testValueClassWithInternalParam() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassWithInternalParam.kt");
  }

  @TestMetadata("valueClassWithPrivateParam.kt")
  public void testValueClassWithPrivateParam() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassWithPrivateParam.kt");
  }

  @TestMetadata("valueClassWithSuperInterface.kt")
  public void testValueClassWithSuperInterface() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassWithSuperInterface.kt");
  }

  @TestMetadata("valueClassWithValueClass.kt")
  public void testValueClassWithValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/valueClassWithValueClass.kt");
  }

  @TestMetadata("VarArgs.kt")
  public void testVarArgs() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/VarArgs.kt");
  }

  @TestMetadata("varargAndValueClass.kt")
  public void testVarargAndValueClass() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/varargAndValueClass.kt");
  }

  @TestMetadata("wildcardWithoutArguments.kt")
  public void testWildcardWithoutArguments() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/wildcardWithoutArguments.kt");
  }

  @TestMetadata("wildcardWithoutArgumentsOnType.kt")
  public void testWildcardWithoutArgumentsOnType() {
    runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/wildcardWithoutArgumentsOnType.kt");
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class CompilationErrors extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("ActualClass.kt")
    public void testActualClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ActualClass.kt");
    }

    @TestMetadata("ActualTypeAlias.kt")
    public void testActualTypeAlias() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ActualTypeAlias.kt");
    }

    @TestMetadata("ActualTypeAliasCustomJvmPackageName.kt")
    public void testActualTypeAliasCustomJvmPackageName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ActualTypeAliasCustomJvmPackageName.kt");
    }

    public void testAllFilesPresentInCompilationErrors() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("AllInlineOnly.kt")
    public void testAllInlineOnly() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/AllInlineOnly.kt");
    }

    @TestMetadata("AnnotationModifiers.kt")
    public void testAnnotationModifiers() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/AnnotationModifiers.kt");
    }

    @TestMetadata("EnumCompanion.kt")
    public void testEnumCompanion() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/EnumCompanion.kt");
    }

    @TestMetadata("EnumNameOverride.kt")
    public void testEnumNameOverride() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/EnumNameOverride.kt");
    }

    @TestMetadata("ExpectClass.kt")
    public void testExpectClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ExpectClass.kt");
    }

    @TestMetadata("ExpectObject.kt")
    public void testExpectObject() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ExpectObject.kt");
    }

    @TestMetadata("ExpectedNestedClass.kt")
    public void testExpectedNestedClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ExpectedNestedClass.kt");
    }

    @TestMetadata("ExpectedNestedClassInObject.kt")
    public void testExpectedNestedClassInObject() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/ExpectedNestedClassInObject.kt");
    }

    @TestMetadata("FieldWithoutName.kt")
    public void testFieldWithoutName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/FieldWithoutName.kt");
    }

    @TestMetadata("FieldWithoutName2.kt")
    public void testFieldWithoutName2() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/FieldWithoutName2.kt");
    }

    @TestMetadata("FieldWithoutName3.kt")
    public void testFieldWithoutName3() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/FieldWithoutName3.kt");
    }

    @TestMetadata("FunctionWithoutName.kt")
    public void testFunctionWithoutName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/FunctionWithoutName.kt");
    }

    @TestMetadata("JvmPackageName.kt")
    public void testJvmPackageName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/JvmPackageName.kt");
    }

    @TestMetadata("LocalInAnnotation.kt")
    public void testLocalInAnnotation() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/LocalInAnnotation.kt");
    }

    @TestMetadata("MultiplatformCommonFacade.kt")
    public void testMultiplatformCommonFacade() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/MultiplatformCommonFacade.kt");
    }

    @TestMetadata("MultiplatformIntermediateFacade.kt")
    public void testMultiplatformIntermediateFacade() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/MultiplatformIntermediateFacade.kt");
    }

    @TestMetadata("MultiplatformJvmFacade.kt")
    public void testMultiplatformJvmFacade() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/MultiplatformJvmFacade.kt");
    }

    @TestMetadata("PrivateInTrait.kt")
    public void testPrivateInTrait() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/PrivateInTrait.kt");
    }

    @TestMetadata("PropertyWithoutName.kt")
    public void testPropertyWithoutName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/PropertyWithoutName.kt");
    }

    @TestMetadata("RepetableAnnotations.kt")
    public void testRepetableAnnotations() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/RepetableAnnotations.kt");
    }

    @TestMetadata("SameName.kt")
    public void testSameName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/SameName.kt");
    }

    @TestMetadata("TopLevelDestructuring.kt")
    public void testTopLevelDestructuring() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/TopLevelDestructuring.kt");
    }

    @TestMetadata("TraitClassObjectField.kt")
    public void testTraitClassObjectField() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/TraitClassObjectField.kt");
    }

    @TestMetadata("TwoOverrides.kt")
    public void testTwoOverrides() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/TwoOverrides.kt");
    }

    @TestMetadata("typeAliasActualization.kt")
    public void testTypeAliasActualization() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/typeAliasActualization.kt");
    }

    @TestMetadata("typeAliasActualization2.kt")
    public void testTypeAliasActualization2() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/typeAliasActualization2.kt");
    }

    @TestMetadata("UnitAsTypeAliasActualization.kt")
    public void testUnitAsTypeAliasActualization() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/UnitAsTypeAliasActualization.kt");
    }

    @TestMetadata("unresolvedQuialifierInAnnotation.kt")
    public void testUnresolvedQuialifierInAnnotation() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/unresolvedQuialifierInAnnotation.kt");
    }

    @TestMetadata("valueClassInSuperType.kt")
    public void testValueClassInSuperType() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/valueClassInSuperType.kt");
    }

    @TestMetadata("WrongAnnotations.kt")
    public void testWrongAnnotations() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/compilationErrors/WrongAnnotations.kt");
    }
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/delegation")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class Delegation extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInDelegation() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/delegation"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("Function.kt")
    public void testFunction() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/delegation/Function.kt");
    }

    @TestMetadata("Property.kt")
    public void testProperty() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/delegation/Property.kt");
    }

    @TestMetadata("WithImplicitType.kt")
    public void testWithImplicitType() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/delegation/WithImplicitType.kt");
    }
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/facades")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class Facades extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInFacades() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/facades"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("AllPrivate.kt")
    public void testAllPrivate() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/AllPrivate.kt");
    }

    @TestMetadata("contextParameters.kt")
    public void testContextParameters() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/contextParameters.kt");
    }

    @TestMetadata("DelegatedProperty.kt")
    public void testDelegatedProperty() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/DelegatedProperty.kt");
    }

    @TestMetadata("Deprecated.kt")
    public void testDeprecated() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/Deprecated.kt");
    }

    @TestMetadata("FunctionTypeParameterAnnotation.kt")
    public void testFunctionTypeParameterAnnotation() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/FunctionTypeParameterAnnotation.kt");
    }

    @TestMetadata("InternalFacadeClass.kt")
    public void testInternalFacadeClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/InternalFacadeClass.kt");
    }

    @TestMetadata("MultiFile.kt")
    public void testMultiFile() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/MultiFile.kt");
    }

    @TestMetadata("PropertyTypeParameterAnnotation.kt")
    public void testPropertyTypeParameterAnnotation() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/PropertyTypeParameterAnnotation.kt");
    }

    @TestMetadata("SingleFile.kt")
    public void testSingleFile() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/SingleFile.kt");
    }

    @TestMetadata("SingleJvmClassName.kt")
    public void testSingleJvmClassName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/SingleJvmClassName.kt");
    }

    @TestMetadata("valueClassAsContextParameter.kt")
    public void testValueClassAsContextParameter() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/valueClassAsContextParameter.kt");
    }

    @TestMetadata("ValueClassInParametersWithJvmName.kt")
    public void testValueClassInParametersWithJvmName() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/facades/ValueClassInParametersWithJvmName.kt");
    }
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class NullabilityAnnotations extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInNullabilityAnnotations() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("Class.kt")
    public void testClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/Class.kt");
    }

    @TestMetadata("ClassObjectField.kt")
    public void testClassObjectField() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/ClassObjectField.kt");
    }

    @TestMetadata("ClassWithConstructor.kt")
    public void testClassWithConstructor() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/ClassWithConstructor.kt");
    }

    @TestMetadata("ClassWithConstructorAndProperties.kt")
    public void testClassWithConstructorAndProperties() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/ClassWithConstructorAndProperties.kt");
    }

    @TestMetadata("FileFacade.kt")
    public void testFileFacade() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/FileFacade.kt");
    }

    @TestMetadata("Generic.kt")
    public void testGeneric() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/Generic.kt");
    }

    @TestMetadata("ImplicitArrayWithFlexibleParameterTypes.kt")
    public void testImplicitArrayWithFlexibleParameterTypes() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/ImplicitArrayWithFlexibleParameterTypes.kt");
    }

    @TestMetadata("IntOverridesAny.kt")
    public void testIntOverridesAny() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/IntOverridesAny.kt");
    }

    @TestMetadata("JvmOverloads.kt")
    public void testJvmOverloads() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/JvmOverloads.kt");
    }

    @TestMetadata("NullableUnitReturn.kt")
    public void testNullableUnitReturn() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/NullableUnitReturn.kt");
    }

    @TestMetadata("OverrideAnyWithUnit.kt")
    public void testOverrideAnyWithUnit() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/OverrideAnyWithUnit.kt");
    }

    @TestMetadata("PlatformTypes.kt")
    public void testPlatformTypes() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/PlatformTypes.kt");
    }

    @TestMetadata("PrimitiveBackedInlineClasses.kt")
    public void testPrimitiveBackedInlineClasses() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/PrimitiveBackedInlineClasses.kt");
    }

    @TestMetadata("Primitives.kt")
    public void testPrimitives() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/Primitives.kt");
    }

    @TestMetadata("PrivateInClass.kt")
    public void testPrivateInClass() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/PrivateInClass.kt");
    }

    @TestMetadata("Synthetic.kt")
    public void testSynthetic() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/Synthetic.kt");
    }

    @TestMetadata("Trait.kt")
    public void testTrait() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/Trait.kt");
    }

    @TestMetadata("typeAlias.kt")
    public void testTypeAlias() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/typeAlias.kt");
    }

    @TestMetadata("UnitAsGenericArgument.kt")
    public void testUnitAsGenericArgument() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/UnitAsGenericArgument.kt");
    }

    @TestMetadata("UnitParameter.kt")
    public void testUnitParameter() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/UnitParameter.kt");
    }

    @TestMetadata("VoidReturn.kt")
    public void testVoidReturn() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/nullabilityAnnotations/VoidReturn.kt");
    }
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/script")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class Script extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInScript() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/script"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("HelloWorld.kts")
    public void testHelloWorld() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/script/HelloWorld.kts");
    }

    @TestMetadata("InnerClasses.kts")
    public void testInnerClasses() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/script/InnerClasses.kts");
    }
  }

  @TestMetadata("compiler/testData/asJava/lightClasses/lightClassByFqName/withTestCompilerPluginEnabled")
  @TestDataPath("$PROJECT_ROOT")
  @RunWith(JUnit3RunnerWithInners.class)
  public static class WithTestCompilerPluginEnabled extends AbstractCompilerLightClassTest {
    private void runTest(String testDataFilePath) {
      KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInWithTestCompilerPluginEnabled() {
      KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/lightClasses/lightClassByFqName/withTestCompilerPluginEnabled"), Pattern.compile("^([^.]+)\\.(kt|kts)$"), null, true);
    }

    @TestMetadata("allOpen.kt")
    public void testAllOpen() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/withTestCompilerPluginEnabled/allOpen.kt");
    }

    @TestMetadata("companionWithFoo_companionTypeUsedInJava_ReferenceFromKotlin.kt")
    public void testCompanionWithFoo_companionTypeUsedInJava_ReferenceFromKotlin() {
      runTest("compiler/testData/asJava/lightClasses/lightClassByFqName/withTestCompilerPluginEnabled/companionWithFoo_companionTypeUsedInJava_ReferenceFromKotlin.kt");
    }
  }
}
