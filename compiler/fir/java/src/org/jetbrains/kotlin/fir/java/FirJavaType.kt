/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.load.java.structure.JavaType

class FirJavaType(
    session: FirSession,
    psi: PsiElement?,
    val type: JavaType
    ) : FirType, FirAbstractAnnotatedElement(session, psi) {

}