package com.paultech;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TestSimpleStringSubstitutor {
    private SimpleStringSubstitutor simpleStringSubstitutor;

    private Map<String, Object> data;

    @Before
    public void before() {
        simpleStringSubstitutor = new SimpleStringSubstitutor();

        data = new HashMap<>();
        data.put("string", "Lorem ipsum dolor sit amet");
        data.put("int", 2);
        data.put("double", 3.14159);
    }

    @Test
    public void testNormal() {
        String substitute = simpleStringSubstitutor.substitute("Hello ${string} ${int} ${double}", data);

        System.out.println(substitute);
        Assert.assertEquals("Hello Lorem ipsum dolor sit amet 2 3.14159", substitute);
    }

    @Test
    public void testMethodWithNoArgs() {
        data.put("stringWithSpace", "   World     ");
        String substitute = simpleStringSubstitutor.substitute("Hello ${stringWithSpace | trim} ${int} ${double}", data);
        System.out.println(substitute);
        Assert.assertEquals("Hello World 2 3.14159", substitute);
    }

    @Test
    public void testMethodWithArgs() {
        String substitute = simpleStringSubstitutor.substitute("Hello ${string | prefix 'String:'} ${int} ${double}", data);
        System.out.println(substitute);
        Assert.assertEquals("Hello String:Lorem ipsum dolor sit amet 2 3.14159", substitute);
    }

    @Test
    public void testChainedMethod() {
        data.put("stringWithSpace", "   World     ");
        String substitute = simpleStringSubstitutor.substitute("Hello ${stringWithSpace | trim | prefix 'pre:'} ${int} ${double}", data);
        System.out.println(substitute);
        Assert.assertEquals("Hello pre:World 2 3.14159", substitute);
    }

    @Test
    public void testMethodDoesNotExist() {
        String substitute = simpleStringSubstitutor.substitute("Hello ${string | append}", data);

        System.out.println(substitute);
        Assert.assertEquals("Hello Lorem ipsum dolor sit amet", substitute);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailure1() {
        simpleStringSubstitutor.setThrowsExplicitException(true);
        String substitute = simpleStringSubstitutor.substitute("Hello ${does_not_exist}", data);

        System.out.println(substitute);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailure2() {
        simpleStringSubstitutor.setThrowsExplicitException(true);
        String substitute = simpleStringSubstitutor.substitute("Hello ${string|func_does_not_exist}", data);

        System.out.println(substitute);
    }
}
