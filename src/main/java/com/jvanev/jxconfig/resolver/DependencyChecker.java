package com.jvanev.jxconfig.resolver;

/**
 * Implementations of this interface provide custom logic for checking
 * whether a dependency's value matches the required value.
 */
@FunctionalInterface
public interface DependencyChecker {
    /**
     * The default condition check operator's symbol.
     */
    String DEFAULT_OPERATOR = "X";

    /**
     * Returns the result of checking the dependency's value against the required value using the specified operator.
     * <p>
     * <b>Note:</b> This method will only be invoked for non-default operators. The library's
     * built-in mechanism handles the {@link #DEFAULT_OPERATOR} check automatically.
     *
     * @param dependencyValue The resolved value of the dependency
     * @param operator        The comparison operator to be used
     * @param requiredValue   The value that the dependency must have in order for the check to be successful
     *
     * @return {@code true} if the check is successful, {@code false} otherwise.
     */
    boolean check(String dependencyValue, String operator, String requiredValue);
}
