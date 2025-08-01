/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
public class FirLightTreeDiagnosticsWithoutAliasExpansionTestGenerated extends AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest {
  @Nested
  @TestMetadata("compiler/fir/analysis-tests/testData/resolve")
  @TestDataPath("$PROJECT_ROOT")
  public class Resolve {
    @Test
    @TestMetadata("catchParameter.kt")
    public void testCatchParameter() {
      runTest("compiler/fir/analysis-tests/testData/resolve/catchParameter.kt");
    }

    @Test
    @TestMetadata("delegatingConstructorsAndTypeAliases.kt")
    public void testDelegatingConstructorsAndTypeAliases() {
      runTest("compiler/fir/analysis-tests/testData/resolve/delegatingConstructorsAndTypeAliases.kt");
    }

    @Test
    @TestMetadata("fakeRecursiveTypealias.kt")
    public void testFakeRecursiveTypealias() {
      runTest("compiler/fir/analysis-tests/testData/resolve/fakeRecursiveTypealias.kt");
    }

    @Test
    @TestMetadata("functionTypeAlias.kt")
    public void testFunctionTypeAlias() {
      runTest("compiler/fir/analysis-tests/testData/resolve/functionTypeAlias.kt");
    }

    @Test
    @TestMetadata("NestedOfAliasedType.kt")
    public void testNestedOfAliasedType() {
      runTest("compiler/fir/analysis-tests/testData/resolve/NestedOfAliasedType.kt");
    }

    @Test
    @TestMetadata("recursiveTypeAlias.kt")
    public void testRecursiveTypeAlias() {
      runTest("compiler/fir/analysis-tests/testData/resolve/recursiveTypeAlias.kt");
    }

    @Test
    @TestMetadata("simpleTypeAlias.kt")
    public void testSimpleTypeAlias() {
      runTest("compiler/fir/analysis-tests/testData/resolve/simpleTypeAlias.kt");
    }

    @Test
    @TestMetadata("statusResolveForTypealiasAsSuperClass.kt")
    public void testStatusResolveForTypealiasAsSuperClass() {
      runTest("compiler/fir/analysis-tests/testData/resolve/statusResolveForTypealiasAsSuperClass.kt");
    }

    @Test
    @TestMetadata("typeAliasWithGeneric.kt")
    public void testTypeAliasWithGeneric() {
      runTest("compiler/fir/analysis-tests/testData/resolve/typeAliasWithGeneric.kt");
    }

