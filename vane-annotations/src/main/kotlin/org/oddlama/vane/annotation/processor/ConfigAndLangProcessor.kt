package org.oddlama.vane.annotation.processor

import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@SupportedAnnotationTypes(
    "org.oddlama.vane.annotation.config.ConfigBoolean",
    "org.oddlama.vane.annotation.config.ConfigDict",
    "org.oddlama.vane.annotation.config.ConfigDouble",
    "org.oddlama.vane.annotation.config.ConfigDoubleList",
    "org.oddlama.vane.annotation.config.ConfigExtendedMaterial",
    "org.oddlama.vane.annotation.config.ConfigInt",
    "org.oddlama.vane.annotation.config.ConfigIntList",
    "org.oddlama.vane.annotation.config.ConfigItemStack",
    "org.oddlama.vane.annotation.config.ConfigLong",
    "org.oddlama.vane.annotation.config.ConfigMaterial",
    "org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap",
    "org.oddlama.vane.annotation.config.ConfigMaterialSet",
    "org.oddlama.vane.annotation.config.ConfigString",
    "org.oddlama.vane.annotation.config.ConfigStringList",
    "org.oddlama.vane.annotation.config.ConfigStringListMap",
    "org.oddlama.vane.annotation.config.ConfigVersion",
    "org.oddlama.vane.annotation.lang.LangMessage",
    "org.oddlama.vane.annotation.lang.LangMessageArray",
    "org.oddlama.vane.annotation.lang.LangVersion"
)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
class ConfigAndLangProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            roundEnv.getElementsAnnotatedWith(annotation).forEach { e: Element? -> verifyType(annotation, e!!) }
        }

        return true
    }

    private fun verifyType(annotation: TypeElement, element: Element) {
        val type: String = element.asType().toString()
        val requiredType: String? = fieldTypeMapping[annotation.asType().toString()]
        if (requiredType == null) {
            processingEnv
                .messager
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    element.asType().toString() +
                            ": @" +
                            annotation.simpleName +
                            " has no requiredType mapping! This is a bug."
                )
        } else {
            if (requiredType != "<any>" && requiredType != type) {
                processingEnv
                    .messager
                    .printMessage(
                        Diagnostic.Kind.ERROR,
                        element.asType().toString() +
                                ": @" +
                                annotation.simpleName +
                                " requires a field of type " +
                                requiredType +
                                " but got " +
                                type
                    )
            }
        }
    }

    companion object {
        private val fieldTypeMapping: MutableMap<String?, String?>

        init {
            val map: MutableMap<String?, String?> = HashMap<String?, String?>()
            map["org.oddlama.vane.annotation.config.ConfigBoolean"] = "boolean"
            map["org.oddlama.vane.annotation.config.ConfigDict"] = "<any>"
            map["org.oddlama.vane.annotation.config.ConfigDouble"] = "double"
            map["org.oddlama.vane.annotation.config.ConfigDoubleList"] = "java.util.List<java.lang.Double>"
            map["org.oddlama.vane.annotation.config.ConfigExtendedMaterial"] = "org.oddlama.vane.core.material.ExtendedMaterial"
            map["org.oddlama.vane.annotation.config.ConfigInt"] = "int"
            map["org.oddlama.vane.annotation.config.ConfigIntList"] = "java.util.List<java.lang.Integer>"
            map["org.oddlama.vane.annotation.config.ConfigItemStack"] = "org.bukkit.inventory.ItemStack"
            map["org.oddlama.vane.annotation.config.ConfigLong"] = "long"
            map["org.oddlama.vane.annotation.config.ConfigMaterial"] = "org.bukkit.Material"
            map["org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap"] = "java.util.Map<java.lang.String,java.util.Map<java.lang.String,java.util.Map<java.lang.String,org.bukkit.Material>>>"
            map["org.oddlama.vane.annotation.config.ConfigMaterialSet"] = "java.util.Set<org.bukkit.Material>"
            map["org.oddlama.vane.annotation.config.ConfigString"] = "java.lang.String"
            map["org.oddlama.vane.annotation.config.ConfigStringList"] = "java.util.List<java.lang.String>"
            map["org.oddlama.vane.annotation.config.ConfigStringListMap"] = "java.util.Map<java.lang.String,java.util.List<java.lang.String>>"
            map["org.oddlama.vane.annotation.config.ConfigVersion"] = "long"
            map["org.oddlama.vane.annotation.lang.LangMessage"] = "org.oddlama.vane.core.lang.TranslatedMessage"
            map["org.oddlama.vane.annotation.lang.LangMessageArray"] = "org.oddlama.vane.core.lang.TranslatedMessageArray"
            map["org.oddlama.vane.annotation.lang.LangVersion"] = "long"
            fieldTypeMapping = Collections.unmodifiableMap<String?, String?>(map)
        }
    }
}
