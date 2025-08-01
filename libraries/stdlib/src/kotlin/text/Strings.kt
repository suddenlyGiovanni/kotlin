/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StringsKt")

package kotlin.text

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName

/**
 * Returns a copy of this string converted to upper case using the rules of the default locale.
 */
@Deprecated("Use uppercase() instead.", ReplaceWith("uppercase()"))
@DeprecatedSinceKotlin(warningSince = "1.5", errorSince = "2.1")
public expect fun String.toUpperCase(): String

/**
 * Returns a copy of this string converted to upper case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.uppercase
 */
@SinceKotlin("1.5")
public expect fun String.uppercase(): String

/**
 * Returns a copy of this string converted to lower case using the rules of the default locale.
 */
@Deprecated("Use lowercase() instead.", ReplaceWith("lowercase()"))
@DeprecatedSinceKotlin(warningSince = "1.5", errorSince = "2.1")
public expect fun String.toLowerCase(): String

/**
 * Returns a copy of this string converted to lower case using Unicode mapping rules of the invariant locale.
 *
 * This function supports one-to-many and many-to-one character mapping,
 * thus the length of the returned string can be different from the length of the original string.
 *
 * @sample samples.text.Strings.lowercase
 */
@SinceKotlin("1.5")
public expect fun String.lowercase(): String

/**
 * Returns a copy of this string having its first letter titlecased using the rules of the default locale,
 * or the original string if it's empty or already starts with a title case letter.
 *
 * The title case of a character is usually the same as its upper case with several exceptions.
 * The particular list of characters with the special title case form depends on the underlying platform.
 *
 * @sample samples.text.Strings.capitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public expect fun String.capitalize(): String

/**
 * Returns a copy of this string having its first letter lowercased using the rules of the default locale,
 * or the original string if it's empty or already starts with a lower case letter.
 *
 * @sample samples.text.Strings.decapitalize
 */
@Deprecated("Use replaceFirstChar instead.", ReplaceWith("replaceFirstChar { it.lowercase() }"))
@DeprecatedSinceKotlin(warningSince = "1.5")
public expect fun String.decapitalize(): String

/**
 * Returns a subsequence of this char sequence having leading and trailing characters matching the [predicate] removed.
 */
public inline fun CharSequence.trim(predicate: (Char) -> Boolean): CharSequence {
    var startIndex = 0
    var endIndex = length - 1
    var startFound = false

    while (startIndex <= endIndex) {
        val index = if (!startFound) startIndex else endIndex
        val match = predicate(this[index])

        if (!startFound) {
            if (!match)
                startFound = true
            else
                startIndex += 1
        } else {
            if (!match)
                break
            else
                endIndex -= 1
        }
    }

    return subSequence(startIndex, endIndex + 1)
}

/**
 * Returns a string having leading and trailing characters matching the [predicate] removed.
 */
public inline fun String.trim(predicate: (Char) -> Boolean): String =
    (this as CharSequence).trim(predicate).toString()

/**
 * Returns a subsequence of this char sequence having leading characters matching the [predicate] removed.
 */
public inline fun CharSequence.trimStart(predicate: (Char) -> Boolean): CharSequence {
    for (index in this.indices)
        if (!predicate(this[index]))
            return subSequence(index, length)

    return ""
}

/**
 * Returns a string having leading characters matching the [predicate] removed.
 */
public inline fun String.trimStart(predicate: (Char) -> Boolean): String =
    (this as CharSequence).trimStart(predicate).toString()

/**
 * Returns a subsequence of this char sequence having trailing characters matching the [predicate] removed.
 */
public inline fun CharSequence.trimEnd(predicate: (Char) -> Boolean): CharSequence {
    for (index in this.indices.reversed())
        if (!predicate(this[index]))
            return subSequence(0, index + 1)

    return ""
}

/**
 * Returns a string having trailing characters matching the [predicate] removed.
 */
public inline fun String.trimEnd(predicate: (Char) -> Boolean): String =
    (this as CharSequence).trimEnd(predicate).toString()

/**
 * Returns a subsequence of this char sequence having leading and trailing characters from the [chars] array removed.
 */
public fun CharSequence.trim(vararg chars: Char): CharSequence = trim { it in chars }

/**
 * Returns a string having leading and trailing characters from the [chars] array removed.
 */
public fun String.trim(vararg chars: Char): String = trim { it in chars }

/**
 * Returns a subsequence of this char sequence having leading characters from the [chars] array removed.
 */
public fun CharSequence.trimStart(vararg chars: Char): CharSequence = trimStart { it in chars }

/**
 * Returns a string having leading characters from the [chars] array removed.
 */
public fun String.trimStart(vararg chars: Char): String = trimStart { it in chars }

/**
 * Returns a subsequence of this char sequence having trailing characters from the [chars] array removed.
 */
public fun CharSequence.trimEnd(vararg chars: Char): CharSequence = trimEnd { it in chars }

/**
 * Returns a string having trailing characters from the [chars] array removed.
 */
public fun String.trimEnd(vararg chars: Char): String = trimEnd { it in chars }

/**
 * Returns a subsequence of this char sequence having leading and trailing whitespace removed.
 */
public fun CharSequence.trim(): CharSequence = trim(Char::isWhitespace)

/**
 * Returns a string having leading and trailing whitespace removed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trim(): String = (this as CharSequence).trim().toString()

/**
 * Returns a subsequence of this char sequence having leading whitespace removed.
 */
public fun CharSequence.trimStart(): CharSequence = trimStart(Char::isWhitespace)

/**
 * Returns a string having leading whitespace removed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trimStart(): String = (this as CharSequence).trimStart().toString()

/**
 * Returns a subsequence of this char sequence having trailing whitespace removed.
 */
public fun CharSequence.trimEnd(): CharSequence = trimEnd(Char::isWhitespace)

/**
 * Returns a string having trailing whitespace removed.
 */
@kotlin.internal.InlineOnly
public inline fun String.trimEnd(): String = (this as CharSequence).trimEnd().toString()