    @Test
    @TestMetadata("typeAliasWithTypeArguments.kt")
    public void testTypeAliasWithTypeArguments() {
      runTest("compiler/fir/analysis-tests/testData/resolve/typeAliasWithTypeArguments.kt");
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/annotations")
    @TestDataPath("$PROJECT_ROOT")
    public class Annotations {
      @Test
      @TestMetadata("requiredAnnotationWithStarTypealiasedImportedArgument.kt")
      public void testRequiredAnnotationWithStarTypealiasedImportedArgument() {
        runTest("compiler/fir/analysis-tests/testData/resolve/annotations/requiredAnnotationWithStarTypealiasedImportedArgument.kt");
      }

      @Test
      @TestMetadata("requiredAnnotationWithTypealiasedArgument.kt")
      public void testRequiredAnnotationWithTypealiasedArgument() {
        runTest("compiler/fir/analysis-tests/testData/resolve/annotations/requiredAnnotationWithTypealiasedArgument.kt");
      }

      @Test
      @TestMetadata("requiredAnnotationWithTypealiasedArgumentAsMetaAnnotation.kt")
      public void testRequiredAnnotationWithTypealiasedArgumentAsMetaAnnotation() {
        runTest("compiler/fir/analysis-tests/testData/resolve/annotations/requiredAnnotationWithTypealiasedArgumentAsMetaAnnotation.kt");
      }

      @Test
      @TestMetadata("requiredAnnotationWithTypealiasedImportedArgument.kt")
      public void testRequiredAnnotationWithTypealiasedImportedArgument() {
        runTest("compiler/fir/analysis-tests/testData/resolve/annotations/requiredAnnotationWithTypealiasedImportedArgument.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/callResolution")
    @TestDataPath("$PROJECT_ROOT")
    public class CallResolution {
      @Test
      @TestMetadata("safeCallOnTypeAlias.kt")
      public void testSafeCallOnTypeAlias() {
        runTest("compiler/fir/analysis-tests/testData/resolve/callResolution/safeCallOnTypeAlias.kt");
      }

      @Test
      @TestMetadata("typeAliasWithNotNullBound.kt")
      public void testTypeAliasWithNotNullBound() {
        runTest("compiler/fir/analysis-tests/testData/resolve/callResolution/typeAliasWithNotNullBound.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/checkers")
    @TestDataPath("$PROJECT_ROOT")
    public class Checkers {
      @Test
      @TestMetadata("delegationToItself.kt")
      public void testDelegationToItself() {
        runTest("compiler/fir/analysis-tests/testData/resolve/checkers/delegationToItself.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/constVal")
    @TestDataPath("$PROJECT_ROOT")
    public class ConstVal {
      @Test
      @TestMetadata("constValWithTypealiasType.kt")
      public void testConstValWithTypealiasType() {
        runTest("compiler/fir/analysis-tests/testData/resolve/constVal/constValWithTypealiasType.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/constructors")
    @TestDataPath("$PROJECT_ROOT")
    public class Constructors {
      @Test
      @TestMetadata("noSuperCallInSupertypes.kt")
      public void testNoSuperCallInSupertypes() {
        runTest("compiler/fir/analysis-tests/testData/resolve/constructors/noSuperCallInSupertypes.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextParameters")
    @TestDataPath("$PROJECT_ROOT")
    public class ContextParameters {
      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextParameters/declarationAndUsages")
      @TestDataPath("$PROJECT_ROOT")
      public class DeclarationAndUsages {
        @Test
        @TestMetadata("onClassifierDeclaration.kt")
        public void testOnClassifierDeclaration() {
          runTest("compiler/fir/analysis-tests/testData/resolve/contextParameters/declarationAndUsages/onClassifierDeclaration.kt");
        }

        @Test
        @TestMetadata("typealiasToContextFunctionType.kt")
        public void testTypealiasToContextFunctionType() {
          runTest("compiler/fir/analysis-tests/testData/resolve/contextParameters/declarationAndUsages/typealiasToContextFunctionType.kt");
        }
      }

      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextParameters/diagnostics")
      @TestDataPath("$PROJECT_ROOT")
      public class Diagnostics {
        @Test
        @TestMetadata("unsupportedContextParameters.kt")
        public void testUnsupportedContextParameters() {
          runTest("compiler/fir/analysis-tests/testData/resolve/contextParameters/diagnostics/unsupportedContextParameters.kt");
        }
      }

      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextParameters/inference")
      @TestDataPath("$PROJECT_ROOT")
      public class Inference {
      }

      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextParameters/overloads")
      @TestDataPath("$PROJECT_ROOT")
      public class Overloads {
        @Test
        @TestMetadata("OverloadingByTypeAlias.kt")
        public void testOverloadingByTypeAlias() {
          runTest("compiler/fir/analysis-tests/testData/resolve/contextParameters/overloads/OverloadingByTypeAlias.kt");
        }
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/contextSensitiveResolutionUsingExpectedType")
    @TestDataPath("$PROJECT_ROOT")
    public class ContextSensitiveResolutionUsingExpectedType {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/diagnostics")
    @TestDataPath("$PROJECT_ROOT")
    public class Diagnostics {
      @Test
      @TestMetadata("conflictingOverloads.kt")
      public void testConflictingOverloads() {
        runTest("compiler/fir/analysis-tests/testData/resolve/diagnostics/conflictingOverloads.kt");
      }

      @Test
      @TestMetadata("conflictingProjection.kt")
      public void testConflictingProjection() {
        runTest("compiler/fir/analysis-tests/testData/resolve/diagnostics/conflictingProjection.kt");
      }

      @Test
      @TestMetadata("infixFunctions.kt")
      public void testInfixFunctions() {
        runTest("compiler/fir/analysis-tests/testData/resolve/diagnostics/infixFunctions.kt");
      }

      @Test
      @TestMetadata("upperBoundViolated.kt")
      public void testUpperBoundViolated() {
        runTest("compiler/fir/analysis-tests/testData/resolve/diagnostics/upperBoundViolated.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/exhaustiveness")
    @TestDataPath("$PROJECT_ROOT")
    public class Exhaustiveness {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/expresssions")
    @TestDataPath("$PROJECT_ROOT")
    public class Expresssions {
      @Test
      @TestMetadata("typeAliasConstructor.kt")
      public void testTypeAliasConstructor() {
        runTest("compiler/fir/analysis-tests/testData/resolve/expresssions/typeAliasConstructor.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/extraCheckers")
    @TestDataPath("$PROJECT_ROOT")
    public class ExtraCheckers {
      @Test
      @TestMetadata("ArrayEqualityCanBeReplacedWithEquals.kt")
      public void testArrayEqualityCanBeReplacedWithEquals() {
        runTest("compiler/fir/analysis-tests/testData/resolve/extraCheckers/ArrayEqualityCanBeReplacedWithEquals.kt");
      }

      @Test
      @TestMetadata("RedundantSingleExpressionStringTemplateChecker.kt")
      public void testRedundantSingleExpressionStringTemplateChecker() {
        runTest("compiler/fir/analysis-tests/testData/resolve/extraCheckers/RedundantSingleExpressionStringTemplateChecker.kt");
      }

      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolve/extraCheckers/RedundantCallOfConversionMethod")
      @TestDataPath("$PROJECT_ROOT")
      public class RedundantCallOfConversionMethod {
        @Test
        @TestMetadata("int.kt")
        public void testInt() {
          runTest("compiler/fir/analysis-tests/testData/resolve/extraCheckers/RedundantCallOfConversionMethod/int.kt");
        }
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/fromBuilder")
    @TestDataPath("$PROJECT_ROOT")
    public class FromBuilder {
      @Test
      @TestMetadata("typeParameters.kt")
      public void testTypeParameters() {
        runTest("compiler/fir/analysis-tests/testData/resolve/fromBuilder/typeParameters.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/inference")
    @TestDataPath("$PROJECT_ROOT")
    public class Inference {
      @Test
      @TestMetadata("typeDepthForTypeAlias.kt")
      public void testTypeDepthForTypeAlias() {
        runTest("compiler/fir/analysis-tests/testData/resolve/inference/typeDepthForTypeAlias.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/inlineClasses")
    @TestDataPath("$PROJECT_ROOT")
    public class InlineClasses {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/multifile")
    @TestDataPath("$PROJECT_ROOT")
    public class Multifile {
      @Test
      @TestMetadata("Annotations.kt")
      public void testAnnotations() {
        runTest("compiler/fir/analysis-tests/testData/resolve/multifile/Annotations.kt");
      }

      @Test
      @TestMetadata("TypeAliasExpansion.kt")
      public void testTypeAliasExpansion() {
        runTest("compiler/fir/analysis-tests/testData/resolve/multifile/TypeAliasExpansion.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases")
    @TestDataPath("$PROJECT_ROOT")
    public class NestedTypeAliases {
      @Test
      @TestMetadata("annotations.kt")
      public void testAnnotations() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/annotations.kt");
      }

      @Test
      @TestMetadata("companion.kt")
      public void testCompanion() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/companion.kt");
      }

      @Test
      @TestMetadata("declarationNegatives.kt")
      public void testDeclarationNegatives() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/declarationNegatives.kt");
      }

      @Test
      @TestMetadata("deepInnerRHS.kt")
      public void testDeepInnerRHS() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/deepInnerRHS.kt");
      }

      @Test
      @TestMetadata("forFun.kt")
      public void testForFun() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/forFun.kt");
      }

      @Test
      @TestMetadata("innerRHS.kt")
      public void testInnerRHS() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/innerRHS.kt");
      }

      @Test
      @TestMetadata("innerRHSInGeneric.kt")
      public void testInnerRHSInGeneric() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/innerRHSInGeneric.kt");
      }

      @Test
      @TestMetadata("KJInterop.kt")
      public void testKJInterop() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/KJInterop.kt");
      }

      @Test
      @TestMetadata("kmp.kt")
      public void testKmp() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/kmp.kt");
      }

      @Test
      @TestMetadata("localTypeAliases.kt")
      public void testLocalTypeAliases() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/localTypeAliases.kt");
      }

      @Test
      @TestMetadata("overrides.kt")
      public void testOverrides() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/overrides.kt");
      }

      @Test
      @TestMetadata("simple.kt")
      public void testSimple() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/simple.kt");
      }

      @Test
      @TestMetadata("typeAliasExpansionCapturesOuterTypeParametersErrorMessage.kt")
      public void testTypeAliasExpansionCapturesOuterTypeParametersErrorMessage() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/typeAliasExpansionCapturesOuterTypeParametersErrorMessage.kt");
      }

      @Test
      @TestMetadata("visibility.kt")
      public void testVisibility() {
        runTest("compiler/fir/analysis-tests/testData/resolve/nestedTypeAliases/visibility.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/smartcasts")
    @TestDataPath("$PROJECT_ROOT")
    public class Smartcasts {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/stdlib")
    @TestDataPath("$PROJECT_ROOT")
    public class Stdlib {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/suppress")
    @TestDataPath("$PROJECT_ROOT")
    public class Suppress {
      @Test
      @TestMetadata("withSuppression.kt")
      public void testWithSuppression() {
        runTest("compiler/fir/analysis-tests/testData/resolve/suppress/withSuppression.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/types")
    @TestDataPath("$PROJECT_ROOT")
    public class Types {
      @Test
      @TestMetadata("bareWithSubjectTypeAlias.kt")
      public void testBareWithSubjectTypeAlias() {
        runTest("compiler/fir/analysis-tests/testData/resolve/types/bareWithSubjectTypeAlias.kt");
      }

      @Test
      @TestMetadata("typeAliasInArguments.kt")
      public void testTypeAliasInArguments() {
        runTest("compiler/fir/analysis-tests/testData/resolve/types/typeAliasInArguments.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/unqualifiedEnum")
    @TestDataPath("$PROJECT_ROOT")
    public class UnqualifiedEnum {
      @Test
      @TestMetadata("typeAlias.kt")
      public void testTypeAlias() {
        runTest("compiler/fir/analysis-tests/testData/resolve/unqualifiedEnum/typeAlias.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/vfir")
    @TestDataPath("$PROJECT_ROOT")
    public class Vfir {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolve/visibility")
    @TestDataPath("$PROJECT_ROOT")
    public class Visibility {
      @Test
      @TestMetadata("exposedTypeAlias.kt")
      public void testExposedTypeAlias() {
        runTest("compiler/fir/analysis-tests/testData/resolve/visibility/exposedTypeAlias.kt");
      }

      @Test
      @TestMetadata("privateAliasInSamePackage.kt")
      public void testPrivateAliasInSamePackage() {
        runTest("compiler/fir/analysis-tests/testData/resolve/visibility/privateAliasInSamePackage.kt");
      }
    }
  }

  @Nested
  @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib")
  @TestDataPath("$PROJECT_ROOT")
  public class ResolveWithStdlib {
    @Test
    @TestMetadata("ClassIdDiagnosticRendering.kt")
    public void testClassIdDiagnosticRendering() {
      runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/ClassIdDiagnosticRendering.kt");
    }

    @Test
    @TestMetadata("concurrentMapOfAliases.kt")
    public void testConcurrentMapOfAliases() {
      runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/concurrentMapOfAliases.kt");
    }

    @Test
    @TestMetadata("hashMapTypeAlias.kt")
    public void testHashMapTypeAlias() {
      runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/hashMapTypeAlias.kt");
    }

    @Test
    @TestMetadata("typeAliasWithForEach.kt")
    public void testTypeAliasWithForEach() {
      runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/typeAliasWithForEach.kt");
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/callableReferences")
    @TestDataPath("$PROJECT_ROOT")
    public class CallableReferences {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts")
    @TestDataPath("$PROJECT_ROOT")
    public class Contracts {
      @Nested
      @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource")
      @TestDataPath("$PROJECT_ROOT")
      public class FromSource {
        @Nested
        @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/bad")
        @TestDataPath("$PROJECT_ROOT")
        public class Bad {
        }

        @Nested
        @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/good")
        @TestDataPath("$PROJECT_ROOT")
        public class Good {
          @Nested
          @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/contracts/fromSource/good/variousContracts")
          @TestDataPath("$PROJECT_ROOT")
          public class VariousContracts {
          }
        }
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/diagnostics")
    @TestDataPath("$PROJECT_ROOT")
    public class Diagnostics {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/inference")
    @TestDataPath("$PROJECT_ROOT")
    public class Inference {
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/intellij")
    @TestDataPath("$PROJECT_ROOT")
    public class Intellij {
      @Test
      @TestMetadata("typeAliasAmbiguity.kt")
      public void testTypeAliasAmbiguity() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/intellij/typeAliasAmbiguity.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/j+k")
    @TestDataPath("$PROJECT_ROOT")
    public class J_k {
      @Test
      @TestMetadata("flexibleTypeAliases.kt")
      public void testFlexibleTypeAliases() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/j+k/flexibleTypeAliases.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/problems")
    @TestDataPath("$PROJECT_ROOT")
    public class Problems {
      @Test
      @TestMetadata("EnumMapGet.kt")
      public void testEnumMapGet() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/EnumMapGet.kt");
      }

      @Test
      @TestMetadata("exceptionInRepeatedAnnotation.kt")
      public void testExceptionInRepeatedAnnotation() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/exceptionInRepeatedAnnotation.kt");
      }

      @Test
      @TestMetadata("typeAnnotationArgument2.kt")
      public void testTypeAnnotationArgument2() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/typeAnnotationArgument2.kt");
      }

      @Test
      @TestMetadata("WithValidityAssertion.kt")
      public void testWithValidityAssertion() {
        runTest("compiler/fir/analysis-tests/testData/resolveWithStdlib/problems/WithValidityAssertion.kt");
      }
    }

    @Nested
    @TestMetadata("compiler/fir/analysis-tests/testData/resolveWithStdlib/properties")
    @TestDataPath("$PROJECT_ROOT")
    public class Properties {
    }
  }
}
