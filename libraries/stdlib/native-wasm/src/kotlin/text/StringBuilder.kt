/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

/**
 * A mutable sequence of characters.
 *
 * String builder can be used to efficiently perform multiple string manipulation operations.
 */
public actual class StringBuilder
private constructor (private var array: CharArray) : CharSequence, Appendable {

    /** Constructs an empty string builder. */
    public actual constructor() : this(10)

    /** Constructs an empty string builder with the specified initial [capacity]. */
    public actual constructor(capacity: Int) : this(CharArray(capacity))

    /** Constructs a string builder that contains the same characters as the specified [content] string. */
    public actual constructor(content: String) : this(content.toCharArray()) {
        _length = array.size
    }

    /** Constructs a string builder that contains the same characters as the specified [content] char sequence. */
    public actual constructor(content: CharSequence): this(content.length) {
        append(content)
    }

    // Of CharSequence.
    private var _length: Int = 0

    actual override val length: Int
        get() = _length

    actual override fun get(index: Int): Char {
        AbstractList.checkElementIndex(index, _length)
        return array[index]
    }

    actual override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = substring(startIndex, endIndex)

    // Of Appenable.
    @IgnorableReturnValue
    actual override fun append(value: Char) : StringBuilder {
        ensureExtraCapacity(1)
        array[_length++] = value
        return this
    }

    @IgnorableReturnValue
    actual override fun append(value: CharSequence?): StringBuilder {
        // Kotlin/JVM processes null as if the argument was "null" char sequence.
        val toAppend = value ?: "null"
        return append(toAppend, 0, toAppend.length)
    }

    @IgnorableReturnValue
    actual override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): StringBuilder =
            this.appendRange(value ?: "null", startIndex, endIndex)

    /**
     * Reverses the contents of this string builder and returns this instance.
     *
     * Surrogate pairs included in this string builder are treated as single characters.
     * Therefore, the order of the high-low surrogates is never reversed.
     *
     * Note that the reverse operation may produce new surrogate pairs that were unpaired low-surrogates and high-surrogates before the operation.
     * For example, reversing `"\uDC00\uD800"` produces `"\uD800\uDC00"` which is a valid surrogate pair.
     */
    // Based on Apache Harmony implementation.
    @IgnorableReturnValue
    public actual fun reverse(): StringBuilder {
        if (this.length < 2) {
            return this
        }
        var end = _length - 1
        var front = 0
        var frontLeadingChar = array[0]
        var endTrailingChar = array[end]
        var allowFrontSurrogate = true
        var allowEndSurrogate = true
        while (front < _length / 2) {

            var frontTrailingChar = array[front + 1]
            var endLeadingChar = array[end - 1]
            var surrogateAtFront = allowFrontSurrogate && frontTrailingChar.isLowSurrogate() && frontLeadingChar.isHighSurrogate()
            if (surrogateAtFront && _length < 3) {
                return this
            }
            var surrogateAtEnd = allowEndSurrogate && endTrailingChar.isLowSurrogate() && endLeadingChar.isHighSurrogate()
            allowFrontSurrogate = true
            allowEndSurrogate = true
            when {
                surrogateAtFront && surrogateAtEnd -> {
                    // Both surrogates - just exchange them.
                    array[end] = frontTrailingChar
                    array[end - 1] = frontLeadingChar
                    array[front] = endLeadingChar
                    array[front + 1] = endTrailingChar
                    frontLeadingChar = array[front + 2]
                    endTrailingChar = array[end - 2]
                    front++
                    end--
                }
                !surrogateAtFront && !surrogateAtEnd -> {
                    // Neither surrogates - exchange only front/end.
                    array[end] = frontLeadingChar
                    array[front] = endTrailingChar
                    frontLeadingChar = frontTrailingChar
                    endTrailingChar = endLeadingChar
                }
                surrogateAtFront && !surrogateAtEnd -> {
                    // Surrogate only at the front -
                    // move the low part, the high part will be moved as a usual character on the next iteration.
                    array[end] = frontTrailingChar
                    array[front] = endTrailingChar
                    endTrailingChar = endLeadingChar
                    allowFrontSurrogate = false
                }
                !surrogateAtFront && surrogateAtEnd -> {
                    // Surrogate only at the end -
                    // move the high part, the low part will be moved as a usual character on the next iteration.
                    array[end] = frontLeadingChar
                    array[front] = endLeadingChar
                    frontLeadingChar = frontTrailingChar
                    allowEndSurrogate = false
                }
            }
            front++
            end--
        }
        if (_length % 2 == 1 && (!allowEndSurrogate || !allowFrontSurrogate)) {
            array[end] = if (allowFrontSurrogate) endTrailingChar else frontLeadingChar
        }
        return this
    }

    /**
     * Appends the string representation of the specified object [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Any?): StringBuilder = append(value.toString())

    // TODO: optimize the append overloads with primitive value!

    /**
     * Appends the string representation of the specified boolean [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Boolean): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public fun append(value: Byte): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified short [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    public fun append(value: Short): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified int [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Int): StringBuilder {
        ensureExtraCapacity(11)
        _length += insertInt(array, _length, value)
        return this
    }

    /**
     * Appends the string representation of the specified long [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Long): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified float [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Float): StringBuilder = append(value.toString())

    /**
     * Appends the string representation of the specified double [value] to this string builder and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was appended to this string builder.
     */
    @IgnorableReturnValue
    public actual fun append(value: Double): StringBuilder = append(value.toString())

    /**
     * Appends characters in the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at the index 0.
     */
    @IgnorableReturnValue
    public actual fun append(value: CharArray): StringBuilder {
        ensureExtraCapacity(value.size)
        value.copyInto(array, _length)
        _length += value.size
        return this
    }

    /**
     * Appends the specified string [value] to this string builder and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are appended.
     */
    @IgnorableReturnValue
    public actual fun append(value: String?): StringBuilder {
        val toAppend = value ?: "null"
        ensureExtraCapacity(toAppend.length)
        _length += insertString(array, _length, toAppend)
        return this
    }

    /**
     * Returns the current capacity of this string builder.
     *
     * The capacity is the maximum length this string builder can have before an allocation occurs.
     */
    public actual fun capacity(): Int = array.size

    /**
     * Ensures that the capacity of this string builder is at least equal to the specified [minimumCapacity].
     *
     * If the current capacity is less than the [minimumCapacity], a new backing storage is allocated with greater capacity.
     * Otherwise, this method takes no action and simply returns.
     */
    public actual fun ensureCapacity(minimumCapacity: Int) {
        if (minimumCapacity <= array.size) return
        ensureCapacityInternal(minimumCapacity)
    }

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun indexOf(string: String): Int {
        return (this as CharSequence).indexOf(string, startIndex = 0, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the first occurrence of the specified [string],
     * starting at the specified [startIndex].
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    public actual fun indexOf(string: String, startIndex: Int): Int {
        if (string.isEmpty() && startIndex >= _length) return _length
        return (this as CharSequence).indexOf(string, startIndex, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string].
     * The last occurrence of empty string `""` is considered to be at the index equal to `this.length`.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun lastIndexOf(string: String): Int {
        if (string.isEmpty()) return _length
        return (this as CharSequence).lastIndexOf(string, startIndex = lastIndex, ignoreCase = false)
    }

    /**
     * Returns the index within this string builder of the last occurrence of the specified [string],
     * starting from the specified [startIndex] toward the beginning.
     *
     * Returns `-1` if the specified [string] does not occur in this string builder starting at the specified [startIndex].
     */
    @SinceKotlin("1.4")
    public actual fun lastIndexOf(string: String, startIndex: Int): Int {
        if (string.isEmpty() && startIndex >= _length) return _length
        return (this as CharSequence).lastIndexOf(string, startIndex, ignoreCase = false)
    }

    // TODO: optimize the insert overloads with primitive value!

    /**
     * Inserts the string representation of the specified boolean [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Boolean): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public fun insert(index: Int, value: Byte): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    public fun insert(index: Int, value: Short): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified int [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Int): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified long [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Long): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified float [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Float): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string representation of the specified double [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Double): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the specified character [value] into this string builder at the specified [index] and returns this instance.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Char): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        ensureExtraCapacity(1)
        val newLastIndex = lastIndex + 1
        for (i in newLastIndex downTo index + 1) {
            array[i] = array[i - 1]
        }
        array[index] = value
        _length++
        return this
    }

    /**
     * Inserts characters in the specified character array [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in same order as in the [value] character array, starting at [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: CharArray): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        ensureExtraCapacity(value.size)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + value.size)
        value.copyInto(array, destinationOffset = index)

        _length += value.size
        return this
    }

    /**
     * Inserts characters in the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the character sequence from which characters are inserted. If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: CharSequence?): StringBuilder {
        // Kotlin/JVM inserts the "null" string if the argument is null.
        val toInsert = value ?: "null"
        return insertRange(index, toInsert, 0, toInsert.length)
    }

    /**
     * Inserts the string representation of the specified object [value] into this string builder at the specified [index] and returns this instance.
     *
     * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
     * and then that string was inserted into this string builder at the specified [index].
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: Any?): StringBuilder = insert(index, value.toString())

    /**
     * Inserts the string [value] into this string builder at the specified [index] and returns this instance.
     *
     * If [value] is `null`, then the four characters `"null"` are inserted.
     *
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @IgnorableReturnValue
    public actual fun insert(index: Int, value: String?): StringBuilder {
        val toInsert = value ?: "null"
        AbstractList.checkPositionIndex(index, _length)
        ensureExtraCapacity(toInsert.length)
        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + toInsert.length)
        _length += insertString(array, index, toInsert)
        return this
    }

    /**
     *  Sets the length of this string builder to the specified [newLength].
     *
     *  If the [newLength] is less than the current length, it is changed to the specified [newLength].
     *  Otherwise, null characters '\u0000' are appended to this string builder until its length is less than the [newLength].
     *
     *  Note that in Kotlin/JS [set] operator function has non-constant execution time complexity.
     *  Therefore, increasing length of this string builder and then updating each character by index may slow down your program.
     *
     *  @throws IndexOutOfBoundsException or [IllegalArgumentException] if [newLength] is less than zero.
     */
    public actual fun setLength(newLength: Int) {
        if (newLength < 0) {
            throw IllegalArgumentException("Negative new length: $newLength.")
        }

        if (newLength > _length) {
            array.fill('\u0000', _length, newLength.coerceAtMost(array.size))
        }
        ensureCapacity(newLength)
        _length = newLength
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [endIndex] (exclusive).
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    public actual fun substring(startIndex: Int, endIndex: Int): String {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, _length)
        return unsafeStringFromCharArray(array, startIndex, endIndex - startIndex)
    }

    /**
     * Returns a new [String] that contains characters in this string builder at [startIndex] (inclusive) and up to the [length] (exclusive).
     *
     * @throws IndexOutOfBoundsException if [startIndex] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public actual fun substring(startIndex: Int): String {
        return substring(startIndex, _length)
    }

    /**
     * Attempts to reduce storage used for this string builder.
     *
     * If the backing storage of this string builder is larger than necessary to hold its current contents,
     * then it may be resized to become more space efficient.
     * Calling this method may, but is not required to, affect the value of the [capacity] property.
     */
    public actual fun trimToSize() {
        if (_length < array.size)
            array = array.copyOf(_length)
    }

    override fun toString(): String = unsafeStringFromCharArray(array, 0, _length)

    /**
     * Sets the character at the specified [index] to the specified [value].
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
     */
    public operator fun set(index: Int, value: Char) {
        AbstractList.checkElementIndex(index, _length)
        array[index] = value
    }

    /**
     * Replaces characters in the specified range of this string builder with characters in the specified string [value] and returns this instance.
     *
     * @param startIndex the beginning (inclusive) of the range to replace.
     * @param endIndex the end (exclusive) of the range to replace.
     * @param value the string to replace with.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)

        val coercedEndIndex = endIndex.coerceAtMost(_length)
        val lengthDiff = value.length - (coercedEndIndex - startIndex)
        ensureExtraCapacity(lengthDiff)
        array.copyInto(array, startIndex = coercedEndIndex, endIndex = _length, destinationOffset = startIndex + value.length)
        var replaceIndex = startIndex
        for (index in 0 until value.length) array[replaceIndex++] = value[index] // optimize
        _length += lengthDiff

        return this
    }

    /**
     * Removes the character at the specified [index] from this string builder and returns this instance.
     *
     * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
     *
     * @param index the index of `Char` to remove.
     *
     * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
     */
    @SinceKotlin("1.4")
    public fun deleteAt(index: Int): StringBuilder {
        AbstractList.checkElementIndex(index, _length)
        array.copyInto(array, startIndex = index + 1, endIndex = _length, destinationOffset = index)
        --_length
        return this
    }

    /**
     * Removes characters in the specified range from this string builder and returns this instance.
     *
     * @param startIndex the beginning (inclusive) of the range to remove.
     * @param endIndex the end (exclusive) of the range to remove.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun deleteRange(startIndex: Int, endIndex: Int): StringBuilder {
        checkReplaceRange(startIndex, endIndex, _length)

        val coercedEndIndex = endIndex.coerceAtMost(_length)
        array.copyInto(array, startIndex = coercedEndIndex, endIndex = _length, destinationOffset = startIndex)
        _length -= coercedEndIndex - startIndex
        return this
    }

    /**
     * Copies characters from this string builder into the [destination] character array.
     *
     * @param destination the array to copy to.
     * @param destinationOffset the position in the array to copy to, 0 by default.
     * @param startIndex the beginning (inclusive) of the range to copy, 0 by default.
     * @param endIndex the end (exclusive) of the range to copy, length of this string builder by default.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
     *  or when that index is out of the [destination] array indices range.
     */
    @SinceKotlin("1.4")
    public fun toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length) {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, _length)
        AbstractList.checkBoundsIndexes(destinationOffset, destinationOffset + endIndex - startIndex, destination.size)

        array.copyInto(destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Appends characters in a subarray of the specified character array [value] to this string builder and returns this instance.
     *
     * Characters are appended in order, starting at specified [startIndex].
     *
     * @param value the array from which characters are appended.
     * @param startIndex the beginning (inclusive) of the subarray to append.
     * @param endIndex the end (exclusive) of the subarray to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.size)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        value.copyInto(array, _length, startIndex, endIndex)
        _length += extraLength
        return this
    }

    /**
     * Appends a subsequence of the specified character sequence [value] to this string builder and returns this instance.
     *
     * @param value the character sequence from which a subsequence is appended.
     * @param startIndex the beginning (inclusive) of the subsequence to append.
     * @param endIndex the end (exclusive) of the subsequence to append.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
     */
    @SinceKotlin("1.4")
    public fun appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.length)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        (value as? String)?.let {
            _length += insertString(array, _length, it, startIndex, extraLength)
            return this
        }
        var index = startIndex
        while (index < endIndex)
            array[_length++] = value[index++]
        return this
    }

    /**
     * Inserts characters in a subsequence of the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the character sequence from which a subsequence is inserted.
     * @param startIndex the beginning (inclusive) of the subsequence to insert.
     * @param endIndex the end (exclusive) of the subsequence to insert.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public fun insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.length)
        AbstractList.checkPositionIndex(index, _length)
        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)

        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + extraLength)
        var from = startIndex
        var to = index
        while (from < endIndex) {
            array[to++] = value[from++]
        }

        _length += extraLength
        return this
    }

    /**
     * Inserts characters in a subarray of the specified character array [value] into this string builder at the specified [index] and returns this instance.
     *
     * The inserted characters go in same order as in the [value] array, starting at [index].
     *
     * @param index the position in this string builder to insert at.
     * @param value the array from which characters are inserted.
     * @param startIndex the beginning (inclusive) of the subarray to insert.
     * @param endIndex the end (exclusive) of the subarray to insert.
     *
     * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
     * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
     */
    @SinceKotlin("1.4")
    public fun insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder {
        AbstractList.checkPositionIndex(index, _length)
        AbstractList.checkBoundsIndexes(startIndex, endIndex, value.size)

        val extraLength = endIndex - startIndex
        ensureExtraCapacity(extraLength)
        array.copyInto(array, startIndex = index, endIndex = _length, destinationOffset = index + extraLength)
        value.copyInto(array, startIndex = startIndex, endIndex = endIndex, destinationOffset = index)

        _length += extraLength
        return this
    }

    // ---------------------------- private ----------------------------

    private fun ensureExtraCapacity(n: Int) {
        ensureCapacityInternal(_length + n)
    }

    private fun ensureCapacityInternal(minCapacity: Int) {
        if (minCapacity < 0) throw OutOfMemoryError()    // overflow
        if (minCapacity > array.size) {
            val newSize = AbstractList.newCapacity(array.size, minCapacity)
            array = array.copyOf(newSize)
        }
    }

    private fun checkReplaceRange(startIndex: Int, endIndex: Int, length: Int) {
        if (startIndex < 0 || startIndex > length) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, length: $length")
        }
        if (startIndex > endIndex) {
            throw IllegalArgumentException("startIndex($startIndex) > endIndex($endIndex)")
        }
    }
}


