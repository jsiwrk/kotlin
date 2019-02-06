/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.symbols.JavaSymbolFactory
import org.jetbrains.kotlin.fir.resolve.AbstractFirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class JavaSymbolProvider(
    session: FirSession,
    val project: Project,
    private val searchScope: GlobalSearchScope
) : AbstractFirSymbolProvider() {
    private val factory = JavaSymbolFactory(session)

    override fun getCallableSymbols(callableId: CallableId): List<ConeSymbol> {
        // TODO
        return emptyList()
    }

    override fun getSymbolByFqName(classId: ClassId): ConeSymbol? {
        return classCache.lookupCacheOrCalculate(classId) {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val foundClass = facade.findClass(JavaClassFinder.Request(classId), searchScope)
            foundClass?.let { javaClass -> factory.createClassSymbol(javaClass) }
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.lookupCacheOrCalculate(fqName) {
            val facade = KotlinJavaPsiFacade.getInstance(project)
            val javaPackage = facade.findPackage(fqName.asString(), searchScope) ?: return@lookupCacheOrCalculate null
            FqName(javaPackage.qualifiedName)
        }
    }
}

