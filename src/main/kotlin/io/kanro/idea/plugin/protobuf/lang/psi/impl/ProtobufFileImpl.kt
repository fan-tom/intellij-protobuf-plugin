package io.kanro.idea.plugin.protobuf.lang.psi.impl

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName
import io.kanro.idea.plugin.protobuf.Icons
import io.kanro.idea.plugin.protobuf.lang.ProtobufFileType
import io.kanro.idea.plugin.protobuf.lang.ProtobufLanguage
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufEnumDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufFile
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufImportStatement
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufMessageDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufPackageName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufPackageStatement
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufReservedName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufServiceDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufSyntaxStatement
import io.kanro.idea.plugin.protobuf.lang.psi.primitive.stratify.ProtobufOptionHover
import io.kanro.idea.plugin.protobuf.lang.psi.primitive.structure.ProtobufScope
import io.kanro.idea.plugin.protobuf.lang.psi.primitive.structure.ProtobufScopeItem
import io.kanro.idea.plugin.protobuf.lang.util.doc
import javax.swing.Icon

class ProtobufFileImpl(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ProtobufLanguage), ProtobufFile {
    override fun getFileType(): FileType {
        return ProtobufFileType
    }

    override fun messages(): Iterable<ProtobufMessageDefinition> {
        return findChildrenByClass(ProtobufMessageDefinition::class.java).asIterable()
    }

    override fun enums(): Iterable<ProtobufEnumDefinition> {
        return findChildrenByClass(ProtobufEnumDefinition::class.java).asIterable()
    }

    override fun services(): Iterable<ProtobufServiceDefinition> {
        return findChildrenByClass(ProtobufServiceDefinition::class.java).asIterable()
    }

    override fun imports(): Iterable<ProtobufImportStatement> {
        return findChildrenByClass(ProtobufImportStatement::class.java).asIterable()
    }

    override fun toString(): String {
        return "Protobuf File"
    }

    override fun lookup(): LookupElementBuilder? {
        return null
    }

    override fun name(): String {
        return this.name
    }

    override fun nameElement(): PsiElement? {
        return null
    }

    override fun type(): String {
        return "file"
    }

    override fun getPresentation(): ItemPresentation? {
        return this
    }

    override fun getLocationString(): String? {
        return scope()?.toString()
    }

    private fun getRootInfo(): String? {
        return when (val system = virtualFile.fileSystem) {
            is ArchiveFileSystem -> {
                val project = ProjectLocator.getInstance().guessProjectForFile(virtualFile)
                if (project != null) {
                    LibraryUtil.findLibraryEntry(virtualFile, project)?.let {
                        return it.presentableName
                    }
                }
                "${system.getLocalByEntry(virtualFile)?.name}"
            }
            is LocalFileSystem -> {
                val project = ProjectLocator.getInstance().guessProjectForFile(virtualFile)
                if (project != null) {
                    ModuleManager.getInstance(project).modules.forEach {
                        if (ModuleRootManager.getInstance(it).fileIndex.isInContent(virtualFile)) {
                            return it.name
                        }
                    }
                    return project.name
                }
                "(external)"
            }
            else -> "(unsupported)"
        }
    }

    override fun navigateInfo(): String? {
        return doc {
            link {
                locationString?.let {
                    text("$it ")
                }
            }
            text(getRootInfo())
            definition {
                text("${type()} $presentableText")
            }
        }
    }

    override fun owner(): ProtobufScope? {
        return null
    }

    override fun scope(): QualifiedName? {
        return QualifiedName.fromComponents(packageParts().map { it.text })
    }

    override fun syntax(): String? {
        val syntax = this.findChildByClass(ProtobufSyntaxStatement::class.java) ?: return null
        val text = syntax.stringValue?.text ?: return null
        return text.substring(1, text.length - 1)
    }

    override fun packageParts(): Array<ProtobufPackageName> {
        return findChildByClass(ProtobufPackageStatement::class.java)?.packageNameList?.toTypedArray() ?: arrayOf()
    }

    override fun options(): Array<ProtobufOptionHover> {
        return this.findChildrenByClass(ProtobufOptionHover::class.java)
    }

    override fun items(): Array<ProtobufScopeItem> {
        return this.findChildrenByClass(ProtobufScopeItem::class.java)
    }

    override fun reservedNames(): Array<ProtobufReservedName> {
        return arrayOf()
    }

    override fun getIcon(unused: Boolean): Icon? {
        return Icons.FILE
    }
}
