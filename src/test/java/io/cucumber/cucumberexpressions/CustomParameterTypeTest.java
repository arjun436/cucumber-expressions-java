package io.cucumber.cucumberexpressions;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CustomParameterTypeTest {
    public static class Color {
        public final String name;

        /// [color-constructor]
        public Color(String name) {
            this.name = name;
        }
        /// [color-constructor]

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Color && ((Color) obj).name.equals(name);
        }
    }

    public static class CssColor {
        public final String name;

        /// [color-constructor]
        public CssColor(String name) {
            this.name = name;
        }
        /// [color-constructor]

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CssColor && ((CssColor) obj).name.equals(name);
        }
    }

    private ParameterTypeRegistry parameterTypeRegistry = new ParameterTypeRegistry(Locale.ENGLISH);

    @Before
    public void create_parameter() {
        /// [add-color-parameter-type]
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "color",
                Color.class,
                "red|blue|yellow",
                new Function<String, Color>() {
                    @Override
                    public Color apply(String name) {
                        return new Color(name);
                    }
                }
        ));
        /// [add-color-parameter-type]
    }

    @Test
    public void matches_CucumberExpression_parameters_with_custom_parameter_type() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterTypeRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void matches_CucumberExpression_parameters_with_custom_parameter_type_using_optional_group() {
        parameterTypeRegistry = new ParameterTypeRegistry(Locale.ENGLISH);
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "color",
                Color.class,
                asList("red|blue|yellow", "(?:dark|light) (?:red|blue|yellow)"),
                new Function<String, Color>() {
                    @Override
                    public Color apply(String name) {
                        return new Color(name);
                    }
                }
        ));
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterTypeRegistry);
        Object transformedArgumentValue = expression.match("I have a dark red ball").get(0).getTransformedValue();
        assertEquals(new Color("dark red"), transformedArgumentValue);
    }

    @Test
    public void matches_CucumberExpression_parameters_with_custom_parameter_without_type_and_transform() {
        parameterTypeRegistry = new ParameterTypeRegistry(Locale.ENGLISH);
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "color",
                "red|blue|yellow"
        ));
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>emptyList(), parameterTypeRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals("red", transformedArgumentValue);
    }

    @Test
    public void matches_CucumberExpression_parameters_with_explicit_type() {
        Expression expression = new CucumberExpression("I have a {color} ball", Collections.<Type>singletonList(Color.class), new ParameterTypeRegistry(Locale.ENGLISH));
        Color transformedArgumentValue = (Color) expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals("red", transformedArgumentValue.name);
    }

    @Test
    public void defers_transformation_until_queried_from_argument() {
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "throwing",
                String.class,
                "bad",
                new Function<String, String>() {
                    @Override
                    public String apply(String name) {
                        throw new RuntimeException(String.format("Can't transform [%s]", name));
                    }
                }));
        Expression expression = new CucumberExpression("I have a {throwing} parameter", Collections.<Type>emptyList(), parameterTypeRegistry);
        List<Argument> arguments = expression.match("I have a bad parameter");
        try {
            arguments.get(0).getTransformedValue();
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("Can't transform [bad]", expected.getMessage());
        }
    }

    ///// Conflicting parameter types

    @Test
    public void conflicting_parameter_type_is_detected_for_type() {
        try {
            parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                    "color",
                    String.class,
                    ".*",
                    new Function<String, String>() {
                        @Override
                        public String apply(String s) {
                            return s;
                        }
                    }));
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("There is already a parameter type with type name color", expected.getMessage());
        }
    }

    @Test
    public void conflicting_parameter_type_is_detected_for_type_name() {
        try {
            parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                    "whatever",
                    Color.class,
                    ".*",
                    new Function<String, Color>() {
                        @Override
                        public Color apply(String s) {
                            return new Color(s);
                        }
                    }));
            fail("should have failed");
        } catch (RuntimeException expected) {
            assertEquals("There is already a parameter type with type io.cucumber.cucumberexpressions.CustomParameterTypeTest$Color", expected.getMessage());
        }
    }

    @Test
    public void conflicting_parameter_type_is_not_detected_for_regexp() {
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "css-color",
                CssColor.class,
                "red|blue|yellow",
                new Function<String, CssColor>() {
                    @Override
                    public CssColor apply(String s) {
                        return new CssColor(s);
                    }
                }));

        assertEquals(new CssColor("blue"), new CucumberExpression("I have a {css-color} ball", emptyList(), parameterTypeRegistry).match("I have a blue ball").get(0).getTransformedValue());
        assertEquals(new CssColor("blue"), new CucumberExpression("I have a {css-color} ball", singletonList(CssColor.class), parameterTypeRegistry).match("I have a blue ball").get(0).getTransformedValue());
        assertEquals(new Color("blue"), new CucumberExpression("I have a {color} ball", emptyList(), parameterTypeRegistry).match("I have a blue ball").get(0).getTransformedValue());
        assertEquals(new Color("blue"), new CucumberExpression("I have a {color} ball", singletonList(Color.class), parameterTypeRegistry).match("I have a blue ball").get(0).getTransformedValue());
    }

    @Test
    public void conflicting_parameter_type_is_not_detected_when_type_is_null() {
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "foo",
                null,
                "foo",
                new Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s;
                    }
                }));
        parameterTypeRegistry.defineParameterType(new SimpleParameterType<>(
                "bar",
                null,
                "bar",
                new Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s;
                    }
                }));
    }

    ///// RegularExpression

    @Test
    public void matches_RegularExpression_arguments_with_explicit_type() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>singletonList(Color.class), parameterTypeRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void matches_RegularExpression_arguments_without_explicit_type() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>emptyList(), parameterTypeRegistry);
        Object transformedArgumentValue = expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals(new Color("red"), transformedArgumentValue);
    }

    @Test
    public void matches_RegularExpression_arguments_with_explicit_type_using_constructor_directly() {
        Expression expression = new RegularExpression(compile("I have a (red|blue|yellow) ball"), Collections.<Type>singletonList(Color.class), new ParameterTypeRegistry(Locale.ENGLISH));
        Color transformedArgumentValue = (Color) expression.match("I have a red ball").get(0).getTransformedValue();
        assertEquals("red", transformedArgumentValue.name);
    }
}
