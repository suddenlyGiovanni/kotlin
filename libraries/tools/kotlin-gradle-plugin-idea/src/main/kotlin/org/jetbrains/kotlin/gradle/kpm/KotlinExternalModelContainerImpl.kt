/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.WriteReplacedModel
import java.io.Serializable

@WriteReplacedModel(SerializedKotlinExternalModelContainerCarrier::class)
internal class KotlinMutableExternalModelContainerImpl private constructor(
    private val values: MutableMap<KotlinExternalModelKey<*>, Any>
) : KotlinMutableExternalModelContainer() {

    constructor() : this(mutableMapOf())

    override val ids: Set<KotlinExternalModelId<*>>
        @Synchronized get() = values.keys.map { it.id }.toSet()

    @Synchronized
    override fun <T : Any> set(key: KotlinExternalModelKey<T>, value: T) {
        values[key] = value
    }

    @Synchronized
    override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean {
        return key in values
    }

    @Synchronized
    @Suppress("unchecked_cast")
    override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? {
        return values[key]?.let { it as T }
    }

    override fun plus(other: KotlinExternalModelContainer): KotlinExternalModelContainer {
        return when (other) {
            is Empty -> this
            is KotlinMutableExternalModelContainerImpl -> KotlinMutableExternalModelContainerImpl(
                this.values.plus(other.values).toMutableMap()
            )
            is SerializedKotlinExternalModelContainer -> SerializedKotlinExternalModelContainer(
                serializedValues = other.serializedValues.filterKeys { key -> key !in ids }.toMutableMap(),
            ).also { container -> container.deserializedValues.putAll(this.values) }
        }
    }

    @Synchronized
    private fun writeReplace(): Any {
        return SerializedKotlinExternalModelContainerCarrier(serialize(values))
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

@WriteReplacedModel(SerializedKotlinExternalModelContainerCarrier::class)
private class SerializedKotlinExternalModelContainer(
    val serializedValues: MutableMap<KotlinExternalModelId<*>, ByteArray>
) : KotlinExternalModelContainer(), Serializable {

    val deserializedValues = mutableMapOf<KotlinExternalModelKey<*>, Any>()

    override val ids: Set<KotlinExternalModelId<*>>
        @Synchronized get() = serializedValues.keys + deserializedValues.keys.map { it.id }

    @Synchronized
    override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean {
        return key.id in serializedValues || key in deserializedValues
    }

    @Synchronized
    @Suppress("unchecked_cast")
    override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? {
        deserializedValues[key]?.let { return it as T }
        val serializedValue = serializedValues[key.id] ?: return null
        val deserializedValue = key.serializer?.deserialize(serializedValue) ?: return null
        deserializedValues[key] = deserializedValue
        serializedValues.remove(key.id)
        return deserializedValue
    }

    override fun plus(other: KotlinExternalModelContainer): KotlinExternalModelContainer {
        return when (other) {
            is Empty -> return this
            is KotlinMutableExternalModelContainerImpl -> return other + this
            is SerializedKotlinExternalModelContainer -> {
                val thisDeserializedIds = this.deserializedValues.keys.map { it.id }.toSet()
                val otherDeserializedIds = other.deserializedValues.keys.map { it.id }.toSet()
                val container = SerializedKotlinExternalModelContainer(
                    (serializedValues.filterKeys { it !in otherDeserializedIds } +
                            other.serializedValues.filterKeys { it !in thisDeserializedIds }).toMutableMap()
                )
                container.deserializedValues.putAll(other.deserializedValues)
                container
            }
        }
    }

    @Synchronized
    private fun writeReplace(): Any {
        return SerializedKotlinExternalModelContainerCarrier(serializedValues + serialize(deserializedValues))
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

private class SerializedKotlinExternalModelContainerCarrier(
    private val serializedValues: Map<KotlinExternalModelId<*>, ByteArray>
) : Serializable {

    private fun readResolve(): Any {
        return SerializedKotlinExternalModelContainer(serializedValues.toMutableMap())
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

private fun serialize(values: Map<KotlinExternalModelKey<*>, Any>): Map<KotlinExternalModelId<*>, ByteArray> {
    return values.filterKeys { it.serializer != null }
        .mapValues { (key, value) ->
            @Suppress("unchecked_cast")
            val serializer = checkNotNull(key.serializer) as KotlinExternalModelSerializer<Any>
            serializer.serialize(value)
        }.mapKeys { (key, _) -> key.id }
}
