package com.paultech;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleStringSubstitutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleStringSubstitutor.class);

    /**
     * Regular expression for split custom string processing method name and parameters.
     * Use space as delimiter. Spaces in double or single quoted string will be ignored.
     */
    private static final Pattern FUNCTION_WITH_PARAMS_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\"|'.+?')\\s*");

    /**
     * Regular expression for extracting placeholder: "${key}".
     */
    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile("\\$\\{[^}]+}");

    private static final String DEFAULT_STRING_UTIL_CLASS_NAME = "com.paultech.StringUtils";

    private Class<?> stringUtilClass;

    private boolean throwsExplicitException;

    /**
     * Default constructor.
     * Using default class for string processing and do not throw exception if it encounters error.
     */
    public SimpleStringSubstitutor() {
        this(DEFAULT_STRING_UTIL_CLASS_NAME, false);
    }

    /**
     * Constructor for SimpleStringSubstitutor.
     * @param stringUtilClassName User specified class for string processing.
     *                            It should contain package name and class name. Be aware to put it in the classpath.
     */
    public SimpleStringSubstitutor(String stringUtilClassName, boolean throwsExplicitException) {
        this.throwsExplicitException = throwsExplicitException;
        try {
            stringUtilClass = Class.forName(stringUtilClassName);
        } catch (ClassNotFoundException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Cannot find class: {}", DEFAULT_STRING_UTIL_CLASS_NAME);
                LOGGER.error(e.getMessage());
            }
            if (throwsExplicitException) {
                throw new IllegalArgumentException("Cannot find class: {} for custom string processing methods");
            }
            stringUtilClass = null;
        }
    }

    /**
     * If true, method substitute will throw IllegalArgumentException when error instead of only printing warn or error log.
     * @param throwsExplicitException Whether it will throw IllegalArgumentException if error occurs.
     */
    public void setThrowsExplicitException(boolean throwsExplicitException) {
        this.throwsExplicitException = throwsExplicitException;
    }

    /**
     * Substitute pattern with given params. The pattern string contains placeholders with key,
     * which will be substitute with the value in params with the same key.
     * eg: Given the pattern: "Hello ${key1}" and parameter map: {"key1": "World"},
     * the result string will be "Hello World".
     * Also, placeholders support additional string processing method.
     * eg: Given the pattern "${key1 | trim}" and parameter map: {"key1": "  World  "}, first the map value will be trimmed
     * and then replace the placeholder resided in the pattern.
     * The result string will be "World", with the spaces trimmed.
     * @param pattern Pattern contains placeholder.
     * @param params Map contains data in string object key value pair.
     * @return String with placeholder replaced by parameter map.
     */
    public String substitute(String pattern, Map<String, Object> params) {
        Matcher matcher = PLACEHOLDER_REGEX.matcher(pattern);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group();
            String expression = group.substring(2, group.length() - 1);
            String[] keyValueExpressionArray = expression.split("\\|");
            String key = keyValueExpressionArray[0].trim();

            if (!params.containsKey(key)) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("param map does not contain key: {}. Use raw pattern instead", key);
                }
                if (throwsExplicitException) {
                    throw new IllegalArgumentException(String.format("param map does not contain key: %s. Use raw pattern instead", key));
                }
                continue;
            }

            String value = params.get(key).toString();

            for (int i = 1; i < keyValueExpressionArray.length; i++) {
                String[] functionWithParams = parseKeyValueExpressionArray(keyValueExpressionArray[i].trim());
                value = formatValue(value, functionWithParams);
            }

            matcher.appendReplacement(result, value);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String[] parseKeyValueExpressionArray(String functionWithParamsString) {
        functionWithParamsString = functionWithParamsString.trim();
        Matcher m = FUNCTION_WITH_PARAMS_PATTERN.matcher(functionWithParamsString);
        List<String> resultList = new ArrayList<>();
        while (m.find())
            resultList.add(m.group(1));

        return resultList.toArray(new String[0]);
    }

    private String formatValue(String value, String[] functionWithParams) {
        String functionName = functionWithParams[0];
        try {
            Class<?>[] parameterTypes = determineMethodParameterTypes(functionWithParams);
            Method method = stringUtilClass.getDeclaredMethod(functionName, parameterTypes);
            return (String) method.invoke(stringUtilClass, convertParameterTypes(functionWithParams, value, parameterTypes));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(e.toString());
                LOGGER.error("Cannot find corresponding string processing method in {}", stringUtilClass.getName());
            }
            if (throwsExplicitException) {
                throw new IllegalArgumentException(String.format("Cannot find corresponding string processing method in %s", stringUtilClass.getName()));
            }
        }

        return value;
    }

    private Class<?>[] determineMethodParameterTypes(String[] functionWithParams) {
        Class<?>[] classes = new Class<?>[functionWithParams.length];
        classes[0] = String.class;
        for (int i = 1; i < functionWithParams.length; i++) {
            String param = functionWithParams[i];
            if ((param.startsWith("\"") && param.endsWith("\"")) || (param.startsWith("'") && param.endsWith("'"))) {
                classes[i] = String.class;
            } else if (param.matches("^\\d+$")) {
                classes[i] = Integer.class;
            } else if (param.matches("^\\d+\\.\\d+$")) {
                classes[i] = Double.class;
            } else {
                LOGGER.warn("Type of param {} is not supported. Use type object instead", functionWithParams[i]);
                if (throwsExplicitException) {
                    throw new IllegalArgumentException(String.format("Type of param %s is not supported. Use type object instead", functionWithParams[i]));
                }
                classes[i] = Object.class;
            }
        }
        return classes;
    }

    private Object[] convertParameterTypes(String[] functionWithParams, String value, Class<?>[] types) {
        if (functionWithParams.length != types.length) {
            String errorMessage = "The lengths of functionWithParams and types are not equal";
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(errorMessage);
            }
            if (throwsExplicitException) {
                throw new IllegalArgumentException(errorMessage);
            }
            return null;
        }
        Object[] objects = new Object[functionWithParams.length];

        objects[0] = value;

        for (int i = 1; i < functionWithParams.length; i++) {
            if (types[i] == String.class) {
                objects[i] = functionWithParams[i].substring(1, functionWithParams[i].length() - 1);
            } else if (types[i] == Integer.class) {
                objects[i] = Integer.parseInt(functionWithParams[i]);
            } else if (types[i] == Double.class) {
                objects[i] = Double.parseDouble(functionWithParams[i]);
            } else {
                LOGGER.warn("Type of param {} is {}, which is not supported", functionWithParams[i], types[i].getName());
                objects[i] = functionWithParams[i];
            }
        }

        return objects;
    }
}
