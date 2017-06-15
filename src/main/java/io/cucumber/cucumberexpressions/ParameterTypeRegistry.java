package io.cucumber.cucumberexpressions;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ParameterTypeRegistry {
    private static final List<String> INTEGER_REGEXPS = asList("-?\\d+", "\\d+");
    private static final List<String> FLOAT_REGEXPS = singletonList("-?\\d*[\\.,]\\d+");
    private static final List<String> HEX_REGEXPS = singletonList("0[xX][0-9a-fA-F]{2}");
    private static final Map<Class, Class> BOXED = new HashMap<Class, Class>() {{
        put(byte.class, Byte.class);
        put(short.class, Short.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(float.class, Float.class);
        put(double.class, Double.class);
        // Not dealing with boolean, char, void
    }};

    private final Map<Type, ParameterType<?>> parameterTypeByType = new HashMap<>();
    private final Map<String, ParameterType<?>> parameterTypeByName = new HashMap<>();
    private final Map<String, SortedSet<ParameterType<?>>> parameterTypesByRegexp = new HashMap<>();

    public ParameterTypeRegistry(Locale locale) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        NumberParser numberParser = new NumberParser(numberFormat);

        defineParameterType(new SimpleParameterType<>("bigint", BigInteger.class, false, INTEGER_REGEXPS, BigInteger::new));
        defineParameterType(new SimpleParameterType<>("bigdecimal", BigDecimal.class, false, INTEGER_REGEXPS, BigDecimal::new));
        defineParameterType(new SimpleParameterType<>("byte", Byte.class, false, HEX_REGEXPS, Byte::decode));
        defineParameterType(new SimpleParameterType<>("short", Short.class, false, INTEGER_REGEXPS, Short::decode));
        defineParameterType(new SimpleParameterType<>("int", Integer.class, true, INTEGER_REGEXPS, Integer::decode));
        defineParameterType(new SimpleParameterType<>("long", Long.class, false, INTEGER_REGEXPS, Long::decode));
        defineParameterType(new SimpleParameterType<>("float", Float.class, false, FLOAT_REGEXPS, numberParser::parseFloat));
        defineParameterType(new SimpleParameterType<>("double", Double.class, true, FLOAT_REGEXPS, numberParser::parseDouble));
    }

    public void defineParameterType(ParameterType<?> parameterType) {
        put(parameterTypeByName, parameterType.getName(), parameterType, "name", parameterType.getName());
        put(parameterTypeByType, parameterType.getType(), parameterType, "type", parameterType.getType().getTypeName());

        for (String parameterTypeRegexp : parameterType.getRegexps()) {
            SortedSet<ParameterType<?>> parameterTypes = parameterTypesByRegexp
                    .computeIfAbsent(parameterTypeRegexp, r -> new TreeSet<>(new ParameterTypeComparator()));
            if (!parameterTypes.isEmpty() && parameterTypes.first().isPreferential() && parameterType.isPreferential()) {
                throw new CucumberExpressionException(String.format(
                        "There can only be one preferential parameter type per regexp. " +
                                "The regexp /%s/ is used for two preferential parameter types, {%s} and {%s}",
                        parameterTypeRegexp, parameterTypes.first().getName(), parameterType.getName()
                ));
            }
            parameterTypes.add(parameterType);
        }
    }

    private <K> void put(Map<K, ParameterType<?>> map, K key, ParameterType<?> parameterType, String prop, String keyName) {
        if (map.containsKey(key))
            throw new DuplicateTypeNameException(String.format("There is already a parameter type with %s %s", prop, keyName));
        map.put(key, parameterType);
    }

    public <T> ParameterType<T> lookupByType(Type type) {
        if (type instanceof Class && ((Class) type).isPrimitive()) {
            type = BOXED.get(type);
        }
        return (ParameterType<T>) parameterTypeByType.get(type);
    }

    public <T> ParameterType<T> lookupByTypeName(String typeName) {
        return (ParameterType<T>) parameterTypeByName.get(typeName);
    }

    public <T> ParameterType<T> lookupByRegexp(String parameterTypeRegexp, Pattern expressionRegexp, String text) {
        SortedSet<ParameterType<?>> parameterTypes = parameterTypesByRegexp.get(parameterTypeRegexp);
        if (parameterTypes == null) return null;
        if (parameterTypes.size() > 1 && !parameterTypes.first().isPreferential()) {
            // We don't do this check on insertion because we only want to restrict
            // ambiguity when we look up by Regexp. Users of CucumberExpression should
            // not be restricted.
            List<GeneratedExpression> generatedExpressions = new CucumberExpressionGenerator(this).generateExpressions(text);
            throw new AmbiguousParameterTypeException(parameterTypeRegexp, expressionRegexp, parameterTypes, generatedExpressions);
        }
        return (ParameterType<T>) parameterTypes.first();
    }

    public Collection<ParameterType<?>> getParameterTypes() {
        return parameterTypeByName.values();
    }
}