/**
 * Returns a char sequence with content of this char sequence padded at the beginning
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @return Returns a char sequence of length at least [length] consisting of `this` char sequence prepended with [padChar] as many times
 * as are necessary to reach that length.
 * @sample samples.text.Strings.padStart
 */
public fun CharSequence.padStart(length: Int, padChar: Char = ' '): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length)
        return this.subSequence(0, this.length)

    val sb = StringBuilder(length)
    for (i in 1..(length - this.length))
        sb.append(padChar)
    sb.append(this)
    return sb
}

/**
 * Pads the string to the specified [length] at the beginning with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @return Returns a string of length at least [length] consisting of `this` string prepended with [padChar] as many times
 * as are necessary to reach that length.
 * @sample samples.text.Strings.padStart
 */
public fun String.padStart(length: Int, padChar: Char = ' '): String =
    (this as CharSequence).padStart(length, padChar).toString()

/**
 * Returns a char sequence with content of this char sequence padded at the end
 * to the specified [length] with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @return Returns a char sequence of length at least [length] consisting of `this` char sequence appended with [padChar] as many times
 * as are necessary to reach that length.
 * @sample samples.text.Strings.padEnd
 */
public fun CharSequence.padEnd(length: Int, padChar: Char = ' '): CharSequence {
    if (length < 0)
        throw IllegalArgumentException("Desired length $length is less than zero.")
    if (length <= this.length)
        return this.subSequence(0, this.length)

    val sb = StringBuilder(length)
    sb.append(this)
    for (i in 1..(length - this.length))
        sb.append(padChar)
    return sb
}

/**
 * Pads the string to the specified [length] at the end with the specified character or space.
 *
 * @param length the desired string length.
 * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
 * @return Returns a string of length at least [length] consisting of `this` string appended with [padChar] as many times
 * as are necessary to reach that length.
 * @sample samples.text.Strings.padEnd
 */
public fun String.padEnd(length: Int, padChar: Char = ' '): String =
    (this as CharSequence).padEnd(length, padChar).toString()

/**
 * Returns `true` if this nullable char sequence is either `null` or empty.
 *
 * @sample samples.text.Strings.stringIsNullOrEmpty
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.length == 0
}

/**
 * Returns `true` if this char sequence is empty (contains no characters).
 *
 * @sample samples.text.Strings.stringIsEmpty
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isEmpty(): Boolean = length == 0

/**
 * Returns `true` if this char sequence is not empty.
 *
 * @sample samples.text.Strings.stringIsNotEmpty
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isNotEmpty(): Boolean = length > 0

/**
 * Returns `true` if this char sequence is empty or consists solely of whitespace characters according to [Char.isWhitespace].
 *
 * @sample samples.text.Strings.stringIsBlank
 */
public fun CharSequence.isBlank(): Boolean = all { it.isWhitespace() }

/**
 * Returns `true` if this char sequence is not empty and contains some characters except whitespace characters.
 *
 * @sample samples.text.Strings.stringIsNotBlank
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.isNotBlank(): Boolean = !isBlank()

/**
 * Returns `true` if this nullable char sequence is either `null` or empty or consists solely of whitespace characters.
 *
 * @sample samples.text.Strings.stringIsNullOrBlank
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrBlank(): Boolean {
    contract {
        returns(false) implies (this@isNullOrBlank != null)
    }

    return this == null || this.isBlank()
}

/**
 * Iterator for characters of the given char sequence.
 */
public operator fun CharSequence.iterator(): CharIterator = object : CharIterator() {
    private var index = 0

    public override fun nextChar(): Char = get(index++)

    public override fun hasNext(): Boolean = index < length
}

/** Returns the string if it is not `null`, or the empty string otherwise. */
@kotlin.internal.InlineOnly
public inline fun String?.orEmpty(): String = this ?: ""

/**
 * Returns this char sequence if it's not empty
 * or the result of calling [defaultValue] function if the char sequence is empty.
 *
 * @sample samples.text.Strings.stringIfEmpty
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <C, R> C.ifEmpty(defaultValue: () -> R): R where C : CharSequence, C : R {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isEmpty()) defaultValue() else this
}

/**
 * Returns this char sequence if it is not empty and doesn't consist solely of whitespace characters,
 * or the result of calling [defaultValue] function otherwise.
 *
 * @sample samples.text.Strings.stringIfBlank
 */
@SinceKotlin("1.3")
@kotlin.internal.InlineOnly
public inline fun <C, R> C.ifBlank(defaultValue: () -> R): R where C : CharSequence, C : R {
    contract {
        callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE)
    }
    return if (isBlank()) defaultValue() else this
}

/**
 * Returns the range of valid character indices for this char sequence.
 */
public val CharSequence.indices: IntRange
    get() = 0..length - 1

/**
 * Returns the index of the last character in the char sequence or -1 if it is empty.
 */
public val CharSequence.lastIndex: Int
    get() = this.length - 1

/**
 * Returns `true` if this CharSequence has Unicode surrogate pair at the specified [index].
 */
public fun CharSequence.hasSurrogatePairAt(index: Int): Boolean {
    return index in 0..length - 2
            && this[index].isHighSurrogate()
            && this[index + 1].isLowSurrogate()
}

/**
 * Returns a substring specified by the given [range] of indices.
 */
public fun String.substring(range: IntRange): String = substring(range.start, range.endInclusive + 1)

/**
 * Returns a subsequence of this char sequence specified by the given [range] of indices.
 */
public fun CharSequence.subSequence(range: IntRange): CharSequence = subSequence(range.start, range.endInclusive + 1)

/**
 * Returns a subsequence of this char sequence.
 *
 * This extension is chosen only for invocation with old-named parameters.
 * Replace parameter names with the same as those of [CharSequence.subSequence].
 */
@kotlin.internal.InlineOnly
@Suppress("EXTENSION_SHADOWED_BY_MEMBER") // false warning
@Deprecated("Use parameters named startIndex and endIndex.", ReplaceWith("subSequence(startIndex = start, endIndex = end)"))
public inline fun String.subSequence(start: Int, end: Int): CharSequence = subSequence(start, end)

