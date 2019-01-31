/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.Name

interface FirScope {
    fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean = true

    fun processFunctionsByName(
        name: Name,
        processor: (ConeFunctionSymbol) -> ProcessorAction
    ): ProcessorAction = NEXT

    fun processPropertiesByName(
        name: Name,
        processor: (ConePropertySymbol) -> ProcessorAction
    ): ProcessorAction = NEXT
}

fun FirScope.processCallablesByName(
    name: Name,
    processor: (ConeCallableSymbol) -> ProcessorAction
): ProcessorAction {
    if (!processFunctionsByName(name, processor)) return STOP
    return processPropertiesByName(name, processor)
}

enum class FirPosition(val allowTypeParameters: Boolean = true) {
    SUPER_TYPE_OR_EXPANSION(allowTypeParameters = false),
    OTHER
}

enum class ProcessorAction {
    STOP,
    NEXT;

    operator fun not(): Boolean {
        return when (this) {
            STOP -> true
            NEXT -> false
        }
    }

    fun next() = this == NEXT
}