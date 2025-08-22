package org.morecup.pragmaddd.analyzer.collector

import org.morecup.pragmaddd.analyzer.error.AnalysisError
import org.morecup.pragmaddd.analyzer.error.AnalysisWarning

/**
 * Validation result containing errors and warnings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<AnalysisError>,
    val warnings: List<AnalysisWarning>
)