/*******************************************************************************
* Copyright 2000-2016 JetBrains s.r.o.
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
*
*******************************************************************************/
package org.jetbrains.kotlin.core.resolve

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.core.model.KotlinCommonEnvironment
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.ModuleMapping
import org.jetbrains.kotlin.load.kotlin.PackageParts
import org.jetbrains.kotlin.utils.SmartList
import java.io.EOFException

public class KotlinPackagePartProvider(private val environment: KotlinCommonEnvironment) : PackagePartProvider {
    private data class ModuleMappingInfo(val root: VirtualFile, val mapping: ModuleMapping)
    
    private val notLoadedRoots by lazy(LazyThreadSafetyMode.NONE) {
            environment.getRoots()
            .map { it.file }
            .filter { it.findChild("META-INF") != null }
            .toMutableList()
    }
    
    private val loadedModules: MutableList<ModuleMappingInfo> = SmartList()

    override fun findPackageParts(packageFqName: String): List<String> {
        val rootToPackageParts = getPackageParts(packageFqName)
        if (rootToPackageParts.isEmpty()) return emptyList()

        val result = linkedSetOf<String>()
        val visitedMultifileFacades = linkedSetOf<String>()
        for ((virtualFile, packageParts) in rootToPackageParts) {
            for (name in packageParts.parts) {
                val facadeName = packageParts.getMultifileFacadeName(name)
                if (facadeName == null || facadeName !in visitedMultifileFacades) {
                    result.add(name)
                }
            }
            packageParts.parts.mapNotNullTo(visitedMultifileFacades) { packageParts.getMultifileFacadeName(it) }
        }
        return result.toList()
    }

    override fun findMetadataPackageParts(packageFqName: String): List<String> =
            getPackageParts(packageFqName).values.flatMap(PackageParts::metadataParts).distinct()

    @Synchronized
    private fun getPackageParts(packageFqName: String): Map<VirtualFile, PackageParts> {
        processNotLoadedRelevantRoots(packageFqName)

        val result = mutableMapOf<VirtualFile, PackageParts>()
        for ((root, mapping) in loadedModules) {
            val newParts = mapping.findPackageParts(packageFqName) ?: continue
            result[root]?.let { parts -> parts += newParts } ?: result.put(root, newParts)
        }
        return result
    }

    private fun processNotLoadedRelevantRoots(packageFqName: String) {
        if (notLoadedRoots.isEmpty()) return

        val pathParts = packageFqName.split('.')

        val relevantRoots = notLoadedRoots.filter {
            //filter all roots by package path existing
            pathParts.fold(it) {
                parent, part ->
                if (part.isEmpty()) parent
                else parent.findChild(part) ?: return@filter false
            }
            true
        }
        notLoadedRoots.removeAll(relevantRoots)

        for (root in relevantRoots) {
            val metaInf = root.findChild("META-INF") ?: continue
            val moduleFiles = metaInf.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }
            for (moduleFile in moduleFiles) {
                val mapping = try {
                    ModuleMapping.create(moduleFile.contentsToByteArray(), moduleFile.toString())
                }
                catch (e: EOFException) {
                    throw RuntimeException("Error on reading package parts for '$packageFqName' package in '$moduleFile', " +
                                           "roots: $notLoadedRoots", e)
                }
                loadedModules.add(ModuleMappingInfo(root, mapping))
            }
        }
    }
}