package io.kanro.idea.plugin.protobuf.lang.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufBuiltInOptionName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufConstant
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufEnumDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufEnumValue
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufEnumValueDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufFieldAssign
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufFieldDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufFieldName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufGroupDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufImportStatement
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufMapFieldDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufMessageDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufOptionAssign
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufReservedName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufReservedRange
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufServiceDefinition
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufStringValue
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufTypeName
import io.kanro.idea.plugin.protobuf.lang.psi.ProtobufVisitor
import io.kanro.idea.plugin.protobuf.lang.psi.enum
import io.kanro.idea.plugin.protobuf.lang.psi.field
import io.kanro.idea.plugin.protobuf.lang.psi.float
import io.kanro.idea.plugin.protobuf.lang.psi.forEach
import io.kanro.idea.plugin.protobuf.lang.psi.int
import io.kanro.idea.plugin.protobuf.lang.psi.isFieldDefaultOption
import io.kanro.idea.plugin.protobuf.lang.psi.message
import io.kanro.idea.plugin.protobuf.lang.psi.resolve
import io.kanro.idea.plugin.protobuf.lang.psi.uint
import io.kanro.idea.plugin.protobuf.lang.support.BuiltInType

class ProtobufAnnotator : Annotator {
    companion object {
        private val allowKeyType = setOf(
            BuiltInType.INT32.value(),
            BuiltInType.INT64.value(),
            BuiltInType.UINT32.value(),
            BuiltInType.UINT64.value(),
            BuiltInType.SINT32.value(),
            BuiltInType.SINT64.value(),
            BuiltInType.FIXED32.value(),
            BuiltInType.FIXED64.value(),
            BuiltInType.SFIXED32.value(),
            BuiltInType.SFIXED64.value(),
            BuiltInType.BOOL.value(),
            BuiltInType.STRING.value()
        )
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(object : ProtobufVisitor() {
            override fun visitMapFieldDefinition(o: ProtobufMapFieldDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
                NumberTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
                val types = o.typeNameList
                if (types.size != 2) return
                val keyType = types[0].text
                if (keyType !in allowKeyType) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "$keyType is not a valid key type of map"
                    )
                        .range(types[0].textRange)
                        .create()
                }
            }

            override fun visitImportStatement(o: ProtobufImportStatement) {
                ImportTracker.tracker(o.file()).visit(o, holder)
            }

            override fun visitTypeName(o: ProtobufTypeName) {
                if (o.symbolNameList.size == 1 && BuiltInType.isBuiltInType(o.text)) return
                if (o.resolve() == null) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Symbol '${o.text}' not found"
                    )
                        .range(o.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                }
            }

            override fun visitBuiltInOptionName(o: ProtobufBuiltInOptionName) {
                if (!o.isFieldDefaultOption() && o.reference?.resolve() == null) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Built-in option '${o.text}' not found"
                    )
                        .range(o.textRange)
                        .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        .create()
                }
            }

            override fun visitFieldName(o: ProtobufFieldName) {
                val message = o.message() ?: return
                message.forEach {
                    if (it.name() == element.text) {
                        return
                    }
                }
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Field '${o.text}' not found"
                )
                    .range(o.textRange)
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create()
            }

            override fun visitEnumValue(o: ProtobufEnumValue) {
                val enum = o.enum() ?: return
                enum.forEach {
                    if (it.name() == element.text) {
                        return
                    }
                }
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "Enum value '${o.text}' not found"
                )
                    .range(o.textRange)
                    .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                    .create()
            }

            override fun visitStringValue(o: ProtobufStringValue) {
                o.reference?.let {
                    if (it.resolve() == null) {
                        holder.newAnnotation(
                            HighlightSeverity.ERROR,
                            "Resource name ${o.text} not found."
                        )
                            .range(o.textRange)
                            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                            .create()
                    }
                    return
                }
            }

            override fun visitConstant(o: ProtobufConstant) {
                val field = when (val parent = o.parent) {
                    is ProtobufOptionAssign -> {
                        parent.optionName.field() as? ProtobufFieldDefinition ?: return
                    }
                    is ProtobufFieldAssign -> {
                        parent.fieldName.reference?.resolve() as? ProtobufFieldDefinition ?: return
                    }
                    else -> return
                }

                val message = when (val type = field.typeName.text) {
                    BuiltInType.BOOL.value() -> if (o.booleanValue == null) {
                        "Field \"${field.name()}\" required a boolean value"
                    } else null
                    BuiltInType.STRING.value() -> if (o.stringValue == null) {
                        "Field \"${field.name()}\" required a string value"
                    } else null
                    BuiltInType.FLOAT.value(),
                    BuiltInType.DOUBLE.value() -> if (o.numberValue?.float() == null) {
                        "Field \"${field.name()}\" required a number value"
                    } else null
                    BuiltInType.UINT32.value(),
                    BuiltInType.UINT64.value(),
                    BuiltInType.FIXED32.value(),
                    BuiltInType.FIXED64.value() -> if (o.numberValue?.int() == null) {
                        "Field \"${field.name()}\" required a int value"
                    } else null
                    BuiltInType.INT32.value(),
                    BuiltInType.INT64.value(),
                    BuiltInType.SINT32.value(),
                    BuiltInType.SINT64.value(),
                    BuiltInType.SFIXED32.value(),
                    BuiltInType.SFIXED64.value() -> if (o.numberValue?.uint() == null) {
                        "Field \"${field.name()}\" required a uint value"
                    } else null
                    else -> {
                        when (val typeDefinition = field.typeName.resolve()) {
                            is ProtobufEnumDefinition -> if (o.enumValue == null) {
                                "Field \"${field.name()}\" required a value of \"${typeDefinition.qualifiedName()}\""
                            } else null
                            is ProtobufMessageDefinition -> if (o.messageValue == null) {
                                "Field \"${field.name()}\" required \"${typeDefinition.qualifiedName()}\" value"
                            } else null
                            else -> null
                        }
                    }
                }

                message?.let {
                    holder.newAnnotation(HighlightSeverity.ERROR, it)
                        .range(o.textRange)
                        .create()
                }
            }

            override fun visitEnumDefinition(o: ProtobufEnumDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
                if (o.items().isEmpty()) {
                    holder.newAnnotation(
                        HighlightSeverity.ERROR,
                        "Enum must not be empty"
                    )
                        .range(o.body()?.textRange ?: o.textRange)
                        .create()
                }
            }

            override fun visitFieldDefinition(o: ProtobufFieldDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
                NumberTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
            }

            override fun visitMessageDefinition(o: ProtobufMessageDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
            }

            override fun visitServiceDefinition(o: ProtobufServiceDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
            }

            override fun visitGroupDefinition(o: ProtobufGroupDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
                NumberTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
            }

            override fun visitEnumValueDefinition(o: ProtobufEnumValueDefinition) {
                ScopeTracker.tracker(o.owner() ?: return).visit(o, holder)
                NumberTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
            }

            override fun visitReservedName(o: ProtobufReservedName) {
                ScopeTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
            }

            override fun visitReservedRange(o: ProtobufReservedRange) {
                NumberTracker.tracker(o.parentOfType() ?: return).visit(o, holder)
            }
        })
    }
}