/**
 * Appends the string representation of the specified byte [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.append(value: Byte): StringBuilder = this.append(value)

/**
 * Appends the string representation of the specified short [value] to this string builder and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was appended to this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.append(value: Short): StringBuilder = this.append(value)

/**
 * Inserts the string representation of the specified byte [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insert(index: Int, value: Byte): StringBuilder = this.insert(index, value)

/**
 * Inserts the string representation of the specified short [value] into this string builder at the specified [index] and returns this instance.
 *
 * The overall effect is exactly as if the [value] were converted to a string by the `value.toString()` method,
 * and then that string was inserted into this string builder at the specified [index].
 *
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.9")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insert(index: Int, value: Short): StringBuilder = this.insert(index, value)

/**
 * Clears the content of this string builder making it empty and returns this instance.
 *
 * @sample samples.text.Strings.clearStringBuilder
 */
@SinceKotlin("1.3")
@IgnorableReturnValue
public actual fun StringBuilder.clear(): StringBuilder = apply { setLength(0) }

/**
 * Sets the character at the specified [index] to the specified [value].
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@kotlin.internal.InlineOnly
public actual inline operator fun StringBuilder.set(index: Int, value: Char): Unit = this.set(index, value)

/**
 * Replaces characters in the specified range of this string builder with characters in the specified string [value] and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to replace.
 * @param endIndex the end (exclusive) of the range to replace.
 * @param value the string to replace with.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] if [startIndex] is less than zero, greater than the length of this string builder, or `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.setRange(startIndex: Int, endIndex: Int, value: String): StringBuilder =
        this.setRange(startIndex, endIndex, value)

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.deleteAt(index: Int): StringBuilder = this.deleteAt(index)

/**
 * Removes characters in the specified range from this string builder and returns this instance.
 *
 * @param startIndex the beginning (inclusive) of the range to remove.
 * @param endIndex the end (exclusive) of the range to remove.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.deleteRange(startIndex: Int, endIndex: Int): StringBuilder = this.deleteRange(startIndex, endIndex)

/**
 * Copies characters from this string builder into the [destination] character array.
 *
 * @param destination the array to copy to.
 * @param destinationOffset the position in the array to copy to, 0 by default.
 * @param startIndex the beginning (inclusive) of the range to copy, 0 by default.
 * @param endIndex the end (exclusive) of the range to copy, length of this string builder by default.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this string builder indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
 *  or when that index is out of the [destination] array indices range.
 */