/**
 * Returns a substring of chars from a range of this char sequence starting at the [startIndex] and ending right before the [endIndex].
 *
 * @param startIndex the start index (inclusive).
 * @param endIndex the end index (exclusive). If not specified, the length of the char sequence is used.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.substring(startIndex: Int, endIndex: Int = length): String = subSequence(startIndex, endIndex).toString()

/**
 * Returns a substring of chars at indices from the specified [range] of this char sequence.
 */
public fun CharSequence.substring(range: IntRange): String = subSequence(range.start, range.endInclusive + 1).toString()

/**
 * Returns a substring before the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBefore(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring before the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBefore(delimiter: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring after the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfter(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}

/**
 * Returns a substring after the first occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfter(delimiter: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length, length)
}

/**
 * Returns a substring before the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBeforeLast(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring before the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringBeforeLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

/**
 * Returns a substring after the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfterLast(delimiter: Char, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}

/**
 * Returns a substring after the last occurrence of [delimiter].
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.substringAfterLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + delimiter.length, length)
}

/**
 * Returns a char sequence with content of this char sequence where its part at the given range
 * is replaced with the [replacement] char sequence.
 * @param startIndex the index of the first character to be replaced.
 * @param endIndex the index of the first character after the replacement to keep in the string.
 */
public fun CharSequence.replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): CharSequence {
    if (endIndex < startIndex)
        throw IndexOutOfBoundsException("End index ($endIndex) is less than start index ($startIndex).")
    val sb = StringBuilder()
    sb.appendRange(this, 0, startIndex)
    sb.append(replacement)
    sb.appendRange(this, endIndex, length)
    return sb
}

/**
 * Replaces the part of the string at the given range with the [replacement] char sequence.
 * @param startIndex the index of the first character to be replaced.
 * @param endIndex the index of the first character after the replacement to keep in the string.
 */
@kotlin.internal.InlineOnly
public inline fun String.replaceRange(startIndex: Int, endIndex: Int, replacement: CharSequence): String =
    (this as CharSequence).replaceRange(startIndex, endIndex, replacement).toString()

/**
 * Returns a char sequence with content of this char sequence where its part at the given [range]
 * is replaced with the [replacement] char sequence.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
public fun CharSequence.replaceRange(range: IntRange, replacement: CharSequence): CharSequence =
    replaceRange(range.start, range.endInclusive + 1, replacement)

/**
 * Replace the part of string at the given [range] with the [replacement] string.
 *
 * The end index of the [range] is included in the part to be replaced.
 */
@kotlin.internal.InlineOnly
public inline fun String.replaceRange(range: IntRange, replacement: CharSequence): String =
    (this as CharSequence).replaceRange(range, replacement).toString()

/**
 * Returns a [CharSequence] obtained by removing the specified subsequence from this char sequence.
 *
 * @param startIndex the beginning (inclusive) of the subsequence to remove.
 * @param endIndex the end (exclusive) of the subsequence to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this char sequence indices
 *   or when `startIndex > endIndex`.
 *
 * @sample samples.text.Strings.removeRangeCharSequence
 */
public fun CharSequence.removeRange(startIndex: Int, endIndex: Int): CharSequence {
    if (endIndex < startIndex)
        throw IndexOutOfBoundsException("End index ($endIndex) is less than start index ($startIndex).")

    if (endIndex == startIndex)
        return this.subSequence(0, length)

    val sb = StringBuilder(length - (endIndex - startIndex))
    sb.appendRange(this, 0, startIndex)
    sb.appendRange(this, endIndex, length)
    return sb
}

/**
 * Returns a [String] obtained by removing the specified substring from this string.
 *
 * @param startIndex the beginning (inclusive) of the substring to remove.
 * @param endIndex the end (exclusive) of the substring to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string indices
 *   or when `startIndex > endIndex`.
 *
 * @sample samples.text.Strings.removeRangeString
 */
@kotlin.internal.InlineOnly
public inline fun String.removeRange(startIndex: Int, endIndex: Int): String =
    (this as CharSequence).removeRange(startIndex, endIndex).toString()

/**
 * Returns a [CharSequence] obtained by removing the specified subsequence from this char sequence.
 *
 * @param range the range of indexes of the subsequence to remove.
 *   Note: the character at index [IntRange.endInclusive] of the [range] is removed as well.
 *
 * @sample samples.text.Strings.removeRangeCharSequence
 */
public fun CharSequence.removeRange(range: IntRange): CharSequence = removeRange(range.start, range.endInclusive + 1)

/**
 * Returns a [String] obtained by removing the specified substring from this char sequence.
 *
 * @param range the range of indexes of the substring to remove.
 *   Note: the character at index [IntRange.endInclusive] of the [range] is removed as well.
 *
 * @sample samples.text.Strings.removeRangeString
 */
@kotlin.internal.InlineOnly
public inline fun String.removeRange(range: IntRange): String =
    (this as CharSequence).removeRange(range).toString()

/**
 * If this char sequence starts with the given [prefix], returns a new char sequence
 * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removePrefix(prefix: CharSequence): CharSequence {
    if (startsWith(prefix)) {
        return subSequence(prefix.length, length)
    }
    return subSequence(0, length)
}

/**
 * If this string starts with the given [prefix], returns a copy of this string
 * with the prefix removed. Otherwise, returns this string.
 */
public fun String.removePrefix(prefix: CharSequence): String {
    if (startsWith(prefix)) {
        return substring(prefix.length)
    }
    return this
}

/**
 * If this char sequence ends with the given [suffix], returns a new char sequence
 * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSuffix(suffix: CharSequence): CharSequence {
    if (endsWith(suffix)) {
        return subSequence(0, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * If this string ends with the given [suffix], returns a copy of this string
 * with the suffix removed. Otherwise, returns this string.
 */
public fun String.removeSuffix(suffix: CharSequence): String {
    if (endsWith(suffix)) {
        return substring(0, length - suffix.length)
    }
    return this
}

