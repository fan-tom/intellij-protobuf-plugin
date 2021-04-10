package io.kanro.idea.plugin.protobuf.lang.file

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

class ModuleSourceProtobufFileResolver : FileResolver {
    override fun findFile(path: String, project: Project): Iterable<VirtualFile> {
        return listOf()
    }

    override fun findFile(path: String, module: Module): Iterable<VirtualFile> {
        return ModuleRootManager.getInstance(module).sourceRoots.mapNotNull {
            it.findFileByRelativePath(path)?.takeIf { it.exists() }
        }
    }
}
