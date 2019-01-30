/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.inspections.gradle.findAll
import org.jetbrains.kotlin.idea.inspections.gradle.findKotlinPluginVersion
import org.jetbrains.kotlin.konan.library.info.LiteKonanLibraryInfoProvider
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData
import org.jetbrains.plugins.gradle.service.project.data.GradleExtensionsDataService

/**
 * Adds names for Kotlin/Native libraries imported as file dependencies from Gradle project.
 */
class KotlinGradleNativeProjectDataService : AbstractProjectDataService<ProjectData, Void>() {

    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun postProcess(
        toImport: MutableCollection<DataNode<ProjectData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        val projectNode = toImport.firstOrNull() ?: return
        val rootModuleNode = ExternalSystemApiUtil.find(projectNode, ProjectKeys.MODULE) ?: return

        val classpathData = rootModuleNode.findAll(BuildScriptClasspathData.KEY).firstOrNull()?.data ?: return
        val kotlinVersion = findKotlinPluginVersion(classpathData) ?: return

        val customKotlinNativeHome = ExternalSystemApiUtil.find(rootModuleNode, GradleExtensionsDataService.KEY)
            ?.data
            ?.gradleProperties
            ?.firstOrNull { it.name == KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY }
            ?.value
            ?.toString()

        val libraryInfoProvider = LiteKonanLibraryInfoProvider(customKotlinNativeHome)

        modelsProvider.allLibraries.forEach { library ->
            library.fixNameIfNeeded(modelsProvider, libraryInfoProvider, kotlinVersion)
        }

        modelsProvider.getModules(projectNode.data).forEach { module ->
            val rootModel = modelsProvider.getModifiableRootModel(module)
            val libraryTable = rootModel.moduleLibraryTable

            libraryTable.libraries.forEach { library ->
                library.fixNameIfNeeded(modelsProvider, libraryInfoProvider, kotlinVersion)
            }
        }
    }

    companion object {
        private const val KOTLIN_NATIVE_HOME_PRIVATE_PROPERTY = "konanHome"
    }
}

private fun Library.fixNameIfNeeded(
    modelsProvider: IdeModifiableModelsProvider,
    libraryInfoProvider: LiteKonanLibraryInfoProvider,
    kotlinVersion: String
) {
    val libraryModel = modelsProvider.getModifiableLibraryModel(this)

    val libraryName = libraryModel.name.orEmpty()
    if (libraryName.isNotEmpty() && !libraryName.startsWith("Gradle: Kotlin/Native") && !libraryName.startsWith("Kotlin/Native"))
        return // don't generate new name if name is already not empty and does not contain Kotlin/Native

    val libraryUrl = modelsProvider.getLibraryUrls(this, OrderRootType.CLASSES).singleOrNull() ?: return
    val libraryPath = VfsUtil.urlToPath(libraryUrl)

    val libraryInfo = libraryInfoProvider.getDistributionLibraryInfo(libraryPath) ?: return

    val platformNamePart = libraryInfo.platform?.let { " [$it]" }.orEmpty()
    val newLibraryName = "Kotlin/Native $kotlinVersion - ${libraryInfo.name}$platformNamePart"

    libraryModel.name = newLibraryName
}