/**
 * When this char sequence starts with the given [prefix] and ends with the given [suffix],
 * returns a new char sequence having both the given [prefix] and [suffix] removed.
 * Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSurrounding(prefix: CharSequence, suffix: CharSequence): CharSequence {
    if ((length >= prefix.length + suffix.length) && startsWith(prefix) && endsWith(suffix)) {
        return subSequence(prefix.length, length - suffix.length)
    }
    return subSequence(0, length)
}

/**
 * Removes from a string both the given [prefix] and [suffix] if and only if
 * it starts with the [prefix] and ends with the [suffix].
 * Otherwise, returns this string unchanged.
 */
public fun String.removeSurrounding(prefix: CharSequence, suffix: CharSequence): String {
    if ((length >= prefix.length + suffix.length) && startsWith(prefix) && endsWith(suffix)) {
        return substring(prefix.length, length - suffix.length)
    }
    return this
}

/**
 * When this char sequence starts with and ends with the given [delimiter],
 * returns a new char sequence having this [delimiter] removed both from the start and end.
 * Otherwise, returns a new char sequence with the same characters.
 */
public fun CharSequence.removeSurrounding(delimiter: CharSequence): CharSequence = removeSurrounding(delimiter, delimiter)

/**
 * Removes the given [delimiter] string from both the start and the end of this string
 * if and only if it starts with and ends with the [delimiter].
 * Otherwise, returns this string unchanged.
 */
public fun String.removeSurrounding(delimiter: CharSequence): String = removeSurrounding(delimiter, delimiter)

/**
 * Replace part of string before the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBefore(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBefore(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string after the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfter(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string after the first occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfter(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = indexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + delimiter.length, length, replacement)
}

/**
 * Replace part of string after the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceAfterLast(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(index + 1, length, replacement)
}

/**
 * Replace part of string before the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBeforeLast(delimiter: Char, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}

/**
 * Replace part of string before the last occurrence of given delimiter with the [replacement] string.
 * If the string does not contain the delimiter, returns [missingDelimiterValue] which defaults to the original string.
 */
public fun String.replaceBeforeLast(delimiter: String, replacement: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else replaceRange(0, index, replacement)
}


// public fun String.replace(oldChar: Char, newChar: Char, ignoreCase: Boolean): String // JVM- and JS-specific
// public fun String.replace(oldValue: String, newValue: String, ignoreCase: Boolean): String // JVM- and JS-specific

/**
 * Replaces all occurrences of the given regular expression [regex] in this char sequence
 * with the specified [replacement] expression.
 *
 * This is a convenience function that is equivalent to `regex.replace(this, replacement)`.
 * For details about its behaviour and the substitution syntax of [replacement] expression, refer to [Regex.replace].
 *
 * @sample samples.text.Strings.replaceWithExpression
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.replace(regex: Regex, replacement: String): String = regex.replace(this, replacement)

/**
 * Returns a new string obtained by replacing each substring of this char sequence that matches the given regular expression
 * with the result of the given function [transform] that takes [MatchResult] and returns a string to be used as a
 * replacement for that match.
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.replace(regex: Regex, noinline transform: (MatchResult) -> CharSequence): String =
    regex.replace(this, transform)

/**
 * Replaces the first occurrence of the given regular expression [regex] in this char sequence
 * with the specified [replacement] expression.
 *
 * This is a convenience function that is equivalent to `regex.replaceFirst(this, replacement)`.
 * For details about its behaviour and the substitution syntax of [replacement] expression, refer to [Regex.replaceFirst].
 *
 * @sample samples.text.Strings.replaceFirstWithExpression
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.replaceFirst(regex: Regex, replacement: String): String = regex.replaceFirst(this, replacement)

/**
 * Returns a copy of this string having its first character replaced with the result of the specified [transform],
 * or the original string if it's empty.
 *
 * @param transform function that takes the first character and returns the result of the transform applied to the character.
 *
 * @sample samples.text.Strings.replaceFirstChar
 */
@SinceKotlin("1.5")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("replaceFirstCharWithChar")
@kotlin.internal.InlineOnly
public inline fun String.replaceFirstChar(transform: (Char) -> Char): String {
    return if (isNotEmpty()) transform(this[0]) + substring(1) else this
}

/**
 * Returns a copy of this string having its first character replaced with the result of the specified [transform],
 * or the original string if it's empty.
 *
 * @param transform function that takes the first character and returns the result of the transform applied to the character.
 *
 * @sample samples.text.Strings.replaceFirstChar
 */
