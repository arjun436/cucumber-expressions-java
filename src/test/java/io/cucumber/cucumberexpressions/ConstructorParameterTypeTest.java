package io.cucumber.cucumberexpressions;

import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class ConstructorParameterTypeTest {
    @Test
    public void requires_string_ctor() {
        try {
            new ConstructorParameterType<>(NoStringCtor.class, singletonList("whatevs"));
            fail();
        } catch (CucumberExpressionException expected) {
            assertEquals("Missing constructor: `public NoStringCtor(String)`", expected.getMessage());
        }
    }

    @Test
    public void reports_ctor_exceptions() {
        try {
            new ConstructorParameterType<>(FailingCtor.class, singletonList("whatevs")).transform("hello");
            fail();
        } catch (CucumberExpressionException expected) {
            assertEquals("Failed to invoke `new FailingCtor(\"hello\")`", expected.getMessage());
            assertEquals("Boo", expected.getCause().getMessage());
        }
    }

    @Test
    public void reports_abstract_exceptions() {
        try {
            new ConstructorParameterType<>(Abstract.class, singletonList("whatevs")).transform("hello");
            fail();
        } catch (CucumberExpressionException expected) {
            assertEquals("Failed to invoke `new Abstract(\"hello\")`", expected.getMessage());
            assertEquals(InstantiationException.class, expected.getCause().getClass());
        }
    }

    @Test
    public void returns_null_for_value_null() {
        assertNull(new ConstructorParameterType<>(Abstract.class, singletonList("whatevs")).transform(null));
    }

    public static class NoStringCtor {
    }

    public static class FailingCtor {
        public FailingCtor(String s) throws Exception {
            throw new Exception("Boo");
        }
    }

    public static abstract class Abstract {
        public Abstract(String s) throws Exception {
        }
    }
}