@SinceKotlin("1.4")
@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
@kotlin.internal.InlineOnly
public actual inline fun StringBuilder.toCharArray(destination: CharArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = this.length): Unit =
        this.toCharArray(destination, destinationOffset, startIndex, endIndex)

/**
 * Appends characters in a subarray of the specified character array [value] to this string builder and returns this instance.
 *
 * Characters are appended in order, starting at specified [startIndex].
 *
 * @param value the array from which characters are appended.
 * @param startIndex the beginning (inclusive) of the subarray to append.
 * @param endIndex the end (exclusive) of the subarray to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendRange(value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
        this.appendRange(value, startIndex, endIndex)

/**
 * Appends a subsequence of the specified character sequence [value] to this string builder and returns this instance.
 *
 * @param value the character sequence from which a subsequence is appended.
 * @param startIndex the beginning (inclusive) of the subsequence to append.
 * @param endIndex the end (exclusive) of the subsequence to append.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.appendRange(value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
        this.appendRange(value, startIndex, endIndex)

/**
 * Inserts characters in a subarray of the specified character array [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in same order as in the [value] array, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the array from which characters are inserted.
 * @param startIndex the beginning (inclusive) of the subarray to insert.
 * @param endIndex the end (exclusive) of the subarray to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] array indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insertRange(index: Int, value: CharArray, startIndex: Int, endIndex: Int): StringBuilder =
        this.insertRange(index, value, startIndex, endIndex)

/**
 * Inserts characters in a subsequence of the specified character sequence [value] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in the same order as in the [value] character sequence, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param value the character sequence from which a subsequence is inserted.
 * @param startIndex the beginning (inclusive) of the subsequence to insert.
 * @param endIndex the end (exclusive) of the subsequence to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of the [value] character sequence indices or when `startIndex > endIndex`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@SinceKotlin("1.4")
@kotlin.internal.InlineOnly
@IgnorableReturnValue
public actual inline fun StringBuilder.insertRange(index: Int, value: CharSequence, startIndex: Int, endIndex: Int): StringBuilder =
        this.insertRange(index, value, startIndex, endIndex)


internal fun insertString(array: CharArray, start: Int, value: String): Int =
        insertString(array, start, value, 0, value.length)

// Method renamings
/**
 * Inserts characters in a subsequence of the specified character sequence [csq] into this string builder at the specified [index] and returns this instance.
 *
 * The inserted characters go in the same order as in the [csq] character sequence, starting at [index].
 *
 * @param index the position in this string builder to insert at.
 * @param csq the character sequence from which a subsequence is inserted. If [csq] is `null`,
 *  then characters will be inserted as if [csq] contained the four characters `"null"`.
 * @param start the beginning (inclusive) of the subsequence to insert.
 * @param end the end (exclusive) of the subsequence to insert.
 *
 * @throws IndexOutOfBoundsException or [IllegalArgumentException] when [start] or [end] is out of range of the [csq] character sequence indices or when `start > end`.
 * @throws IndexOutOfBoundsException if [index] is less than zero or greater than the length of this string builder.
 */
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated(
        "Use insertRange(index: Int, csq: CharSequence, start: Int, end: Int) instead",
        ReplaceWith("insertRange(index, csq ?: \"null\", start, end)")
)
@kotlin.internal.InlineOnly
public inline fun StringBuilder.insert(index: Int, csq: CharSequence?, start: Int, end: Int): StringBuilder =
        this.insertRange(index, csq ?: "null", start, end)