@SinceKotlin("1.5")
@OptIn(kotlin.experimental.ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("replaceFirstCharWithCharSequence")
@kotlin.internal.InlineOnly
public inline fun String.replaceFirstChar(transform: (Char) -> CharSequence): String {
    return if (isNotEmpty()) transform(this[0]).toString() + substring(1) else this
}


/**
 * Returns `true` if this char sequence matches the given regular expression.
 */
@kotlin.internal.InlineOnly
public inline infix fun CharSequence.matches(regex: Regex): Boolean = regex.matches(this)

/**
 * Implementation of [regionMatches] for CharSequences.
 * Invoked when it's already known that arguments are not Strings, so that no additional type checks are performed.
 */
internal fun CharSequence.regionMatchesImpl(thisOffset: Int, other: CharSequence, otherOffset: Int, length: Int, ignoreCase: Boolean): Boolean {
    if ((otherOffset < 0) || (thisOffset < 0) || (thisOffset > this.length - length) || (otherOffset > other.length - length)) {
        return false
    }

    for (index in 0 until length) {
        if (!this[thisOffset + index].equals(other[otherOffset + index], ignoreCase))
            return false
    }
    return true
}

/**
 * Returns `true` if this char sequence starts with the specified character.
 */
public fun CharSequence.startsWith(char: Char, ignoreCase: Boolean = false): Boolean =
    this.length > 0 && this[0].equals(char, ignoreCase)

/**
 * Returns `true` if this char sequence ends with the specified character.
 */
public fun CharSequence.endsWith(char: Char, ignoreCase: Boolean = false): Boolean =
    this.length > 0 && this[lastIndex].equals(char, ignoreCase)

/**
 * Returns `true` if this char sequence starts with the specified prefix.
 */
public fun CharSequence.startsWith(prefix: CharSequence, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && prefix is String)
        return this.startsWith(prefix)
    else
        return regionMatchesImpl(0, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if a substring of this char sequence starting at the specified offset [startIndex] starts with the specified prefix.
 */
public fun CharSequence.startsWith(prefix: CharSequence, startIndex: Int, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && prefix is String)
        return this.startsWith(prefix, startIndex)
    else
        return regionMatchesImpl(startIndex, prefix, 0, prefix.length, ignoreCase)
}

/**
 * Returns `true` if this char sequence ends with the specified suffix.
 */
public fun CharSequence.endsWith(suffix: CharSequence, ignoreCase: Boolean = false): Boolean {
    if (!ignoreCase && this is String && suffix is String)
        return this.endsWith(suffix)
    else
        return regionMatchesImpl(length - suffix.length, suffix, 0, suffix.length, ignoreCase)
}


// common prefix and suffix

/**
 * Returns the longest string `prefix` such that this char sequence and [other] char sequence both start with this prefix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common prefix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @sample samples.text.Strings.commonPrefixWith
 */
public fun CharSequence.commonPrefixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val shortestLength = minOf(this.length, other.length)

    var i = 0
    while (i < shortestLength && this[i].equals(other[i], ignoreCase = ignoreCase)) {
        i++
    }
    if (this.hasSurrogatePairAt(i - 1) || other.hasSurrogatePairAt(i - 1)) {
        i--
    }
    return subSequence(0, i).toString()
}

/**
 * Returns the longest string `suffix` such that this char sequence and [other] char sequence both end with this suffix,
 * taking care not to split surrogate pairs.
 * If this and [other] have no common suffix, returns the empty string.

 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @sample samples.text.Strings.commonSuffixWith
 */
public fun CharSequence.commonSuffixWith(other: CharSequence, ignoreCase: Boolean = false): String {
    val thisLength = this.length
    val otherLength = other.length
    val shortestLength = minOf(thisLength, otherLength)

    var i = 0
    while (i < shortestLength && this[thisLength - i - 1].equals(other[otherLength - i - 1], ignoreCase = ignoreCase)) {
        i++
    }
    if (this.hasSurrogatePairAt(thisLength - i - 1) || other.hasSurrogatePairAt(otherLength - i - 1)) {
        i--
    }
    return subSequence(thisLength - i, thisLength).toString()
}


// indexOfAny()

/**
 * Finds the index of the first occurrence of any of the specified [chars] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @return An index of the first occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
public fun CharSequence.indexOfAny(chars: CharArray, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    if (!ignoreCase && chars.size == 1 && this is String) {
        val char = chars.single()
        return nativeIndexOf(char, startIndex)
    }

    for (index in startIndex.coerceAtLeast(0)..lastIndex) {
        val charAtIndex = get(index)
        if (chars.any { it.equals(charAtIndex, ignoreCase) })
            return index
    }
    return -1
}

/**
 * Finds the index of the last occurrence of any of the specified [chars] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @return An index of the last occurrence of matched character from [chars] or -1 if none of [chars] are found.
 *
 */
public fun CharSequence.lastIndexOfAny(chars: CharArray, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    if (!ignoreCase && chars.size == 1 && this is String) {
        val char = chars.single()
        return nativeLastIndexOf(char, startIndex)
    }


    for (index in startIndex.coerceAtMost(lastIndex) downTo 0) {
        val charAtIndex = get(index)
        if (chars.any { it.equals(charAtIndex, ignoreCase) })
            return index
    }

    return -1
}


private fun CharSequence.indexOf(other: CharSequence, startIndex: Int, endIndex: Int, ignoreCase: Boolean, last: Boolean = false): Int {
    val indices = if (!last)
        startIndex.coerceAtLeast(0)..endIndex.coerceAtMost(length)
    else
        startIndex.coerceAtMost(lastIndex) downTo endIndex.coerceAtLeast(0)

    if (this is String && other is String) { // smart cast
        for (index in indices) {
            if (other.regionMatches(0, this, index, other.length, ignoreCase))
                return index
        }
    } else {
        for (index in indices) {
            if (other.regionMatchesImpl(0, this, index, other.length, ignoreCase))
                return index
        }
    }
    return -1
}

private fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int, ignoreCase: Boolean, last: Boolean): Pair<Int, String>? {
    if (!ignoreCase && strings.size == 1) {
        val string = strings.single()
        val index = if (!last) indexOf(string, startIndex) else lastIndexOf(string, startIndex)
        return if (index < 0) null else index to string
    }

    val indices = if (!last) startIndex.coerceAtLeast(0)..length else startIndex.coerceAtMost(lastIndex) downTo 0

    if (this is String) {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatches(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    } else {
        for (index in indices) {
            val matchingString = strings.firstOrNull { it.regionMatchesImpl(0, this, index, it.length, ignoreCase) }
            if (matchingString != null)
                return index to matchingString
        }
    }

    return null
}

/**
 * Finds the first occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return A pair of an index of the first occurrence of matched string from [strings] and the string matched
 * or `null` if none of [strings] are found.
 */
public fun CharSequence.findAnyOf(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Pair<Int, String>? =
    findAnyOf(strings, startIndex, ignoreCase, last = false)

/**
 * Finds the last occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return A pair of an index of the last occurrence of matched string from [strings] and the string matched or `null` if none of [strings] are found.
 */
public fun CharSequence.findLastAnyOf(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Pair<Int, String>? =
    findAnyOf(strings, startIndex, ignoreCase, last = true)

/**
 * Finds the index of the first occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return An index of the first occurrence of matched string from [strings] or -1 if none of [strings] are found.
 */
public fun CharSequence.indexOfAny(strings: Collection<String>, startIndex: Int = 0, ignoreCase: Boolean = false): Int =
    findAnyOf(strings, startIndex, ignoreCase, last = false)?.first ?: -1

/**
 * Finds the index of the last occurrence of any of the specified [strings] in this char sequence,
 * starting from the specified [startIndex] and optionally ignoring the case.
 *
 * To avoid ambiguous results when strings in [strings] have characters in common, this method proceeds from
 * the end toward the beginning of this string, and finds at each position the first element in [strings]
 * that matches this string at that position.
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return An index of the last occurrence of matched string from [strings] or -1 if none of [strings] are found.
 */
public fun CharSequence.lastIndexOfAny(strings: Collection<String>, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int =
    findAnyOf(strings, startIndex, ignoreCase, last = true)?.first ?: -1


// indexOf

/**
 * Returns the index within this string of the first occurrence of the specified character, starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @return An index of the first occurrence of [char] or -1 if none is found.
 */
public fun CharSequence.indexOf(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeIndexOf(char, startIndex)
}

/**
 * Returns the index within this char sequence of the first occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return An index of the first occurrence of [string] or `-1` if none is found.
 * @sample samples.text.Strings.indexOf
 */
public fun CharSequence.indexOf(string: String, startIndex: Int = 0, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOf(string, startIndex, length, ignoreCase)
    else
        nativeIndexOf(string, startIndex)
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified character,
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a character. By default `false`.
 * @return An index of the last occurrence of [char] or -1 if none is found.
 */
public fun CharSequence.lastIndexOf(char: Char, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        lastIndexOfAny(charArrayOf(char), startIndex, ignoreCase)
    else
        nativeLastIndexOf(char, startIndex)
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified [string],
 * starting from the specified [startIndex].
 *
 * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
 * @param ignoreCase `true` to ignore character case when matching a string. By default `false`.
 * @return An index of the last occurrence of [string] or -1 if none is found.
 * @sample samples.text.Strings.lastIndexOf
 */
public fun CharSequence.lastIndexOf(string: String, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int {
    return if (ignoreCase || this !is String)
        indexOf(string, startIndex, 0, ignoreCase, last = true)
    else
        nativeLastIndexOf(string, startIndex)
}

/**
 * Returns `true` if this char sequence contains the specified [other] sequence of characters as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 * @sample samples.text.Strings.contains
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
public operator fun CharSequence.contains(other: CharSequence, ignoreCase: Boolean = false): Boolean =
    if (other is String)
        indexOf(other, ignoreCase = ignoreCase) >= 0
    else
        indexOf(other, 0, length, ignoreCase) >= 0



/**
 * Returns `true` if this char sequence contains the specified character [char].
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 */
@Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
public operator fun CharSequence.contains(char: Char, ignoreCase: Boolean = false): Boolean =
    indexOf(char, ignoreCase = ignoreCase) >= 0

/**
 * Returns `true` if this char sequence contains at least one match of the specified regular expression [regex].
 */
@kotlin.internal.InlineOnly
public inline operator fun CharSequence.contains(regex: Regex): Boolean = regex.containsMatchIn(this)


// rangesDelimitedBy


private class DelimitedRangesSequence(
    private val input: CharSequence,
    private val startIndex: Int,
    private val limit: Int,
    private val getNextMatch: CharSequence.(currentIndex: Int) -> Pair<Int, Int>?
) : Sequence<IntRange> {

    override fun iterator(): Iterator<IntRange> = object : Iterator<IntRange> {
        var nextState: Int = -1 // -1 for unknown, 0 for done, 1 for continue
        var currentStartIndex: Int = startIndex.coerceIn(0, input.length)
        var nextSearchIndex: Int = currentStartIndex
        var nextItem: IntRange? = null
        var counter: Int = 0

        private fun calcNext() {
            if (nextSearchIndex < 0) {
                nextState = 0
                nextItem = null
            } else {
                if (limit > 0 && ++counter >= limit || nextSearchIndex > input.length) {
                    nextItem = currentStartIndex..input.lastIndex
                    nextSearchIndex = -1
                } else {
                    val match = input.getNextMatch(nextSearchIndex)
                    if (match == null) {
                        nextItem = currentStartIndex..input.lastIndex
                        nextSearchIndex = -1
                    } else {
                        val (index, length) = match
                        nextItem = currentStartIndex until index
                        currentStartIndex = index + length
                        nextSearchIndex = currentStartIndex + if (length == 0) 1 else 0
                    }
                }
                nextState = 1
            }
        }

        override fun next(): IntRange {
            if (nextState == -1)
                calcNext()
            if (nextState == 0)
                throw NoSuchElementException()
            val result = nextItem as IntRange
            // Clean next to avoid keeping reference on yielded instance
            nextItem = null
            nextState = -1
            return result
        }

        override fun hasNext(): Boolean {
            if (nextState == -1)
                calcNext()
            return nextState == 1
        }
    }
}

/**
 * Iterates over [string] lines. Lines could be separated by either of `\n`, `\r`, `\r\n`.
 * If the [string] ends with a line separator, this iterator will return an extra empty line.
 */
private class LinesIterator(private val string: CharSequence) : Iterator<String> {
    private companion object State {
        const val UNKNOWN = 0
        const val HAS_NEXT = 1
        const val EXHAUSTED = 2
    }

    private var state: Int = UNKNOWN
    private var tokenStartIndex: Int = 0
    private var delimiterStartIndex: Int = 0
    private var delimiterLength: Int = 0 // serves as both a delimiter length and an end-of-input marker (with value < 0)

    override fun hasNext(): Boolean {
        if (state != UNKNOWN) {
            return state == HAS_NEXT
        }

        if (delimiterLength < 0) {
            state = EXHAUSTED
            return false
        }

        var _delimiterLength = -1
        var _delimiterStartIndex = string.length

        for (idx in tokenStartIndex..<string.length) {
            val c = string[idx]
            if (c == '\n' || c == '\r') {
                // If current character is `\n` then it's the only separator character,
                // but for '\r' there are two options: the line ends either with `\r`, or with `\r\n`.
                _delimiterLength = if (c == '\r' && idx + 1 < string.length && string[idx + 1] == '\n') 2 else 1
                _delimiterStartIndex = idx
                break
            }
        }

        // Update fields after the main loop to avoid inconsistent iterator state in case of an exception.
        state = HAS_NEXT
        delimiterLength = _delimiterLength
        delimiterStartIndex = _delimiterStartIndex

        return true
    }

    override fun next(): String {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        state = UNKNOWN
        val lastIndex = delimiterStartIndex
        val firstIndex = tokenStartIndex
        tokenStartIndex = delimiterStartIndex + delimiterLength
        return string.substring(firstIndex, lastIndex)
    }
}

/**
 * Returns a sequence of index ranges of substrings in this char sequence around occurrences of the specified [delimiters].
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 */
private fun CharSequence.rangesDelimitedBy(delimiters: CharArray, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    requireNonNegativeLimit(limit)

    return DelimitedRangesSequence(this, startIndex, limit, { currentIndex ->
        indexOfAny(delimiters, currentIndex, ignoreCase = ignoreCase).let { if (it < 0) null else it to 1 }
    })
}

/**
 * Returns a sequence of index ranges of substrings in this char sequence around occurrences of the specified [delimiters].
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param startIndex The index to start searching delimiters from.
 *  No range having its start value less than [startIndex] is returned.
 *  [startIndex] is coerced to be non-negative and not greater than length of this string.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 */
private fun CharSequence.rangesDelimitedBy(delimiters: Array<out String>, startIndex: Int = 0, ignoreCase: Boolean = false, limit: Int = 0): Sequence<IntRange> {
    requireNonNegativeLimit(limit)
    val delimitersList = delimiters.asList()

    return DelimitedRangesSequence(this, startIndex, limit, { currentIndex -> findAnyOf(delimitersList, currentIndex, ignoreCase = ignoreCase, last = false)?.let { it.first to it.second.length } })

}

internal fun requireNonNegativeLimit(limit: Int) =
    require(limit >= 0) { "Limit must be non-negative, but was $limit" }


// split

/**
 * Splits this char sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * The last element of the resulting sequence corresponds to a subsequence starting right after the last
 * delimiter occurrence (or at the beginning of this char sequence if there were no such occurrences)
 * and ending at the end of this char sequence. That implies that if this char sequence does not
 * contain [delimiters], the resulting sequence will contain a single element corresponding to
 * the whole char sequence. It also implies that for char sequences ending with one of [delimiters],
 * the resulting sequence will end with an empty string.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and finds at each position the first element in [delimiters]
 * that matches this string at that position.
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * @sample samples.text.Strings.splitToSequenceWithStringDelimiters
 */
public fun CharSequence.splitToSequence(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
    rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).map { substring(it) }

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiters].
 *
 * The last element of the resulting list corresponds to a subsequence starting right after the last
 * delimiter occurrence (or at the beginning of this char sequence if there were no such occurrences)
 * and ending at the end of this char sequence. That implies that if this char sequence does not
 * contain [delimiters], the resulting list will contain a single element corresponding to
 * the whole char sequence. It also implies that for char sequences ending with one of [delimiters],
 * the resulting list will end with an empty string.
 *
 * To avoid ambiguous results when strings in [delimiters] have characters in common, this method proceeds from
 * the beginning to the end of this string, and matches at each position the first element in [delimiters]
 * that is equal to a delimiter in this instance at that position.
 *
 * @param delimiters One or more strings to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return. Zero by default means no limit is set.
 *
 * @sample samples.text.Strings.splitWithStringDelimiters
 */
public fun CharSequence.split(vararg delimiters: String, ignoreCase: Boolean = false, limit: Int = 0): List<String> {
    if (delimiters.size == 1) {
        val delimiter = delimiters[0]
        if (!delimiter.isEmpty()) {
            return split(delimiter, ignoreCase, limit)
        }
    }

    return rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).asIterable().map { substring(it) }
}

/**
 * Splits this char sequence to a sequence of strings around occurrences of the specified [delimiters].
 *
 * The last element of the resulting sequence corresponds to a subsequence starting right after the last
 * delimiter occurrence (or at the beginning of this char sequence if there were no such occurrences)
 * and ending at the end of this char sequence. That implies that if this char sequence does not
 * contain [delimiters], the resulting sequence will contain a single element corresponding to
 * the whole char sequence. It also implies that for char sequences ending with one of [delimiters],
 * the resulting sequence will end with an empty string.
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 *
 * @sample samples.text.Strings.splitToSequenceWithCharDelimiters
 */
public fun CharSequence.splitToSequence(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): Sequence<String> =
    rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).map { substring(it) }

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiters].
 *
 * The last element of the resulting list corresponds to a subsequence starting right after the last
 * delimiter occurrence (or at the beginning of this char sequence if there were no such occurrences)
 * and ending at the end of this char sequence. That implies that if this char sequence does not
 * contain [delimiters], the resulting list will contain a single element corresponding to
 * the whole char sequence. It also implies that for char sequences ending with one of [delimiters],
 * the resulting list will end with an empty string.
 *
 * @param delimiters One or more characters to be used as delimiters.
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 * @sample samples.text.Strings.splitWithCharDelimiters
 */
public fun CharSequence.split(vararg delimiters: Char, ignoreCase: Boolean = false, limit: Int = 0): List<String> {
    if (delimiters.size == 1) {
        return split(delimiters[0].toString(), ignoreCase, limit)
    }

    return rangesDelimitedBy(delimiters, ignoreCase = ignoreCase, limit = limit).asIterable().map { substring(it) }
}

/**
 * Splits this char sequence to a list of strings around occurrences of the specified [delimiter].
 * This is specialized version of split which receives single non-empty delimiter and offers better performance
 *
 * The last element of the resulting list corresponds to a subsequence starting right after the last
 * delimiter occurrence (or at the beginning of this char sequence if there were no such occurrences)
 * and ending at the end of this char sequence. That implies that if this char sequence does not
 * contain [delimiter], the resulting list will contain a single element corresponding to
 * the whole char sequence. It also implies that for char sequences ending with [delimiter],
 * the resulting list will end with an empty string.
 *
 * @param delimiter String used as delimiter
 * @param ignoreCase `true` to ignore character case when matching a delimiter. By default `false`.
 * @param limit The maximum number of substrings to return.
 */
private fun CharSequence.split(delimiter: String, ignoreCase: Boolean, limit: Int): List<String> {
    requireNonNegativeLimit(limit)

    var currentOffset = 0
    var nextIndex = indexOf(delimiter, currentOffset, ignoreCase)
    if (nextIndex == -1 || limit == 1) {
        return listOf(this.toString())
    }

    val isLimited = limit > 0
    val result = ArrayList<String>(if (isLimited) limit.coerceAtMost(10) else 10)
    do {
        result.add(substring(currentOffset, nextIndex))
        currentOffset = nextIndex + delimiter.length
        // Do not search for next occurrence if we're reaching limit
        if (isLimited && result.size == limit - 1) break
        nextIndex = indexOf(delimiter, currentOffset, ignoreCase)
    } while (nextIndex != -1)

    result.add(substring(currentOffset, length))
    return result
}

/**
 * Splits this char sequence to a list of strings around matches of the given regular expression.
 *
 * The last element of the resulting list corresponds to a subsequence starting right after the last
 * [regex] match (or at the beginning of this char sequence if there were no matches)
 * and ending at the end of this char sequence. That implies that if this char sequences does not
 * contain subsequences matching [regex], the resulting list will contain a single element
 * corresponding to the whole char sequence.
 * It also implies that for char sequences ending with a [regex] match,
 * the resulting list will end with an empty string.
 *
 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 * @sample samples.text.Strings.splitWithRegex
 */
@kotlin.internal.InlineOnly
public inline fun CharSequence.split(regex: Regex, limit: Int = 0): List<String> = regex.split(this, limit)

/**
 * Splits this char sequence to a sequence of strings around matches of the given regular expression.
 *
 * The last element of the resulting sequence corresponds to a subsequence starting right after the last
 * [regex] match (or at the beginning of this char sequence if there were no matches)
 * and ending at the end of this char sequence. That implies that if this char sequences does not
 * contain subsequences matching [regex], the resulting sequence will contain a single element
 * corresponding to the whole char sequence.
 * It also implies that for char sequences ending with a [regex] match,
 * the resulting sequence will end with an empty string.
 *
 * @param limit Non-negative value specifying the maximum number of substrings to return.
 * Zero by default means no limit is set.
 * @sample samples.text.Strings.splitToSequence
 */
@SinceKotlin("1.6")
@kotlin.internal.InlineOnly
public inline fun CharSequence.splitToSequence(regex: Regex, limit: Int = 0): Sequence<String> = regex.splitToSequence(this, limit)

/**
 * Splits this char sequence to a sequence of lines delimited by any of the following character sequences: CRLF, LF or CR.
 *
 * The lines returned do not include terminating line separators.
 */
public fun CharSequence.lineSequence(): Sequence<String> = Sequence { LinesIterator(this) }

/**
 * Splits this char sequence to a list of lines delimited by any of the following character sequences: CRLF, LF or CR.
 *
 * The lines returned do not include terminating line separators.
 */
public fun CharSequence.lines(): List<String> = lineSequence().toList()

/**
 * Returns `true` if the contents of this char sequence are equal to the contents of the specified [other],
 * i.e. both char sequences contain the same number of the same characters in the same order.
 *
 * @sample samples.text.Strings.contentEquals
 */
@SinceKotlin("1.5")
public expect infix fun CharSequence?.contentEquals(other: CharSequence?): Boolean

/**
 * Returns `true` if the contents of this char sequence are equal to the contents of the specified [other], optionally ignoring case difference.
 *
 * @param ignoreCase `true` to ignore character case when comparing contents.
 *
 * @sample samples.text.Strings.contentEquals
 */
@SinceKotlin("1.5")
public expect fun CharSequence?.contentEquals(other: CharSequence?, ignoreCase: Boolean): Boolean

internal fun CharSequence?.contentEqualsIgnoreCaseImpl(other: CharSequence?): Boolean {
    if (this is String && other is String) {
        return this.equals(other, ignoreCase = true)
    }

    if (this === other) return true
    if (this == null || other == null || this.length != other.length) return false

    for (i in 0 until length) {
        if (!this[i].equals(other[i], ignoreCase = true)) {
            return false
        }
    }

    return true
}

internal fun CharSequence?.contentEqualsImpl(other: CharSequence?): Boolean {
    if (this is String && other is String) {
        return this == other
    }

    if (this === other) return true
    if (this == null || other == null || this.length != other.length) return false

    for (i in 0 until length) {
        if (this[i] != other[i]) {
            return false
        }
    }

    return true
}

/**
 * Returns `true` if the content of this string is equal to the word "true", `false` if it is equal to "false",
 * and throws an exception otherwise.
 *
 * There is also a lenient version of the function available on nullable String, [String?.toBoolean].
 * Note that this function is case-sensitive.
 *
 * @sample samples.text.Strings.toBooleanStrict
 */
@SinceKotlin("1.5")
public fun String.toBooleanStrict(): Boolean = when (this) {
    "true" -> true
    "false" -> false
    else -> throw IllegalArgumentException("The string doesn't represent a boolean value: $this")
}

/**
 * Returns `true` if the content of this string is equal to the word "true", `false` if it is equal to "false",
 * and `null` otherwise.
 *
 * There is also a lenient version of the function available on nullable String, [String?.toBoolean].
 * Note that this function is case-sensitive.
 *
 * @sample samples.text.Strings.toBooleanStrictOrNull
 */
@SinceKotlin("1.5")
public fun String.toBooleanStrictOrNull(): Boolean? = when (this) {
    "true" -> true
    "false" -> false
    else -> null
}
