package io.cucumber.cucumberexpressions;

import java.lang.reflect.Type;
import java.util.List;

import static java.util.Collections.singletonList;

public abstract class AbstractParameter<T> implements Parameter<T> {
    private final String typeName;
    private final Type type;
    private final List<String> captureGroupRegexps;

    public AbstractParameter(String typeName, Type type, List<String> captureGroupRegexps) {
        this.captureGroupRegexps = captureGroupRegexps;
        this.typeName = typeName;
        this.type = type;
    }

    public AbstractParameter(String typeName, Type type, String captureGroupRegexp) {
        this(typeName, type, singletonList(captureGroupRegexp));
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public List<String> getCaptureGroupRegexps() {
        return captureGroupRegexps;
    }
}
