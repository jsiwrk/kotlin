/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeAbbreviatedType
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.impl.ConeAbbreviatedTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl

abstract class FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority: Boolean) : FirAbstractTreeTransformer() {
    protected val towerScope = FirCompositeScope(mutableListOf(), reversedPriority = reversedScopePriority)

    protected inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = towerScope.scopes.size
        val result = l()
        val size = towerScope.scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            towerScope.scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    protected fun lookupSuperTypes(
        klass: FirRegularClass,
        lookupInterfaces: Boolean,
        deep: Boolean,
        useSiteSession: FirSession = klass.session
    ): List<ConeClassLikeType> {
        return mutableListOf<ConeClassLikeType>().also {
            if (lookupInterfaces) klass.symbol.collectSuperTypes(useSiteSession, it, deep)
            else klass.symbol.collectSuperClasses(useSiteSession, it)
        }
    }

    protected fun ConeClassLikeType.projection(useSiteSession: FirSession): ConeClassLikeType {
        if (this is ConeClassErrorType) return this
        val symbolProvider = FirSymbolProvider.getInstance(useSiteSession)
        val declaredSymbol = symbol
        if (declaredSymbol is FirBasedSymbol<*> && declaredSymbol.fir.session.moduleInfo == useSiteSession.moduleInfo) {
            return this
        }
        val useSiteSymbol = symbolProvider.getSymbolByFqName(declaredSymbol.classId)
        return if (useSiteSymbol !is ConeClassLikeSymbol || useSiteSymbol == declaredSymbol) {
            this
        } else when (this) {
            is ConeClassTypeImpl ->
                ConeClassTypeImpl(useSiteSymbol, typeArguments)
            is ConeAbbreviatedTypeImpl ->
                ConeAbbreviatedTypeImpl(useSiteSymbol, typeArguments, directExpansion)
            else ->
                this
        }
    }

    private tailrec fun ConeClassLikeType.computePartialExpansion(): ConeClassLikeType? {
        return when (this) {
            is ConeAbbreviatedType -> directExpansion.takeIf { it !is ConeClassErrorType }?.computePartialExpansion()
            else -> return this
        }
    }

    private tailrec fun ConeClassLikeSymbol.collectSuperClasses(useSiteSession: FirSession, list: MutableList<ConeClassLikeType>) {
        when (this) {
            is ConeClassSymbol -> {
                val superClassType =
                    this.superTypes
                        .map { it.projection(useSiteSession).computePartialExpansion() }
                        .firstOrNull {
                            it !is ConeClassErrorType && (it?.symbol as? ConeClassSymbol)?.kind == ClassKind.CLASS
                        } ?: return
                list += superClassType
                superClassType.symbol.collectSuperClasses(useSiteSession, list)
            }
            is ConeTypeAliasSymbol -> {
                val expansion = expansionType?.projection(useSiteSession)?.computePartialExpansion() ?: return
                expansion.symbol.collectSuperClasses(useSiteSession, list)
            }
            else -> error("?!id:1")
        }
    }

    private fun ConeClassLikeSymbol.collectSuperTypes(useSiteSession: FirSession, list: MutableList<ConeClassLikeType>, deep: Boolean) {
        when (this) {
            is ConeClassSymbol -> {
                val superClassTypes =
                    this.superTypes.mapNotNull { it.projection(useSiteSession).computePartialExpansion() }
                list += superClassTypes
                if (deep)
                    superClassTypes.forEach {
                        if (it !is ConeClassErrorType) {
                            it.symbol.collectSuperTypes(useSiteSession, list, deep)
                        }
                    }
            }
            is ConeTypeAliasSymbol -> {
                val expansion = expansionType?.projection(useSiteSession)?.computePartialExpansion() ?: return
                expansion.symbol.collectSuperTypes(useSiteSession, list, deep)
            }
            else -> error("?!id:1")
        }
    }
}