@DeprecatedSinceKotlin(warningSince = "1.3", errorSince = "1.6")
@Deprecated("Use set(index: Int, value: Char) instead", ReplaceWith("set(index, value)"))
@kotlin.internal.InlineOnly
public inline fun StringBuilder.setCharAt(index: Int, value: Char): Unit = this.set(index, value)

/**
 * Removes the character at the specified [index] from this string builder and returns this instance.
 *
 * If the `Char` at the specified [index] is part of a supplementary code point, this method does not remove the entire supplementary character.
 *
 * @param index the index of `Char` to remove.
 *
 * @throws IndexOutOfBoundsException if [index] is out of bounds of this string builder.
 */
@DeprecatedSinceKotlin(warningSince = "1.4", errorSince = "1.6")
@Deprecated("Use deleteAt(index: Int) instead", ReplaceWith("deleteAt(index)"))
@kotlin.internal.InlineOnly
public inline fun StringBuilder.deleteCharAt(index: Int): StringBuilder = this.deleteAt(index)



internal expect fun unsafeStringFromCharArray(array: CharArray, start: Int, size: Int): String
internal expect fun insertInt(array: CharArray, start: Int, value: Int): Int
internal expect fun insertString(array: CharArray, destinationIndex: Int, value: String, sourceIndex: Int, count: Int): Int
