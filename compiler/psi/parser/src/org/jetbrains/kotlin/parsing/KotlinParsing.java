/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.WhitespacesBinders;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtKeywordToken;
import org.jetbrains.kotlin.lexer.KtSingleValueToken;
import org.jetbrains.kotlin.lexer.KtTokens;

import static org.jetbrains.kotlin.KtNodeTypes.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.parsing.KotlinParsing.AnnotationParsingMode.*;
import static org.jetbrains.kotlin.parsing.KotlinWhitespaceAndCommentsBindersKt.PRECEDING_ALL_BINDER;
import static org.jetbrains.kotlin.parsing.KotlinWhitespaceAndCommentsBindersKt.TRAILING_ALL_BINDER;

public class KotlinParsing extends AbstractKotlinParsing {
    private static final TokenSet GT_COMMA_COLON_SET = TokenSet.create(GT, COMMA, COLON);
    private static final Logger LOG = Logger.getInstance(KotlinParsing.class);

    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            TYPE_ALIAS_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD, OBJECT_KEYWORD,
            FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, PACKAGE_KEYWORD);
    private static final TokenSet TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(SEMICOLON));
    private static final TokenSet LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LT, EQ, SEMICOLON), TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet DECLARATION_FIRST = TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST,
                                                                     TokenSet.create(INIT_KEYWORD, GET_KEYWORD, SET_KEYWORD, CONSTRUCTOR_KEYWORD));

    private static final TokenSet CLASS_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, COLON, LBRACE),
                                                                           TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet TYPE_PARAMETER_GT_RECOVERY_SET = TokenSet.create(WHERE_KEYWORD, LPAR, COLON, LBRACE, GT);
    public static final TokenSet PARAMETER_NAME_RECOVERY_SET = TokenSet.create(COLON, EQ, COMMA, RPAR, VAL_KEYWORD, VAR_KEYWORD);
    private static final TokenSet PACKAGE_NAME_RECOVERY_SET = TokenSet.create(DOT, EOL_OR_SEMICOLON);
    private static final TokenSet IMPORT_RECOVERY_SET = TokenSet.create(AS_KEYWORD, DOT, EOL_OR_SEMICOLON);
    private static final TokenSet TYPE_REF_FIRST = TokenSet.create(LBRACKET, IDENTIFIER, LPAR, HASH, DYNAMIC_KEYWORD);
    private static final TokenSet LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            TokenSet.orSet(TokenSet.create(LBRACE, RBRACE), TYPE_REF_FIRST);
    private static final TokenSet COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            TokenSet.orSet(TokenSet.create(COLON, COMMA, LBRACE, RBRACE), TYPE_REF_FIRST);
    private static final TokenSet RECEIVER_TYPE_TERMINATORS = TokenSet.create(DOT, SAFE_ACCESS);
    private static final TokenSet VALUE_PARAMETER_FIRST =
            TokenSet.orSet(
                    TokenSet.create(IDENTIFIER, LBRACKET, VAL_KEYWORD, VAR_KEYWORD),
                    TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUN_KEYWORD))
            );
    private static final TokenSet LAMBDA_VALUE_PARAMETER_FIRST =
            TokenSet.orSet(
                    TokenSet.create(IDENTIFIER, LBRACKET),
                    TokenSet.andNot(MODIFIER_KEYWORDS, TokenSet.create(FUN_KEYWORD))
            );
    private static final TokenSet SOFT_KEYWORDS_AT_MEMBER_START = TokenSet.create(CONSTRUCTOR_KEYWORD, INIT_KEYWORD);
    private static final TokenSet ANNOTATION_TARGETS = TokenSet.create(
            ALL_KEYWORD, FILE_KEYWORD, FIELD_KEYWORD, GET_KEYWORD, SET_KEYWORD, PROPERTY_KEYWORD,
            RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD, DELEGATE_KEYWORD);
    private static final TokenSet BLOCK_DOC_COMMENT_SET = TokenSet.create(BLOCK_COMMENT, DOC_COMMENT);
    private static final TokenSet SEMICOLON_SET = TokenSet.create(SEMICOLON);
    private static final TokenSet COMMA_COLON_GT_SET = TokenSet.create(COMMA, COLON, GT);
    private static final TokenSet IDENTIFIER_RBRACKET_LBRACKET_SET = TokenSet.create(IDENTIFIER, RBRACKET, LBRACKET);
    private static final TokenSet LBRACE_RBRACE_SET = TokenSet.create(LBRACE, RBRACE);
    private static final TokenSet COMMA_SEMICOLON_RBRACE_SET = TokenSet.create(COMMA, SEMICOLON, RBRACE);
    private static final TokenSet VALUE_ARGS_RECOVERY_SET = TokenSet.create(LBRACE, SEMICOLON, RPAR, EOL_OR_SEMICOLON, RBRACE);
    private static final TokenSet PROPERTY_NAME_FOLLOW_SET =
      TokenSet.create(COLON, EQ, LBRACE, RBRACE, SEMICOLON, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, CLASS_KEYWORD);
    private static final TokenSet DESTRUCTURING_PROPERTY_NAME_FOLLOW_SET = TokenSet.andNot(PROPERTY_NAME_FOLLOW_SET, VAL_VAR);
    private static final TokenSet PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET = TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET = TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet IDENTIFIER_EQ_COLON_SEMICOLON_SET = TokenSet.create(IDENTIFIER, EQ, COLON, SEMICOLON);
    private static final TokenSet COMMA_RPAR_COLON_EQ_SET = TokenSet.create(COMMA, RPAR, COLON, EQ);
    private static final TokenSet ACCESSOR_FIRST_OR_PROPERTY_END =
            TokenSet.orSet(MODIFIER_KEYWORDS, TokenSet.create(AT, GET_KEYWORD, SET_KEYWORD, FIELD_KEYWORD, EOL_OR_SEMICOLON, RBRACE));
    private static final TokenSet RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, IDENTIFIER, COLON, LBRACE, EQ);
    private static final TokenSet COMMA_COLON_RPAR_SET = TokenSet.create(COMMA, COLON, RPAR);
    private static final TokenSet RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(RPAR, COLON, LBRACE, EQ);
    private static final TokenSet LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(LBRACKET, LBRACE, RBRACE, LPAR);
    private static final TokenSet FUNCTION_NAME_FOLLOW_SET = TokenSet.create(LT, LPAR, RPAR, COLON, EQ);
    private static final TokenSet FUNCTION_NAME_RECOVERY_SET = TokenSet.orSet(TokenSet.create(LT, LPAR, RPAR, COLON, EQ), LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST);
    private static final TokenSet VALUE_PARAMETERS_FOLLOW_SET = TokenSet.create(EQ, LBRACE, RBRACE, SEMICOLON, RPAR);
    private static final TokenSet CONTEXT_PARAMETERS_FOLLOW_SET = TokenSet.create(CLASS_KEYWORD, OBJECT_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD);
    private static final TokenSet LPAR_VALUE_PARAMETERS_FOLLOW_SET = TokenSet.orSet(TokenSet.create(LPAR), VALUE_PARAMETERS_FOLLOW_SET);
    private static final TokenSet
            LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET = TokenSet.create(LPAR, LBRACE, COLON, CONSTRUCTOR_KEYWORD);
    private static final TokenSet definitelyOutOfReceiverSet = TokenSet.orSet(
            TokenSet.create(EQ, COLON, LBRACE, RBRACE, BY_KEYWORD),
            TOP_LEVEL_DECLARATION_FIRST
    );
    private final static TokenSet EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(EOL_OR_SEMICOLON, RBRACE);
    private final static TokenSet CLASS_INTERFACE_SET = TokenSet.create(CLASS_KEYWORD, INTERFACE_KEYWORD);

    static KotlinParsing createForTopLevel(SemanticWhitespaceAwarePsiBuilder builder) {
        return new KotlinParsing(builder, true, true);
    }

    static KotlinParsing createForTopLevelNonLazy(SemanticWhitespaceAwarePsiBuilder builder) {
        return new KotlinParsing(builder, true, false);
    }

    private static KotlinParsing createForByClause(SemanticWhitespaceAwarePsiBuilder builder, boolean isLazy) {
        return new KotlinParsing(new SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy);
    }

    private final KotlinExpressionParsing myExpressionParsing;

    private final FirstBefore lastDotAfterReceiverLParPattern = new FirstBefore(
            new AtSet(RECEIVER_TYPE_TERMINATORS),
            new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(boolean topLevel) {
                    if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                        return true;
                    }
                    return topLevel && !at(QUEST) && !at(LPAR) && !at(RPAR);
                }
            }
    );

    private final LastBefore lastDotAfterReceiverNotLParPattern = new LastBefore(
            new AtSet(RECEIVER_TYPE_TERMINATORS),
            new AbstractTokenStreamPredicate() {
                @Override
                public boolean matching(boolean topLevel) {
                    if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(LPAR))) return true;
                    if (topLevel && at(IDENTIFIER)) {
                        IElementType lookahead = lookahead(1);
                        return lookahead != LT && lookahead != DOT && lookahead != SAFE_ACCESS && lookahead != QUEST;
                    }
                    return false;
                }
            });

    private KotlinParsing(SemanticWhitespaceAwarePsiBuilder builder, boolean isTopLevel, boolean isLazy) {
        super(builder, isLazy);
        myExpressionParsing = isTopLevel
                              ? new KotlinExpressionParsing(builder, this, isLazy)
                              : new KotlinExpressionParsing(builder, this, isLazy) {
                                  @Override
                                  protected boolean parseCallWithClosure() {
                                      if (((SemanticWhitespaceAwarePsiBuilderForByClause) builder).getStackSize() > 0) {
                                          return super.parseCallWithClosure();
                                      }
                                      return false;
                                  }

                                  @Override
                                  protected KotlinParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
                                      return createForByClause(builder, isLazy);
                                  }
                              };
    }

    /*
     * [start] kotlinFile
     *   : preamble toplevelObject* [eof]
     *   ;
     */
    void parseFile() {
        PsiBuilder.Marker fileMarker = mark();

        parsePreamble();

        while (!eof()) {
            parseTopLevelDeclaration();
        }

        checkUnclosedBlockComment();
        fileMarker.done(KT_FILE);
    }

    private void checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(myBuilder.rawLookup(-1))) {
            int startOffset = myBuilder.rawTokenTypeStart(-1);
            int endOffset = myBuilder.rawTokenTypeStart(0);
            CharSequence tokenChars = myBuilder.getOriginalText().subSequence(startOffset, endOffset);
            if (!(tokenChars.length() > 2 && tokenChars.subSequence(tokenChars.length() - 2, tokenChars.length()).toString().equals("*/"))) {
                PsiBuilder.Marker marker = myBuilder.mark();
                marker.error("Unclosed comment");
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null);
            }
        }
    }

    void parseTypeCodeFragment() {
        PsiBuilder.Marker marker = mark();
        parseTypeRef();

        checkForUnexpectedSymbols();

        marker.done(TYPE_CODE_FRAGMENT);
    }

    void parseExpressionCodeFragment() {
        PsiBuilder.Marker marker = mark();
        myExpressionParsing.parseExpression();

        checkForUnexpectedSymbols();

        marker.done(EXPRESSION_CODE_FRAGMENT);
    }

    void parseBlockCodeFragment() {
        PsiBuilder.Marker marker = mark();
        PsiBuilder.Marker blockMarker = mark();

        if (at(PACKAGE_KEYWORD) || at(IMPORT_KEYWORD)) {
            PsiBuilder.Marker err = mark();
            parsePreamble();
            err.error("Package directive and imports are forbidden in code fragments");
        }

        myExpressionParsing.parseStatements();

        checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        marker.done(BLOCK_CODE_FRAGMENT);
    }

    void parseLambdaExpression() {
        myExpressionParsing.parseFunctionLiteral(/* preferBlock = */ false, /* collapse = */false);
    }

    void parseBlockExpression() {
        parseBlock(/* collapse = */ false);
    }

    void parseScript() {
        PsiBuilder.Marker fileMarker = mark();

        parsePreamble();

        PsiBuilder.Marker scriptMarker = mark();

        PsiBuilder.Marker blockMarker = mark();

        myExpressionParsing.parseStatements(/*isScriptTopLevel = */true);

        checkForUnexpectedSymbols();

        blockMarker.done(BLOCK);
        blockMarker.setCustomEdgeTokenBinders(PRECEDING_ALL_BINDER, TRAILING_ALL_BINDER);

        scriptMarker.done(SCRIPT);
        scriptMarker.setCustomEdgeTokenBinders(PRECEDING_ALL_BINDER, TRAILING_ALL_BINDER);

        fileMarker.done(KT_FILE);
    }

    private void checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance("Unexpected symbol");
        }
    }

    /*
     *preamble
     *  : fileAnnotationList? packageDirective? import*
     *  ;
     */
    private void parsePreamble() {
        PsiBuilder.Marker firstEntry = mark();

        /*
         * fileAnnotationList
         *   : fileAnnotations*
         */
        parseFileAnnotationList(FILE_ANNOTATIONS_BEFORE_PACKAGE);

        /*
         * packageDirective
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        PsiBuilder.Marker packageDirective = mark();
        parseModifierList(TokenSet.EMPTY);

        if (at(PACKAGE_KEYWORD)) {
            advance(); // PACKAGE_KEYWORD

            parsePackageName();

            firstEntry.drop();

            consumeIf(SEMICOLON);

            packageDirective.done(PACKAGE_DIRECTIVE);
        }
        else {
            // When package directive is omitted we should not report error on non-file annotations at the beginning of the file.
            // So, we rollback the parsing position and reparse file annotation list without report error on non-file annotations.
            firstEntry.rollbackTo();

            parseFileAnnotationList(FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED);
            packageDirective = mark();
            packageDirective.done(PACKAGE_DIRECTIVE);
            // Need to skip everything but shebang comment to allow comments at the start of the file to be bound to the first declaration.
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly.INSTANCE, null);
        }

        parseImportDirectives();
    }

    /* SimpleName{"."} */
    private void parsePackageName() {
        PsiBuilder.Marker qualifiedExpression = mark();
        boolean simpleName = true;
        while (true) {
            if (myBuilder.newlineBeforeCurrentToken()) {
                errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line", PACKAGE_NAME_RECOVERY_SET);
                break;
            }

            if (at(DOT)) {
                advance(); // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list");
                qualifiedExpression = mark();
                continue;
            }

            PsiBuilder.Marker nsName = mark();
            boolean simpleNameFound = expect(IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET);
            if (simpleNameFound) {
                nsName.done(REFERENCE_EXPRESSION);
            }
            else {
                nsName.drop();
            }

            if (!simpleName) {
                PsiBuilder.Marker precedingMarker = qualifiedExpression.precede();
                qualifiedExpression.done(DOT_QUALIFIED_EXPRESSION);
                qualifiedExpression = precedingMarker;
            }

            if (at(DOT)) {
                advance(); // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop();
                    qualifiedExpression = mark();
                }
                else {
                    simpleName = false;
                }
            }
            else {
                break;
            }
        }
        qualifiedExpression.drop();
    }

    /*
     * import
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
     *   ;
     */
    private void parseImportDirective() {
        assert _at(IMPORT_KEYWORD);
        PsiBuilder.Marker importDirective = mark();
        advance(); // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return;
        }

        if (!at(IDENTIFIER)) {
            PsiBuilder.Marker error = mark();
            skipUntil(TokenSet.create(EOL_OR_SEMICOLON));
            error.error("Expecting qualified name");
            importDirective.done(IMPORT_DIRECTIVE);
            consumeIf(SEMICOLON);
            return;
        }

        PsiBuilder.Marker qualifiedName = mark();
        PsiBuilder.Marker reference = mark();
        advance(); // IDENTIFIER
        reference.done(REFERENCE_EXPRESSION);

        while (at(DOT) && lookahead(1) != MUL) {
            advance(); // DOT

            if (closeImportWithErrorIfNewline(importDirective, null, "Import must be placed on a single line")) {
                qualifiedName.drop();
                return;
            }

            reference = mark();
            if (expect(IDENTIFIER, "Qualified name must be a '.'-separated identifier list", IMPORT_RECOVERY_SET)) {
                reference.done(REFERENCE_EXPRESSION);
            }
            else {
                reference.drop();
            }

            PsiBuilder.Marker precede = qualifiedName.precede();
            qualifiedName.done(DOT_QUALIFIED_EXPRESSION);
            qualifiedName = precede;
        }
        qualifiedName.drop();

        if (at(DOT)) {
            advance(); // DOT
            assert _at(MUL);
            advance(); // MUL
            if (at(AS_KEYWORD)) {
                PsiBuilder.Marker as = mark();
                advance(); // AS_KEYWORD
                if (closeImportWithErrorIfNewline(importDirective, null, "Expecting identifier")) {
                    as.drop();
                    return;
                }
                consumeIf(IDENTIFIER);
                as.done(IMPORT_ALIAS);
                as.precede().error("Cannot rename all imported items to one identifier");
            }
        }
        if (at(AS_KEYWORD)) {
            PsiBuilder.Marker alias = mark();
            advance(); // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirective, alias, "Expecting identifier")) {
                return;
            }
            expect(IDENTIFIER, "Expecting identifier", SEMICOLON_SET);
            alias.done(IMPORT_ALIAS);
        }
        consumeIf(SEMICOLON);
        importDirective.done(IMPORT_DIRECTIVE);
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder.INSTANCE);
    }

    private boolean closeImportWithErrorIfNewline(
            PsiBuilder.Marker importDirective,
            @Nullable PsiBuilder.Marker importAlias,
            String errorMessage) {
        if (myBuilder.newlineBeforeCurrentToken()) {
            if (importAlias != null) {
                importAlias.done(IMPORT_ALIAS);
            }
            error(errorMessage);
            importDirective.done(IMPORT_DIRECTIVE);
            return true;
        }
        return false;
    }

    private void parseImportDirectives() {
        PsiBuilder.Marker importList = mark();
        if (!at(IMPORT_KEYWORD)) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything.INSTANCE, null);
        }
        while (at(IMPORT_KEYWORD)) {
            parseImportDirective();
        }
        importList.done(IMPORT_LIST);
    }

    /*
     * toplevelObject
     *   : package
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typeAlias
     *   : object
     *   ;
     */
    private void parseTopLevelDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();

        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, TokenSet.EMPTY);

        IElementType declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.MEMBER_OR_TOPLEVEL);

        if (declType == null && at(LBRACE)) {
            error("Expecting a top level declaration");
            parseBlock();
            declType = FUN;
        }

        if (declType == null && at(IMPORT_KEYWORD)) {
            error("imports are only allowed in the beginning of file");
            parseImportDirectives();
            decl.drop();
        }
        else if (declType == null) {
            errorAndAdvance("Expecting a top level declaration");
            decl.drop();
        }
        else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    public IElementType parseCommonDeclaration(
            @NotNull ModifierDetector detector,
            @NotNull NameParsingMode nameParsingModeForObject,
            @NotNull DeclarationParsingMode declarationParsingMode
    ) {
        switch (getTokenId()) {
            case CLASS_KEYWORD_Id:
            case INTERFACE_KEYWORD_Id:
                return parseClass(detector.isEnumDetected(), true);
            case FUN_KEYWORD_Id:
                return parseFunction();
            case VAL_KEYWORD_Id:
            case VAR_KEYWORD_Id:
                return parseProperty(declarationParsingMode);
            case LPAR_Id:
            case LBRACKET_Id:
                IElementType lookahead = lookahead(1);
                return lookahead == VAL_KEYWORD || lookahead == VAR_KEYWORD ? parseProperty(declarationParsingMode) : null;
            case TYPE_ALIAS_KEYWORD_Id:
                return parseTypeAlias();
            case OBJECT_KEYWORD_Id:
                parseObject(nameParsingModeForObject, true);
                return OBJECT_DECLARATION;
            case IDENTIFIER_Id:
                if (detector.isEnumDetected() && declarationParsingMode.canBeEnumUsedAsSoftKeyword) {
                    return parseClass(true, false);
                }
        }

        return null;
    }

    /*
     * (modifier | annotation)*
     */
    boolean parseModifierList(@NotNull TokenSet noModifiersBefore) {
        return parseModifierList(null, noModifiersBefore);
    }

    void parseAnnotationsList(@NotNull TokenSet noModifiersBefore) {
        doParseModifierList(null, TokenSet.EMPTY, AnnotationParsingMode.DEFAULT, noModifiersBefore);
    }

    /**
     * (modifier | annotation)*
     * <p>
     * Feeds modifiers (not annotations) into the passed consumer, if it is not null
     *
     * @param noModifiersBefore is a token set with elements indicating when met them
     *                          that previous token must be parsed as an identifier rather than modifier
     */
    boolean parseModifierList(@Nullable Consumer<IElementType> tokenConsumer, @NotNull TokenSet noModifiersBefore) {
        return doParseModifierList(tokenConsumer, MODIFIER_KEYWORDS, AnnotationParsingMode.DEFAULT, noModifiersBefore);
    }

    private void parseFunctionTypeValueParameterModifierList() {
        doParseModifierList(null, RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS, NO_ANNOTATIONS_NO_CONTEXT, NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER);
    }

    private void parseTypeModifierList() {
        doParseModifierList(null, TYPE_MODIFIER_KEYWORDS, TYPE_CONTEXT, TokenSet.EMPTY);
    }

    private void parseTypeArgumentModifierList() {
        doParseModifierList(null, TYPE_ARGUMENT_MODIFIER_KEYWORDS, NO_ANNOTATIONS_NO_CONTEXT, COMMA_COLON_GT_SET);
    }

    private boolean doParseModifierListBody(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,
            @NotNull AnnotationParsingMode annotationParsingMode,
            @NotNull TokenSet noModifiersBefore
    ) {
        boolean empty = true;
        PsiBuilder.Marker beforeAnnotationMarker;
        while (!eof()) {
            if (at(AT) && annotationParsingMode.allowAnnotations) {
                beforeAnnotationMarker = mark();

                boolean isAnnotationParsed = parseAnnotationOrList(annotationParsingMode);

                if (!isAnnotationParsed && !annotationParsingMode.withSignificantWhitespaceBeforeArguments) {
                    beforeAnnotationMarker.rollbackTo();
                    // try parse again, but with significant whitespace
                    AnnotationParsingMode newMode = annotationParsingMode.allowContextList
                                                 ? WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS
                                                 : WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT;
                    doParseModifierListBody(tokenConsumer, modifierKeywords, newMode, noModifiersBefore);
                    empty = false;
                    break;
                } else {
                    beforeAnnotationMarker.drop();
                }
            }
            else if (at(CONTEXT_KEYWORD) && annotationParsingMode.allowContextList && lookahead(1) == LPAR) {
                parseContextReceiverList(false);
            }
            else if (tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
                // modifier advanced
            }
            else {
                break;
            }
            empty = false;
        }

        return empty;
    }

    private boolean doParseModifierList(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet modifierKeywords,
            @NotNull AnnotationParsingMode annotationParsingMode,
            @NotNull TokenSet noModifiersBefore
    ) {
        PsiBuilder.Marker list = mark();

        boolean empty = doParseModifierListBody(
                tokenConsumer,
                modifierKeywords,
                annotationParsingMode,
                noModifiersBefore
        );

        if (empty) {
            list.drop();
        }
        else {
            list.done(MODIFIER_LIST);
        }
        return !empty;
    }

    private boolean tryParseModifier(
            @Nullable Consumer<IElementType> tokenConsumer,
            @NotNull TokenSet noModifiersBefore,
            @NotNull TokenSet modifierKeywords
    ) {
        PsiBuilder.Marker marker = mark();

        if (atSet(modifierKeywords)) {
            IElementType lookahead = lookahead(1);

            if (at(FUN_KEYWORD) && lookahead != INTERFACE_KEYWORD) {
                marker.rollbackTo();
                return false;
            }

            if (lookahead != null && !noModifiersBefore.contains(lookahead)) {
                IElementType tt = tt();
                if (tokenConsumer != null) {
                    tokenConsumer.consume(tt);
                }
                advance(); // MODIFIER
                marker.collapse(tt);
                return true;
            }
        }

        marker.rollbackTo();
        return false;
    }

    /*
     * contextReceiverList
     *   : "context" "(" (contextReceiver{","})+ ")"
     */
    private void parseContextReceiverList(boolean inFunctionType) {
        assert _at(CONTEXT_KEYWORD);
        PsiBuilder.Marker contextReceiverList = mark();
        advance(); // CONTEXT_KEYWORD

        assert _at(LPAR);

        if (lookahead(1) == RPAR) {
            advance(); // LPAR
            error("Empty context parameter list");
            advance(); // RPAR
        }
        else {
            valueParameterLoop(inFunctionType, CONTEXT_PARAMETERS_FOLLOW_SET, () -> parseContextReceiver(inFunctionType));
        }

        contextReceiverList.done(CONTEXT_RECEIVER_LIST);
    }

    /*
     * contextReceiver
     *   : label? typeReference
     */
    private void parseContextReceiver(boolean inFunctionType) {
        if (tryParseValueParameter(true)) {
            return;
        }

        PsiBuilder.Marker contextReceiver = mark();
        if (!inFunctionType && myExpressionParsing.isAtLabelDefinitionOrMissingIdentifier()) {
            myExpressionParsing.parseLabelDefinition();
        }
        parseTypeRef();
        contextReceiver.done(CONTEXT_RECEIVER);
    }

    /*
     * fileAnnotationList
     *   : ("[" "file:" annotationEntry+ "]")*
     *   ;
     */
    private void parseFileAnnotationList(AnnotationParsingMode mode) {
        if (!mode.isFileAnnotationParsingMode) {
            LOG.error("expected file annotation parsing mode, but:" + mode);
        }

        PsiBuilder.Marker fileAnnotationsList = mark();

        if (parseAnnotations(mode)) {
            fileAnnotationsList.done(FILE_ANNOTATION_LIST);
        }
        else {
            fileAnnotationsList.drop();
        }
    }

    /*
     * annotations
     *   : (annotation | annotationList)*
     *   ;
     */
    boolean parseAnnotations(AnnotationParsingMode mode) {
        if (!parseAnnotationOrList(mode)) return false;

        while (parseAnnotationOrList(mode)) {
            // do nothing
        }

        return true;
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * annotationList
     *   : "@" (annotationUseSiteTarget ":")? "[" unescapedAnnotation+ "]"
     *   ;
     *
     * annotationUseSiteTarget
     *   : "file"
     *   : "field"
     *   : "property"
     *   : "get"
     *   : "set"
     *   : "param"
     *   : "setparam"
     *   ;
     */
    private boolean parseAnnotationOrList(AnnotationParsingMode mode) {
        if (at(AT)) {
            IElementType nextRawToken = myBuilder.rawLookup(1);
            IElementType tokenToMatch = nextRawToken;
            boolean isTargetedAnnotation = false;

            if ((nextRawToken == IDENTIFIER || ANNOTATION_TARGETS.contains(nextRawToken)) && lookahead(2) == COLON) {
                tokenToMatch = lookahead(3);
                isTargetedAnnotation = true;
            }
            else if (lookahead(1) == COLON) {
                // recovery for "@:ann"
                isTargetedAnnotation = true;
                tokenToMatch = lookahead(2);
            }

            if (tokenToMatch == IDENTIFIER) {
                return parseAnnotation(mode);
            }
            else if (tokenToMatch == LBRACKET) {
                return parseAnnotationList(mode);
            }
            else {
                if (isTargetedAnnotation) {
                    if (lookahead(1) == COLON) {
                        errorAndAdvance("Expected annotation identifier after ':'", 2); // AT, COLON
                    }
                    else {
                        errorAndAdvance("Expected annotation identifier after ':'", 3); // AT, (ANNOTATION TARGET KEYWORD), COLON
                    }
                }
                else {
                    errorAndAdvance("Expected annotation identifier after '@'", 1); // AT
                }
            }
            return true;
        }

        return false;
    }

    private boolean parseAnnotationList(AnnotationParsingMode mode) {
        assert _at(AT);
        PsiBuilder.Marker annotation = mark();

        myBuilder.disableNewlines();

        advance(); // AT

        if (!parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo();
            myBuilder.restoreNewlinesState();
            return false;
        }

        assert _at(LBRACKET);
        advance(); // LBRACKET

        if (!at(IDENTIFIER) && !at(AT)) {
            error("Expecting a list of annotations");
        }
        else {
            while (at(IDENTIFIER) || at(AT)) {
                if (at(AT)) {
                    errorAndAdvance("No '@' needed in annotation list"); // AT
                    continue;
                }

                parseAnnotation(DEFAULT);
                while (at(COMMA)) {
                    errorAndAdvance("No commas needed to separate annotations");
                }
            }
        }

        expect(RBRACKET, "Expecting ']' to close the annotation list");
        myBuilder.restoreNewlinesState();

        annotation.done(ANNOTATION);
        return true;
    }

    // Returns true if we should continue parse annotation
    private boolean parseAnnotationTargetIfNeeded(AnnotationParsingMode mode) {
        String expectedAnnotationTargetBeforeColon = "Expected annotation target before ':'";

        if (at(COLON)) {
            // recovery for "@:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon); // COLON
            return true;
        }

        @Nullable KtKeywordToken targetKeyword = atSet(ANNOTATION_TARGETS) ? (KtKeywordToken)myBuilder.getTokenType() : null;
        if (mode == FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED && !(targetKeyword == FILE_KEYWORD && lookahead(1) == COLON)) {
            return false;
        }

        if (lookahead(1) == COLON && targetKeyword == null && at(IDENTIFIER)) {
            // recovery for "@fil:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon); // IDENTIFIER
            advance(); // COLON
            return true;
        }

        if (targetKeyword == null && mode.isFileAnnotationParsingMode) {
            parseAnnotationTarget(FILE_KEYWORD);
        }
        else if (targetKeyword != null) {
            parseAnnotationTarget(targetKeyword);
        }

        return true;
    }

    private void parseAnnotationTarget(KtKeywordToken keyword) {
        PsiBuilder.Marker marker = mark();

        if (!expect(keyword)) {
            error(generateAnnotationTargetErrorMessage(keyword));
            marker.drop();
        }
        else {
            marker.done(ANNOTATION_TARGET);
        }

        if (!expect(KtTokens.COLON)) {
            errorWithRecovery(generateAnnotationTargetErrorMessage(keyword), IDENTIFIER_RBRACKET_LBRACKET_SET);
        }
    }

    private static String generateAnnotationTargetErrorMessage(KtKeywordToken keyword) {
        return "Expecting \"" + keyword + KtTokens.COLON.getValue() + "\" prefix for " + keyword + " annotations";
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    private boolean parseAnnotation(AnnotationParsingMode mode) {
        assert _at(IDENTIFIER) ||
               // We have "@ann" or "@:ann" or "@ :ann", but not "@ ann"
               // (it's guaranteed that call sites do not allow the latter case)
               (_at(AT) && (!isNextRawTokenCommentOrWhitespace() || lookahead(1) == COLON))
                : "Invalid annotation prefix";

        PsiBuilder.Marker annotation = mark();

        boolean atAt = at(AT);
        if (atAt) {
            advance(); // AT
        }

        if (atAt && !parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo();
            return false;
        }

        PsiBuilder.Marker reference = mark();
        PsiBuilder.Marker typeReference = mark();
        parseUserType();
        typeReference.done(TYPE_REFERENCE);
        reference.done(CONSTRUCTOR_CALLEE);

        parseTypeArgumentList();

        boolean whitespaceAfterAnnotation = WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-1));
        boolean shouldBeParsedNextAsFunctionalType = at(LPAR) && whitespaceAfterAnnotation && mode.withSignificantWhitespaceBeforeArguments;

        if (at(LPAR) && !shouldBeParsedNextAsFunctionalType) {
            myExpressionParsing.parseValueArgumentList();

            /*
             * There are two problem cases relating to parsing of annotations on a functional type:
             *  - Annotation on a functional type was parsed correctly with the capture parentheses of the functional type,
             *      e.g. @Anno () -> Unit
             *                    ^ Parse error only here: Type expected
             *  - It wasn't parsed, e.g. @Anno (x: kotlin.Any) -> Unit
             *                                           ^ Parse error: Expecting ')'
             *
             * In both cases, parser should rollback to start parsing of annotation and tries parse it with significant whitespace.
             * A marker is set here which means that we must to rollback.
             */
            if (mode.typeContext && (getLastToken() != RPAR || at(ARROW))) {
                annotation.done(ANNOTATION_ENTRY);
                return false;
            }
        }
        annotation.done(ANNOTATION_ENTRY);

        return true;
    }

    private boolean isNextRawTokenCommentOrWhitespace() {
        return WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(1));
    }

    public enum NameParsingMode {
        REQUIRED,
        ALLOWED,
        PROHIBITED
    }

    /*
     * class
     *   : modifiers ("class" | "interface") SimpleName
     *       typeParameters?
     *       primaryConstructor?
     *       (":" annotations delegationSpecifier{","})?
     *       typeConstraints
     *       (classBody? | enumClassBody)
     *   ;
     *
     * primaryConstructor
     *   : (modifiers "constructor")? ("(" functionParameter{","} ")")
     *   ;
     *
     * object
     *   : "object" SimpleName? primaryConstructor? ":" delegationSpecifier{","}? classBody?
     *   ;
     */
    private IElementType parseClassOrObject(
            boolean object,
            NameParsingMode nameParsingMode,
            boolean optionalBody,
            boolean enumClass,
            boolean expectKindKeyword
    ) {
        if (expectKindKeyword) {
            if (object) {
                assert _at(OBJECT_KEYWORD);
            }
            else {
                assert _atSet(CLASS_INTERFACE_SET);
            }
            advance(); // CLASS_KEYWORD, INTERFACE_KEYWORD or OBJECT_KEYWORD
        }
        else {
            assert enumClass : "Currently classifiers without class/interface/object are only allowed for enums";
            error("'class' keyword is expected after 'enum'");
        }

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            expect(IDENTIFIER, "Name expected", CLASS_NAME_RECOVERY_SET);
        }
        else {
            assert object : "Must be an object to be nameless";
            if (at(IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
                    errorAndAdvance("An object expression cannot bind a name");
                }
                else {
                    assert nameParsingMode == NameParsingMode.ALLOWED;
                    advance();
                }
            }
        }

        boolean typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        PsiBuilder.Marker beforeConstructorModifiers = mark();
        PsiBuilder.Marker primaryConstructorMarker = mark();
        boolean hasConstructorModifiers = parseModifierList(TokenSet.EMPTY);

        // Some modifiers found, but no parentheses following: class has already ended, and we are looking at something else
        if (hasConstructorModifiers && !atSet(LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET)) {
            beforeConstructorModifiers.rollbackTo();
            return object ? OBJECT_DECLARATION : CLASS;
        }

        // We are still inside a class declaration
        beforeConstructorModifiers.drop();

        boolean hasConstructorKeyword = at(CONSTRUCTOR_KEYWORD);
        if (hasConstructorKeyword) {
            advance(); // CONSTRUCTOR_KEYWORD
        }

        if (at(LPAR)) {
            parseValueParameterList(false, /* typeRequired  = */ true, LBRACE_RBRACE_SET);
            primaryConstructorMarker.done(PRIMARY_CONSTRUCTOR);
        }
        else if (hasConstructorModifiers || hasConstructorKeyword) {
            // A comprehensive error message for cases like:
            //    class A private : Foo
            // or
            //    class A private {
            primaryConstructorMarker.done(PRIMARY_CONSTRUCTOR);
            if (hasConstructorKeyword) {
                error("Expecting primary constructor parameter list");
            }
            else {
                error("Expecting 'constructor' keyword");
            }
        }
        else {
            primaryConstructorMarker.drop();
        }

        if (at(COLON)) {
            advance(); // COLON
            parseDelegationSpecifierList();
        }

        OptionalMarker whereMarker = new OptionalMarker(object);
        parseTypeConstraintsGuarded(typeParametersDeclared);
        whereMarker.error("Where clause is not allowed for objects");

        if (at(LBRACE)) {
            if (enumClass) {
                parseEnumClassBody();
            }
            else {
                parseClassBody();
            }
        }
        else if (!optionalBody) {
            PsiBuilder.Marker fakeBody = mark();
            error("Expecting a class body");
            fakeBody.done(CLASS_BODY);
        }

        return object ? OBJECT_DECLARATION : CLASS;
    }

    private IElementType parseClass(boolean enumClass, boolean expectKindKeyword) {
        return parseClassOrObject(false, NameParsingMode.REQUIRED, true, enumClass, expectKindKeyword);
    }

    void parseObject(NameParsingMode nameParsingMode, boolean optionalBody) {
        parseClassOrObject(true, nameParsingMode, optionalBody, false, true);
    }

    /*
     * enumClassBody
     *   : "{" enumEntries (";" members)? "}"
     *   ;
     */
    private void parseEnumClassBody() {
        if (!at(LBRACE)) return;

        PsiBuilder.Marker body = mark();
        myBuilder.enableNewlines();

        advance(); // LBRACE

        if (!parseEnumEntries() && !at(RBRACE)) {
            error("Expecting ';' after the last enum entry or '}' to close enum class body");
        }
        parseMembers();
        expect(RBRACE, "Expecting '}' to close enum class body");

        myBuilder.restoreNewlinesState();
        body.done(CLASS_BODY);
    }

    /**
     * enumEntries
     *   : enumEntry{","}?
     *   ;
     *
     * @return true if enum regular members can follow, false otherwise
     */
    private boolean parseEnumEntries() {
        while (!eof() && !at(RBRACE)) {
            switch (parseEnumEntry()) {
                case FAILED:
                    // Special case without any enum entries but with possible members after semicolon
                    if (at(SEMICOLON)) {
                        advance();
                        return true;
                    }
                    else {
                        return false;
                    }
                case NO_DELIMITER:
                    return false;
                case COMMA_DELIMITER:
                    break;
                case SEMICOLON_DELIMITER:
                    return true;
            }
        }
        return false;
    }

    private enum ParseEnumEntryResult {
        FAILED,
        NO_DELIMITER,
        COMMA_DELIMITER,
        SEMICOLON_DELIMITER
    }

    /*
     * enumEntry
     *   : modifiers SimpleName ("(" arguments ")")? classBody?
     *   ;
     */
    private ParseEnumEntryResult parseEnumEntry() {
        PsiBuilder.Marker entry = mark();

        parseModifierList(COMMA_SEMICOLON_RBRACE_SET);

        if (!atSet(SOFT_KEYWORDS_AT_MEMBER_START) && at(IDENTIFIER)) {
            advance(); // IDENTIFIER

            if (at(LPAR)) {
                // Arguments should be parsed here
                // Also, "fake" constructor call tree is created,
                // with empty type name inside
                PsiBuilder.Marker initializerList = mark();
                PsiBuilder.Marker delegatorSuperCall = mark();

                PsiBuilder.Marker callee = mark();
                PsiBuilder.Marker typeReference = mark();
                PsiBuilder.Marker type = mark();
                PsiBuilder.Marker referenceExpr = mark();
                referenceExpr.done(ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION);
                type.done(USER_TYPE);
                typeReference.done(TYPE_REFERENCE);
                callee.done(CONSTRUCTOR_CALLEE);

                myExpressionParsing.parseValueArgumentList();
                delegatorSuperCall.done(SUPER_TYPE_CALL_ENTRY);
                initializerList.done(INITIALIZER_LIST);
            }
            if (at(LBRACE)) {
                parseClassBody();
            }
            boolean commaFound = at(COMMA);
            if (commaFound) {
                advance();
            }
            boolean semicolonFound = at(SEMICOLON);
            if (semicolonFound) {
                advance();
            }

            // Probably some helper function
            closeDeclarationWithCommentBinders(entry, ENUM_ENTRY, true);
            return semicolonFound
                   ? ParseEnumEntryResult.SEMICOLON_DELIMITER
                   : (commaFound ? ParseEnumEntryResult.COMMA_DELIMITER : ParseEnumEntryResult.NO_DELIMITER);
        }
        else {
            entry.rollbackTo();
            return ParseEnumEntryResult.FAILED;
        }
    }

    /*
     * classBody
     *   : ("{" members "}")?
     *   ;
     */
    private void parseClassBody() {
        PsiBuilder.Marker body = mark();

        myBuilder.enableNewlines();

        if (expect(LBRACE, "Expecting a class body")) {
            parseMembers();
            expect(RBRACE, "Missing '}");
        }

        myBuilder.restoreNewlinesState();

        body.done(CLASS_BODY);
    }

    /**
     * members
     *   : memberDeclaration*
     *   ;
     */
    private void parseMembers() {
        while (!eof() && !at(RBRACE)) {
            parseMemberDeclaration();
        }
    }

    /*
     * memberDeclaration
     *   : modifiers memberDeclaration'
     *   ;
     *
     * memberDeclaration'
     *   : companionObject
     *   : secondaryConstructor
     *   : function
     *   : property
     *   : class
     *   : extension
     *   : typeAlias
     *   : anonymousInitializer
     *   : object
     *   ;
     */
    private void parseMemberDeclaration() {
        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
            return;
        }
        PsiBuilder.Marker decl = mark();

        ModifierDetector detector = new ModifierDetector();
        parseModifierList(detector, TokenSet.EMPTY);

        IElementType declType = parseMemberDeclarationRest(detector);

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY);
            decl.drop();
        }
        else {
            closeDeclarationWithCommentBinders(decl, declType, true);
        }
    }

    private IElementType parseMemberDeclarationRest(@NotNull ModifierDetector modifierDetector) {
        IElementType declType = parseCommonDeclaration(
                modifierDetector,
                modifierDetector.isCompanionDetected() ? NameParsingMode.ALLOWED : NameParsingMode.REQUIRED,
                DeclarationParsingMode.MEMBER_OR_TOPLEVEL
        );

        if (declType != null) return declType;

        if (at(INIT_KEYWORD)) {
            advance(); // init
            if (at(LBRACE)) {
                parseBlock();
            }
            else {
                mark().error("Expecting '{' after 'init'");
            }
            declType = CLASS_INITIALIZER;
        }
        else if (at(CONSTRUCTOR_KEYWORD)) {
            parseSecondaryConstructor();
            declType = SECONDARY_CONSTRUCTOR;
        }
        else if (at(LBRACE)) {
            error("Expecting member declaration");
            parseBlock();
            declType = FUN;
        }
        return declType;
    }

    /*
     * secondaryConstructor
     *   : modifiers "constructor" valueParameters (":" constructorDelegationCall)? block
     * constructorDelegationCall
     *   : "this" valueArguments
     *   : "super" valueArguments
     */
    private void parseSecondaryConstructor() {
        assert _at(CONSTRUCTOR_KEYWORD);

        advance(); // CONSTRUCTOR_KEYWORD

        if (at(LPAR)) {
            parseValueParameterList(false, /*typeRequired = */ true, VALUE_ARGS_RECOVERY_SET);
        }
        else {
            errorWithRecovery("Expecting '('", TokenSet.orSet(VALUE_ARGS_RECOVERY_SET, TokenSet.create(COLON)));
        }

        if (at(COLON)) {
            advance(); // COLON

            PsiBuilder.Marker delegationCall = mark();

            if (at(THIS_KEYWORD) || at(SUPER_KEYWORD)) {
                parseThisOrSuper();
                myExpressionParsing.parseValueArgumentList();
            }
            else {
                error("Expecting a 'this' or 'super' constructor call");
                PsiBuilder.Marker beforeWrongDelegationCallee = null;
                if (!at(LPAR)) {
                    beforeWrongDelegationCallee = mark();
                    advance(); // wrong delegation callee
                }
                myExpressionParsing.parseValueArgumentList();

                if (beforeWrongDelegationCallee != null) {
                    if (at(LBRACE)) {
                        beforeWrongDelegationCallee.drop();
                    }
                    else {
                        beforeWrongDelegationCallee.rollbackTo();
                    }
                }
            }

            delegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }
        else {
            // empty constructor delegation call
            PsiBuilder.Marker emptyDelegationCall = mark();
            mark().done(CONSTRUCTOR_DELEGATION_REFERENCE);
            emptyDelegationCall.done(CONSTRUCTOR_DELEGATION_CALL);
        }

        if (at(LBRACE)) {
            parseBlock();
        }
    }

    private void parseThisOrSuper() {
        assert _at(THIS_KEYWORD) || _at(SUPER_KEYWORD);
        PsiBuilder.Marker mark = mark();

        advance(); // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(CONSTRUCTOR_DELEGATION_REFERENCE);
    }

    /*
     * typeAlias
     *   : modifiers "typealias" SimpleName typeParameters? "=" type
     *   ;
     */
    private IElementType parseTypeAlias() {
        assert _at(TYPE_ALIAS_KEYWORD);

        advance(); // TYPE_ALIAS_KEYWORD

        expect(IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET);

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET);

        if (at(WHERE_KEYWORD)) {
            PsiBuilder.Marker error = mark();
            parseTypeConstraints();
            error.error("Type alias parameters can't have bounds");
        }

        expect(EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET);

        parseTypeRef();

        consumeIf(SEMICOLON);

        return TYPEALIAS;
    }

    enum DeclarationParsingMode {
        MEMBER_OR_TOPLEVEL(false, true, true),
        LOCAL(true, false, false),
        SCRIPT_TOPLEVEL(true, true, false);

        public final boolean destructuringAllowed;
        public final boolean accessorsAllowed;
        public final boolean canBeEnumUsedAsSoftKeyword;

        DeclarationParsingMode(boolean destructuringAllowed, boolean accessorsAllowed, boolean canBeEnumUsedAsSoftKeyword) {
            this.destructuringAllowed = destructuringAllowed;
            this.accessorsAllowed = accessorsAllowed;
            this.canBeEnumUsedAsSoftKeyword = canBeEnumUsedAsSoftKeyword;
        }
    }

    /*
     * variableDeclarationEntry
     *   : SimpleName (":" type)?
     *   ;
     *
     * property
     *   : modifiers ("val" | "var")
     *       typeParameters?
     *       (type ".")?
     *       ("(" variableDeclarationEntry{","} ")" | variableDeclarationEntry)
     *       typeConstraints
     *       ("by" | "=" expression SEMI?)?
     *       (getter? setter? | setter? getter?) SEMI?
     *   ;
     */
    public IElementType parseProperty(DeclarationParsingMode mode) {
        boolean isShortForm = at(VAL_KEYWORD) || at(VAR_KEYWORD);
        if (isShortForm) {
            advance();
        }

        boolean typeParametersDeclared = at(LT) && parseTypeParameterList(IDENTIFIER_EQ_COLON_SEMICOLON_SET);

        myBuilder.disableJoiningComplexTokens();

        PsiBuilder.Marker receiver = mark();
        boolean receiverTypeDeclared = parseReceiverType("property", PROPERTY_NAME_FOLLOW_SET);

        boolean multiDeclaration = at(LPAR) || at(LBRACKET);

        errorIf(receiver, multiDeclaration && receiverTypeDeclared, "Receiver type is not allowed on a destructuring declaration");

        boolean isNameOnTheNextLine = eol();
        PsiBuilder.Marker beforeName = mark();

        if (multiDeclaration) {
            PsiBuilder.Marker multiDecl = mark();
            parseMultiDeclarationEntry(
                    isShortForm ? PROPERTY_NAME_FOLLOW_SET : DESTRUCTURING_PROPERTY_NAME_FOLLOW_SET,
                    PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET,
                    isShortForm ? MultiDeclarationMode.SHORT : MultiDeclarationMode.FULL);
            errorIf(multiDecl, !mode.destructuringAllowed, "Destructuring declarations are only allowed for local variables/values");
        }
        else {
            parseFunctionOrPropertyName(receiverTypeDeclared, "property", PROPERTY_NAME_FOLLOW_SET, PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET, /*nameRequired = */ true);
        }

        myBuilder.restoreJoiningComplexTokensState();

        boolean noTypeReference = true;
        if (at(COLON)) {
            noTypeReference = false;
            PsiBuilder.Marker type = mark();
            advance(); // COLON
            parseTypeRef();
            errorIf(type, multiDeclaration, "Type annotations are not allowed on destructuring declarations");
        }

        parseTypeConstraintsGuarded(typeParametersDeclared);

        if (!parsePropertyDelegateOrAssignment() && isNameOnTheNextLine && noTypeReference && !receiverTypeDeclared) {
            // Do not parse property identifier on the next line if declaration is invalid
            // In most cases this identifier relates to next statement/declaration
            beforeName.rollbackTo();
            error("Expecting property name or receiver type");
            return PROPERTY;
        }

        beforeName.drop();

        if (mode.accessorsAllowed) {
            // It's only needed for non-local properties, because in local ones:
            // "val a = 1; b" must not be an infix call of b on "val ...;"

            myBuilder.enableNewlines();
            boolean hasNewLineWithSemicolon = consumeIf(SEMICOLON) && myBuilder.newlineBeforeCurrentToken();
            myBuilder.restoreNewlinesState();

            if (!hasNewLineWithSemicolon) {
                PropertyComponentKind.Collector alreadyRead = new PropertyComponentKind.Collector();
                PropertyComponentKind propertyComponentKind = parsePropertyComponent(alreadyRead);

                while (propertyComponentKind != null) {
                    alreadyRead.collect(propertyComponentKind);
                    propertyComponentKind = parsePropertyComponent(alreadyRead);
                }

                if (!atSet(EOL_OR_SEMICOLON_RBRACE_SET)) {
                    if (getLastToken() != SEMICOLON) {
                        errorUntil(
                                "Property getter or setter expected",
                                TokenSet.orSet(DECLARATION_FIRST, TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE)));
                    }
                }
                else {
                    consumeIf(SEMICOLON);
                }
            }
        }

        return multiDeclaration ? DESTRUCTURING_DECLARATION : PROPERTY;
    }

    private boolean parsePropertyDelegateOrAssignment() {
        if (at(BY_KEYWORD)) {
            parsePropertyDelegate();
            return true;
        }
        else if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
            return true;
        }

        return false;
    }

    /*
     * propertyDelegate
     *   : "by" expression
     *   ;
     */
    private void parsePropertyDelegate() {
        assert _at(BY_KEYWORD);
        PsiBuilder.Marker delegate = mark();
        advance(); // BY_KEYWORD
        myExpressionParsing.parseExpression();
        delegate.done(PROPERTY_DELEGATE);
    }

    public enum MultiDeclarationMode {
        SHORT,
        FULL,
        FULL_VAL_ONLY,
    }

    /*
     * (SimpleName (":" type){","})
     */
    public void parseMultiDeclarationEntry(TokenSet follow, TokenSet recoverySet, MultiDeclarationMode mode) {
        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        //   (val a: X = aa, var b) = foo()
        //   val [a, b] = foo()
        //   [val a: X, var b] = foo()
        myBuilder.disableNewlines();

        boolean isParentheses = at(LPAR);
        KtSingleValueToken closingBrace = isParentheses ? RPAR : RBRACKET;

        advance(); // LPAR | LBRACKET

        if (!atSet(follow)) {
            while (true) {
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a name");
                }
                else if (at(closingBrace)) { // For declaration similar to `val () = somethingCall()`
                    error("Expecting a name");
                    break;
                }
                PsiBuilder.Marker property = mark();

                if (mode == MultiDeclarationMode.FULL) {
                    if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
                        advance();
                    } else {
                        errorWithRecovery("Expecting val or var keyword", recoverySet);
                    }
                } else if (mode == MultiDeclarationMode.FULL_VAL_ONLY) {
                    if (at(VAL_KEYWORD)) {
                        advance();
                    } else {
                        errorWithRecovery("Expecting val keyword", recoverySet);
                    }
                } else {
                    parseModifierList(COMMA_RPAR_COLON_EQ_SET);
                }

                expect(IDENTIFIER, "Expecting a name", recoverySet);

                if (at(COLON)) {
                    advance(); // COLON
                    parseTypeRef(follow);
                }

                // Renaming is only allowed in name-based destructuring
                if (at(EQ) && closingBrace == RPAR) {
                    advance();
                    myExpressionParsing.parseSimpleNameExpression();
                }

                property.done(DESTRUCTURING_DECLARATION_ENTRY);

                if (!at(COMMA)) break;
                advance(); // COMMA
                if (at(closingBrace)) break;
            }
        }

        expect(closingBrace, isParentheses ? "Expecting ')'" : "Expecting ']'", follow);
        myBuilder.restoreNewlinesState();
    }

    private enum PropertyComponentKind {
        GET,
        SET,
        FIELD;

        static class Collector {
            private final boolean[] collected = { false, false, false };

            public void collect(PropertyComponentKind kind) {
                collected[kind.ordinal()] = true;
            }

            public boolean contains(PropertyComponentKind kind) {
                return collected[kind.ordinal()];
            }
        }
    }

    /*
     * propertyComponent
     *   : modifiers ("get" | "set")
     *   :
     *        (     "get" "(" ")"
     *           |
     *              "set" "(" modifiers parameter ")"
     *           |
     *              "field"
     *        ) functionBody
     *   ;
     */
    @Nullable
    private PropertyComponentKind parsePropertyComponent(PropertyComponentKind.Collector notAllowedKind) {
        PsiBuilder.Marker propertyComponent = mark();

        parseModifierList(TokenSet.EMPTY);

        PropertyComponentKind propertyComponentKind;
        if (at(GET_KEYWORD)) {
            propertyComponentKind = PropertyComponentKind.GET;
        }
        else if (at(SET_KEYWORD)) {
            propertyComponentKind = PropertyComponentKind.SET;
        }
        else if (at(FIELD_KEYWORD)) {
            propertyComponentKind = PropertyComponentKind.FIELD;
        }
        else {
            propertyComponent.rollbackTo();
            return null;
        }

        if (notAllowedKind.contains(propertyComponentKind)) {
            propertyComponent.rollbackTo();
            return null;
        }

        advance(); // GET_KEYWORD, SET_KEYWORD or FIELD_KEYWORD

        if (!at(LPAR) && propertyComponentKind != PropertyComponentKind.FIELD) {
            // Account for Jet-114 (val a : int get {...})
            if (!atSet(ACCESSOR_FIRST_OR_PROPERTY_END)) {
                errorUntil("Accessor body expected", TokenSet.orSet(ACCESSOR_FIRST_OR_PROPERTY_END, TokenSet.create(LBRACE, LPAR, EQ)));
            }
            else {
                closeDeclarationWithCommentBinders(propertyComponent, PROPERTY_ACCESSOR, true);
                return propertyComponentKind;
            }
        }

        myBuilder.disableNewlines();

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            PsiBuilder.Marker parameterList = mark();
            expect(LPAR, "Expecting '('", RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET);
            if (propertyComponentKind == PropertyComponentKind.SET) {
                PsiBuilder.Marker setterParameter = mark();
                parseModifierList(COMMA_COLON_RPAR_SET);
                expect(IDENTIFIER, "Expecting parameter name", RPAR_COLON_LBRACE_EQ_SET);

                if (at(COLON)) {
                    advance(); // COLON
                    parseTypeRef();
                }
                setterParameter.done(VALUE_PARAMETER);
                if (at(COMMA)) {
                    advance(); // COMMA
                }
            }
            if (!at(RPAR)) {
                errorUntil("Expecting ')'", TokenSet.create(RPAR, COLON, LBRACE, RBRACE, EQ, EOL_OR_SEMICOLON));
            }
            if (at(RPAR)) {
                advance();
            }
            parameterList.done(VALUE_PARAMETER_LIST);
        }
        myBuilder.restoreNewlinesState();

        if (at(COLON)) {
            advance();

            parseTypeRef();
        }

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            parseFunctionContract();
            parseFunctionBody();
        } else if (at(EQ)) {
            advance();
            myExpressionParsing.parseExpression();
            consumeIf(SEMICOLON);
        }

        if (propertyComponentKind == PropertyComponentKind.FIELD) {
            closeDeclarationWithCommentBinders(propertyComponent, BACKING_FIELD, true);
        } else {
            closeDeclarationWithCommentBinders(propertyComponent, PROPERTY_ACCESSOR, true);
        }

        return propertyComponentKind;
    }

    @NotNull
    private IElementType parseFunction() {
        return parseFunction(false);
    }

    /*
     * function
     *   : modifiers "fun" typeParameters?
     *       (type ".")?
     *       SimpleName
     *       typeParameters? functionParameters (":" type)?
     *       typeConstraints
     *       functionBody?
     *   ;
     */
    @Contract("false -> !null")
    IElementType parseFunction(boolean failIfIdentifierExists) {
        assert _at(FUN_KEYWORD);

        advance(); // FUN_KEYWORD

        // Recovery for the case of class A { fun| }
        if (at(RBRACE)) {
            error("Function body expected");
            return FUN;
        }

        boolean typeParameterListOccurred = false;
        if (at(LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET);
            typeParameterListOccurred = true;
        }

        myBuilder.disableJoiningComplexTokens();

        boolean receiverFound = parseReceiverType("function", FUNCTION_NAME_FOLLOW_SET);

        if (at(IDENTIFIER) && failIfIdentifierExists) {
            myBuilder.restoreJoiningComplexTokensState();
            return null;
        }

        // function as expression has no name
        parseFunctionOrPropertyName(receiverFound, "function", FUNCTION_NAME_FOLLOW_SET, FUNCTION_NAME_RECOVERY_SET, /*nameRequired = */ false);

        myBuilder.restoreJoiningComplexTokensState();

        if (at(LT)) {
            PsiBuilder.Marker error = mark();
            parseTypeParameterList(LPAR_VALUE_PARAMETERS_FOLLOW_SET);
            if (typeParameterListOccurred) {
                int finishIndex = myBuilder.rawTokenIndex();
                error.rollbackTo();
                error = mark();
                advance(finishIndex - myBuilder.rawTokenIndex());
                error.error("Only one type parameter list is allowed for a function");
            }
            else {
                error.drop();
            }
            typeParameterListOccurred = true;
        }

        if (at(LPAR)) {
            parseValueParameterList(false, /* typeRequired  = */ false, VALUE_PARAMETERS_FOLLOW_SET);
        }
        else {
            error("Expecting '('");
        }

        if (at(COLON)) {
            advance(); // COLON

            parseTypeRef();
        }

        boolean functionContractOccurred = parseFunctionContract();

        parseTypeConstraintsGuarded(typeParameterListOccurred);

        if (!functionContractOccurred) {
            parseFunctionContract();
        }

        if (at(SEMICOLON)) {
            advance(); // SEMICOLON
        }
        else if (at(EQ) || at(LBRACE)) {
            parseFunctionBody();
        }

        return FUN;
    }

    /*
     *   (type "." | annotations)?
     */
    private boolean parseReceiverType(String title, TokenSet nameFollow) {
        PsiBuilder.Marker annotations = mark();
        boolean annotationsPresent = parseAnnotations(DEFAULT);
        int lastDot = lastDotAfterReceiver();
        boolean receiverPresent = lastDot != -1;
        if (annotationsPresent) {
            if (receiverPresent) {
                annotations.rollbackTo();
            }
            else {
                annotations.error("Annotations are not allowed in this position");
            }
        }
        else {
            annotations.drop();
        }

        if (!receiverPresent) return false;

        createTruncatedBuilder(lastDot).parseTypeRefWithoutIntersections();

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
            advance(); // expectation
        }
        else {
            errorWithRecovery("Expecting '.' before a " + title + " name", nameFollow);
        }
        return true;
    }

    private int lastDotAfterReceiver() {
        AbstractTokenStreamPattern pattern = at(LPAR) ? lastDotAfterReceiverLParPattern : lastDotAfterReceiverNotLParPattern;
        pattern.reset();
        return matchTokenStreamPredicate(pattern);
    }

    /*
     * IDENTIFIER
     */
    private void parseFunctionOrPropertyName(boolean receiverFound, String title, TokenSet nameFollow, TokenSet recoverySet, boolean nameRequired) {
        if (!nameRequired && atSet(nameFollow)) return; // no name

        if (expect(IDENTIFIER)) {
            return;
        }

        errorWithRecovery(
                "Expecting " + title + " name" + (!receiverFound ? " or receiver type" : ""),
                recoverySet
        );
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    private void parseFunctionBody() {
        if (at(LBRACE)) {
            parseBlock();
        }
        else if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
            consumeIf(SEMICOLON);
        }
        else {
            error("Expecting function body");
        }
    }

    /*
     * block
     *   : "{" (expressions)* "}"
     *   ;
     */
    void parseBlock() {
        parseBlock(/*collapse*/ true);
    }

    private void parseBlock(boolean collapse) {
        PsiBuilder.Marker lazyBlock = mark();

        myBuilder.enableNewlines();

        boolean hasOpeningBrace = expect(LBRACE, "Expecting '{' to open a block");
        boolean canCollapse = collapse && hasOpeningBrace && isLazy;

        if (canCollapse) {
            advanceBalancedBlock();
        }
        else {
            myExpressionParsing.parseStatements();
            expect(RBRACE, "Expecting '}'");
        }

        myBuilder.restoreNewlinesState();

        if (canCollapse) {
            lazyBlock.collapse(BLOCK);
        }
        else {
            lazyBlock.done(BLOCK);
        }
    }

    public void advanceBalancedBlock() {

        int braceCount = 1;
        while (!eof()) {
            if (_at(LBRACE)) {
                braceCount++;
            }
            else if (_at(RBRACE)) {
                braceCount--;
            }

            advance();

            if (braceCount == 0) {
                break;
            }
        }
    }

    /*
     * delegationSpecifier{","}
     */
    private void parseDelegationSpecifierList() {
        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) {
                errorAndAdvance("Expecting a delegation specifier");
                continue;
            }
            parseDelegationSpecifier();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(SUPER_TYPE_LIST);
    }

    /*
     * delegationSpecifier
     *   : constructorInvocation // type and constructor arguments
     *   : userType
     *   : explicitDelegation
     *   ;
     *
     * explicitDelegation
     *   : userType "by" element
     *   ;
     */
    private void parseDelegationSpecifier() {
        PsiBuilder.Marker delegator = mark();
        PsiBuilder.Marker reference = mark();
        parseTypeRef();

        if (at(BY_KEYWORD)) {
            reference.drop();
            advance(); // BY_KEYWORD
            createForByClause(myBuilder, isLazy).myExpressionParsing.parseExpression();
            delegator.done(DELEGATED_SUPER_TYPE_ENTRY);
        }
        else if (at(LPAR)) {
            reference.done(CONSTRUCTOR_CALLEE);
            myExpressionParsing.parseValueArgumentList();
            delegator.done(SUPER_TYPE_CALL_ENTRY);
        }
        else {
            reference.drop();
            delegator.done(SUPER_TYPE_ENTRY);
        }
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private boolean parseTypeParameterList(TokenSet recoverySet) {
        boolean result = false;
        if (at(LT)) {
            PsiBuilder.Marker list = mark();

            myBuilder.disableNewlines();
            advance(); // LT

            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting type parameter declaration");
                parseTypeParameter();

                if (!at(COMMA)) break;
                advance(); // COMMA
                if (at(GT)) {
                    break;
                }
            }

            expect(GT, "Missing '>'", recoverySet);
            myBuilder.restoreNewlinesState();
            result = true;

            list.done(TYPE_PARAMETER_LIST);
        }
        return result;
    }

    /*
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    private void parseTypeConstraintsGuarded(boolean typeParameterListOccurred) {
        PsiBuilder.Marker error = mark();
        boolean constraints = parseTypeConstraints();
        errorIf(error, constraints && !typeParameterListOccurred, "Type constraints are not allowed when no type parameters declared");
    }

    private boolean parseTypeConstraints() {
        if (at(WHERE_KEYWORD)) {
            parseTypeConstraintList();
            return true;
        }
        return false;
    }

    /*
     * typeConstraint{","}
     */
    private void parseTypeConstraintList() {
        assert _at(WHERE_KEYWORD);

        advance(); // WHERE_KEYWORD

        PsiBuilder.Marker list = mark();

        while (true) {
            if (at(COMMA)) errorAndAdvance("Type constraint expected");
            parseTypeConstraint();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        list.done(TYPE_CONSTRAINT_LIST);
    }

    /*
     * typeConstraint
     *   : annotations SimpleName ":" type
     *   ;
     */
    private void parseTypeConstraint() {
        PsiBuilder.Marker constraint = mark();

        parseAnnotations(DEFAULT);

        PsiBuilder.Marker reference = mark();
        if (expect(IDENTIFIER, "Expecting type parameter name", COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(REFERENCE_EXPRESSION);
        }
        else {
            reference.drop();
        }

        expect(COLON, "Expecting ':' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET);

        parseTypeRef();

        constraint.done(TYPE_CONSTRAINT);
    }

    private boolean parseFunctionContract() {
        if (at(CONTRACT_KEYWORD)) {
            myExpressionParsing.parseContractDescriptionBlock();
            return true;
        }
        return false;
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private void parseTypeParameter() {
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected");
            return;
        }

        PsiBuilder.Marker mark = mark();

        parseModifierList(GT_COMMA_COLON_SET);

        expect(IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY);

        if (at(COLON)) {
            advance(); // COLON
            parseTypeRef();
        }

        mark.done(TYPE_PARAMETER);

    }

    /*
     * type
     *   : typeModifiers typeReference
     *   ;
     *
     * typeReference
     *   : functionType
     *   : userType
     *   : nullableType
     *   : "dynamic"
     *   ;
     *
     * nullableType
     *   : typeReference "?"
     *   ;
     */
    void parseTypeRef() {
        parseTypeRef(TokenSet.EMPTY);
    }

    void parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY, /* allowSimpleIntersectionTypes */ false);
    }

    void parseTypeRef(TokenSet extraRecoverySet) {
        parseTypeRef(extraRecoverySet, /* allowSimpleIntersectionTypes */ true);
    }

    private void parseTypeRef(TokenSet extraRecoverySet, boolean allowSimpleIntersectionTypes) {
        PsiBuilder.Marker typeRefMarker = parseTypeRefContents(extraRecoverySet, allowSimpleIntersectionTypes);
        typeRefMarker.done(TYPE_REFERENCE);
    }

    // The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
    // on expression-indicating symbols or not
    private PsiBuilder.Marker parseTypeRefContents(TokenSet extraRecoverySet, boolean allowSimpleIntersectionTypes) {
        PsiBuilder.Marker typeRefMarker = mark();

        parseTypeModifierList();

        IElementType lookahead = lookahead(1);
        IElementType lookahead2 = lookahead(2);
        boolean typeBeforeDot = true;
        boolean withContextReceiver = at(CONTEXT_KEYWORD) && lookahead == LPAR;
        boolean wasFunctionTypeParsed = false;

        PsiBuilder.Marker contextReceiversStart = mark();

        if (withContextReceiver) {
            parseContextReceiverList(true);
        }

        PsiBuilder.Marker typeElementMarker = mark();

        if (at(IDENTIFIER) && !(lookahead == DOT && lookahead2 == IDENTIFIER) && lookahead != LT && at(DYNAMIC_KEYWORD)) {
            PsiBuilder.Marker dynamicType = mark();
            advance(); // DYNAMIC_KEYWORD
            dynamicType.done(DYNAMIC_TYPE);
        }
        else if (at(IDENTIFIER) || at(PACKAGE_KEYWORD) || atParenthesizedMutableForPlatformTypes(0)) {
            parseUserType();
        }
        else if (at(LPAR)) {
            PsiBuilder.Marker functionOrParenthesizedType = mark();

            // This may be a function parameter list or just a parenthesized type
            advance(); // LPAR
            parseTypeRefContents(TokenSet.EMPTY, /* allowSimpleIntersectionTypes */ true).drop(); // parenthesized types, no reference element around it is needed

            if (at(RPAR) && lookahead(1) != ARROW) {
                // It's a parenthesized type
                //    (A)
                advance();
                functionOrParenthesizedType.drop();
            }
            else {
                // This must be a function type
                //   (A, B) -> C
                // or
                //   (a : A) -> C
                functionOrParenthesizedType.rollbackTo();
                parseFunctionType(contextReceiversStart.precede());
                wasFunctionTypeParsed = true;
            }
        }
        else {
            errorWithRecovery("Type expected",
                    TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST,
                                   TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON),
                                   extraRecoverySet));
            typeBeforeDot = false;
        }

        // Disabling token merge is required for cases like
        //    Int?.(Foo) -> Bar
        myBuilder.disableJoiningComplexTokens();
        typeElementMarker = parseNullableTypeSuffix(typeElementMarker);
        myBuilder.restoreJoiningComplexTokensState();

        boolean wasIntersection = false;
        if (allowSimpleIntersectionTypes && at(AND)) {
            PsiBuilder.Marker leftTypeRef = typeElementMarker;

            typeElementMarker = typeElementMarker.precede();
            PsiBuilder.Marker intersectionType = leftTypeRef.precede();

            leftTypeRef.done(TYPE_REFERENCE);

            advance(); // &
            parseTypeRef(extraRecoverySet, /* allowSimpleIntersectionTypes */ true);

            intersectionType.done(INTERSECTION_TYPE);
            wasIntersection = true;
        }

        if (typeBeforeDot && at(DOT) && !wasIntersection && !wasFunctionTypeParsed) {
            // This is a receiver for a function type
            //  A.(B) -> C
            //   ^

            PsiBuilder.Marker functionType = contextReceiversStart.precede();

            PsiBuilder.Marker receiverTypeRef = typeElementMarker.precede();
            PsiBuilder.Marker receiverType = receiverTypeRef.precede();
            receiverTypeRef.done(TYPE_REFERENCE);
            receiverType.done(FUNCTION_TYPE_RECEIVER);

            advance(); // DOT

            if (at(LPAR)) {
                parseFunctionType(functionType);
            }
            else {
                functionType.drop();
                error("Expecting function type");
            }

            wasFunctionTypeParsed = true;
        }

        if (withContextReceiver && !wasFunctionTypeParsed) {
            errorWithRecovery("Function type expected expected",
                              TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST,
                                             TokenSet.create(EQ, COMMA, GT, RBRACKET, DOT, RPAR, RBRACE, LBRACE, SEMICOLON),
                                             extraRecoverySet));
        }

        typeElementMarker.drop();
        contextReceiversStart.drop();
        return typeRefMarker;
    }

    @NotNull
    private PsiBuilder.Marker parseNullableTypeSuffix(@NotNull PsiBuilder.Marker typeElementMarker) {
        // ?: is joined regardless of joining state
        while (at(QUEST) && myBuilder.rawLookup(1) != COLON) {
            PsiBuilder.Marker precede = typeElementMarker.precede();
            advance(); // QUEST
            typeElementMarker.done(NULLABLE_TYPE);
            typeElementMarker = precede;
        }
        return typeElementMarker;
    }

    /*
     * userType
     *   : simpleUserType{"."}
     *   ;
     *
     *   recovers on platform types:
     *    - Foo!
     *    - (Mutable)List<Foo>!
     *    - Array<(out) Foo>!
     */
    private void parseUserType() {
        PsiBuilder.Marker userType = mark();

        if (at(PACKAGE_KEYWORD)) {
            PsiBuilder.Marker keyword = mark();
            advance(); // PACKAGE_KEYWORD
            keyword.error("Expecting an element");
            expect(DOT, "Expecting '.'", TokenSet.create(IDENTIFIER, LBRACE, RBRACE));
        }

        PsiBuilder.Marker reference = mark();
        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true);

            if (expect(IDENTIFIER, "Expecting type name", USER_TYPE_NAME_RECOVERY_SET)) {
                reference.done(REFERENCE_EXPRESSION);
            }
            else {
                reference.drop();
                break;
            }

            parseTypeArgumentList();

            recoverOnPlatformTypeSuffix();

            if (!at(DOT)) {
                break;
            }
            if (lookahead(1) == LPAR && !atParenthesizedMutableForPlatformTypes(1)) {
                // This may be a receiver for a function type
                //   Int.(Int) -> Int
                break;
            }

            PsiBuilder.Marker precede = userType.precede();
            userType.done(USER_TYPE);
            userType = precede;

            advance(); // DOT
            reference = mark();
        }

        userType.done(USER_TYPE);
    }

    private boolean atParenthesizedMutableForPlatformTypes(int offset) {
        return recoverOnParenthesizedWordForPlatformTypes(offset, "Mutable", false);
    }

    private boolean recoverOnParenthesizedWordForPlatformTypes(int offset, String word, boolean consume) {
        // Array<(out) Foo>! or (Mutable)List<Bar>!
        if (lookahead(offset) == LPAR && lookahead(offset + 1) == IDENTIFIER && lookahead(offset + 2) == RPAR && lookahead(offset + 3) == IDENTIFIER) {
            PsiBuilder.Marker error = mark();

            advance(offset);

            advance(); // LPAR
            if (!word.equals(myBuilder.getTokenText())) {
                // something other than "out" / "Mutable"
                error.rollbackTo();
                return false;
            }
            else {
                advance(); // IDENTIFIER('out')
                advance(); // RPAR

                if (consume) {
                    error.error("Unexpected tokens");
                }
                else {
                    error.rollbackTo();
                }

                return true;
            }
        }
        return false;
    }

    private void recoverOnPlatformTypeSuffix() {
        // Recovery for platform types
        if (at(EXCL)) {
            PsiBuilder.Marker error = mark();
            advance(); // EXCL
            error.error("Unexpected token");
        }
    }

    /*
     *  (optionalProjection type){","}
     */
    private void parseTypeArgumentList() {
        if (!at(LT)) return;

        PsiBuilder.Marker list = mark();

        tryParseTypeArgumentList(TokenSet.EMPTY);

        list.done(TYPE_ARGUMENT_LIST);
    }

    boolean tryParseTypeArgumentList(TokenSet extraRecoverySet) {
        myBuilder.disableNewlines();
        advance(); // LT

        while (true) {
            PsiBuilder.Marker projection = mark();

            recoverOnParenthesizedWordForPlatformTypes(0, "out", true);

            // Currently we do not allow annotations on star projections and probably we should not
            // Annotations on other kinds of type arguments should be parsed as common type annotations (within parseTypeRef call)
            parseTypeArgumentModifierList();

            if (at(MUL)) {
                advance(); // MUL
            }
            else {
                parseTypeRef(extraRecoverySet);
            }
            projection.done(TYPE_PROJECTION);
            if (!at(COMMA)) break;
            advance(); // COMMA
            if (at(GT)) {
                break;
            }
        }

        boolean atGT = at(GT);
        if (!atGT) {
            error("Expecting a '>'");
        }
        else {
            advance(); // GT
        }
        myBuilder.restoreNewlinesState();
        return atGT;
    }

    /*
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private void parseFunctionType(PsiBuilder.Marker functionType) {
        parseFunctionTypeContents(functionType).done(FUNCTION_TYPE);
    }

    private PsiBuilder.Marker parseFunctionTypeContents(PsiBuilder.Marker functionType) {
        assert _at(LPAR) : tt();

        parseValueParameterList(true, /* typeRequired  = */ true, TokenSet.EMPTY);

        expect(ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST);
        parseTypeRef();

        return functionType;
    }

    private static final TokenSet NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER = TokenSet.create(COMMA, COLON, EQ, RPAR);

    /*package*/ static final TokenSet EXPRESSION_FIRST = TokenSet.create(
            // Prefix
            MINUS, PLUS, MINUSMINUS, PLUSPLUS,
            EXCL, EXCLEXCL, // Joining complex tokens makes it necessary to put EXCLEXCL here
            // Atomic

            COLONCOLON, // callable reference

            LPAR, // parenthesized

            // literal constant
            TRUE_KEYWORD, FALSE_KEYWORD,
            INTERPOLATION_PREFIX, OPEN_QUOTE,
            INTEGER_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,
            NULL_KEYWORD,

            LBRACE, // functionLiteral
            FUN_KEYWORD, // expression function

            THIS_KEYWORD, // this
            SUPER_KEYWORD, // super

            IF_KEYWORD, // if
            WHEN_KEYWORD, // when
            TRY_KEYWORD, // try
            OBJECT_KEYWORD, // object

            // jump
            THROW_KEYWORD,
            RETURN_KEYWORD,
            CONTINUE_KEYWORD,
            BREAK_KEYWORD,

            // loop
            FOR_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,

            IDENTIFIER, // SimpleName

            AT, // Just for better recovery and maybe for annotations

            LBRACKET // Collection literal expression
    );

    /*package*/ static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, COMMA, RBRACE, RPAR, RBRACKET
    );

    private static final TokenSet USER_TYPE_NAME_RECOVERY_SET =
        TokenSet.orSet(EXPRESSION_FIRST, EXPRESSION_FOLLOW, DECLARATION_FIRST);

    /*
     * functionParameters
     *   : "(" functionParameter{","}? ")"
     *   ;
     *
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     *
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private void parseValueParameterList(boolean isFunctionTypeContents, boolean typeRequired, TokenSet recoverySet) {
        assert _at(LPAR);
        PsiBuilder.Marker parameters = mark();

        myBuilder.disableNewlines();

        valueParameterLoop(
                isFunctionTypeContents,
                recoverySet,
                () -> {
                    if (isFunctionTypeContents) {
                        if (!tryParseValueParameter(typeRequired)) {
                            PsiBuilder.Marker valueParameter = mark();
                            parseFunctionTypeValueParameterModifierList();
                            parseTypeRef();
                            closeDeclarationWithCommentBinders(valueParameter, VALUE_PARAMETER, false);
                        }
                    }
                    else {
                        parseValueParameter(typeRequired);
                    }
                });

        myBuilder.restoreNewlinesState();

        parameters.done(VALUE_PARAMETER_LIST);
    }

    private void valueParameterLoop(boolean inFunctionTypeContext, TokenSet recoverySet, Runnable parseParameter) {
        advance(); // LPAR

        if (!at(RPAR) && !atSet(recoverySet)) {
            while (true) {
                int offsetBefore = myBuilder.getCurrentOffset();
                if (at(COMMA)) {
                    errorAndAdvance("Expecting a parameter declaration");
                }
                else if (at(RPAR)) {
                    break;
                }

                parseParameter.run();

                if (at(COMMA)) {
                    advance(); // COMMA
                }
                else if (at(COLON)) {
                    // recovery for the case "fun bar(x: Array<Int> : Int)" when we've just parsed "x: Array<Int>"
                    // error should be reported in the `parseValueParameter` call
                    //noinspection UnnecessaryContinue
                    continue;
                }
                else {
                    if (!at(RPAR)) error("Expecting comma or ')'");
                    if (!atSet(inFunctionTypeContext ? LAMBDA_VALUE_PARAMETER_FIRST : VALUE_PARAMETER_FIRST)) break;
                    if (offsetBefore == myBuilder.getCurrentOffset()) break;
                }
            }
        }

        expect(RPAR, "Expecting ')'", recoverySet);
    }

    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private boolean tryParseValueParameter(boolean typeRequired) {
        return parseValueParameter(true, typeRequired);
    }

    public void parseValueParameter(boolean typeRequired) {
        parseValueParameter(false, typeRequired);
    }

    private boolean parseValueParameter(boolean rollbackOnFailure, boolean typeRequired) {
        PsiBuilder.Marker parameter = mark();

        parseModifierList(NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER);

        if (at(VAR_KEYWORD) || at(VAL_KEYWORD)) {
            advance(); // VAR_KEYWORD | VAL_KEYWORD
        }

        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo();
            return false;
        }

        closeDeclarationWithCommentBinders(parameter, VALUE_PARAMETER, false);
        return true;
    }

    /*
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private boolean parseFunctionParameterRest(boolean typeRequired) {
        boolean noErrors = true;

        // Recovery for the case 'fun foo(Array<String>) {}'
        // Recovery for the case 'fun foo(: Int) {}'
        if ((at(IDENTIFIER) && lookahead(1) == LT) || at(COLON)) {
            error("Parameter name expected");
            if (at(COLON)) {
                // We keep noErrors == true so that unnamed parameters starting with ":" are not rolled back during parsing of functional types
                advance(); // COLON
            }
            else {
                noErrors = false;
            }
            parseTypeRef();
        }
        else {
            expect(IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET);

            if (at(COLON)) {
                advance(); // COLON

                if (at(IDENTIFIER) && lookahead(1) == COLON) {
                    // recovery for the case "fun foo(x: y: Int)" when we're at "y: " it's likely that this is a name of the next parameter,
                    // not a type reference of the current one
                    error("Type reference expected");
                    return false;
                }

                parseTypeRef();
            }
            else if (typeRequired) {
                errorWithRecovery("Parameters must have type annotation", PARAMETER_NAME_RECOVERY_SET);
                noErrors = false;
            }
        }

        if (at(EQ)) {
            advance(); // EQ
            myExpressionParsing.parseExpression();
        }

        return noErrors;
    }

    @Override
    protected KotlinParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return createForTopLevel(builder);
    }

    /*package*/ static class ModifierDetector implements Consumer<IElementType> {
        private boolean enumDetected = false;
        private boolean companionDetected = false;

        @Override
        public void consume(IElementType item) {
            if (item == KtTokens.ENUM_KEYWORD) {
                enumDetected = true;
            }
            else if (item == KtTokens.COMPANION_KEYWORD) {
                companionDetected = true;
            }
        }

        public boolean isEnumDetected() {
            return enumDetected;
        }

        public boolean isCompanionDetected() {
            return companionDetected;
        }
    }

    enum AnnotationParsingMode {
        DEFAULT(false, true, true, false, false),
        FILE_ANNOTATIONS_BEFORE_PACKAGE(true, true, false, false, false),
        FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED(true, true, false, false, false),
        TYPE_CONTEXT(false, true, false, true, false),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS(false, true, true, true, true),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT(false, true, false, true, true),
        NO_ANNOTATIONS_NO_CONTEXT(false, false, false, false, false);

        final boolean isFileAnnotationParsingMode;
        final boolean allowAnnotations;
        final boolean allowContextList;
        final boolean withSignificantWhitespaceBeforeArguments;
        final boolean typeContext;

        AnnotationParsingMode(
                boolean isFileAnnotationParsingMode,
                boolean allowAnnotations,
                boolean allowContextList,
                boolean typeContext,
                boolean withSignificantWhitespaceBeforeArguments
        ) {
            this.isFileAnnotationParsingMode = isFileAnnotationParsingMode;
            this.allowAnnotations = allowAnnotations;
            this.allowContextList = allowContextList;
            this.typeContext = typeContext;
            this.withSignificantWhitespaceBeforeArguments = withSignificantWhitespaceBeforeArguments;
        }
    }
}
