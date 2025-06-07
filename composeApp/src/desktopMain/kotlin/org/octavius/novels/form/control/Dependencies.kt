package org.octavius.novels.form.control

/**
 * Definuje zależność między kontrolkami.
 *
 * @param controlName nazwa kontrolki od której zależy ta kontrolka
 * @param value wartość kontrolki która powoduje aktywację zależności
 * @param dependencyType typ zależności (widoczność lub wymagalność)
 * @param comparisonType sposób porównania wartości
 * @param scope zasięg zależności (lokalny w obrębie wiersza lub globalny)
 */
data class ControlDependency<T>(
    val controlName: String,
    val value: T,
    val dependencyType: DependencyType,
    val comparisonType: ComparisonType,
    val scope: DependencyScope = DependencyScope.Global
)

/**
 * Typ zależności między kontrolkami.
 */
enum class DependencyType {
    /** Kontrolka jest widoczna/niewidoczna w zależności od wartości innej kontrolki */
    Visible,

    /** Kontrolka jest wymagana/niewymagana w zależności od wartości innej kontrolki */
    Required,
}

/**
 * Sposób porównania wartości w zależnościach.
 */
enum class ComparisonType {
    /** Wartość musi być jedną z podanych wartości */
    OneOf,

    /** Wartość musi być równa podanej wartości */
    Equals,

    /** Wartość musi być różna od podanej wartości */
    NotEquals
}

/**
 * Zasięg zależności między kontrolkami.
 */
enum class DependencyScope {
    /** Zależność lokalna - w obrębie tego samego wiersza RepeatableControl */
    Local,

    /** Zależność globalna - w całym formularzu */
    Global
}