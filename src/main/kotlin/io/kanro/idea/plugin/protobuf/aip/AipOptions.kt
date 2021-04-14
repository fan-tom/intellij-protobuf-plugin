package io.kanro.idea.plugin.protobuf.aip

import com.intellij.psi.util.QualifiedName

object AipOptions {
    val resourceReferenceOption =
        QualifiedName.fromComponents("google", "api", "resource_reference")
    val resourceDefinitionOption = QualifiedName.fromComponents("google", "api", "resource_definition")
    val resourceOption = QualifiedName.fromComponents("google", "api", "resource")
    val resourceTypeField = QualifiedName.fromComponents("type")

    val httpRuleBodyName = QualifiedName.fromComponents("google", "api", "HttpRule", "body")
    val httpRuleResponseBodyName = QualifiedName.fromComponents("google", "api", "HttpRule", "response_body")
}