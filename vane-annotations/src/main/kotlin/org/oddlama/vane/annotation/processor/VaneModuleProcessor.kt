package org.oddlama.vane.annotation.processor

import javax.annotation.processing.SupportedAnnotationTypes

/** Validates that `@VaneModule` sits on a class extending vane-core's `Module`. */
@SupportedAnnotationTypes("org.oddlama.vane.annotation.VaneModule")
class VaneModuleProcessor : ClassPlacementProcessor("org.oddlama.vane.core.module.Module")
