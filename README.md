# SimpleStringSubstitutor

A simple tool for string placeholder substitution with keyed data.

# How to use

```java
simpleStringSubstitutor = new SimpleStringSubstitutor();

Map<String, Object> data = new HashMap<>();
data.put("string", "Lorem ipsum dolor sit amet");
data.put("int", 2);
data.put("double", 3.14159);

String substitute = simpleStringSubstitutor.substitute("Hello ${string} ${int} ${double}", data);
```

# Configuration

* setThrowsExplicitException: If true, method substitute will throw IllegalArgumentException when error instead of only printing warn or error log.

# Example

1. Simple placeholder.
```java
String substitute = simpleStringSubstitutor.substitute("Hello ${string} ${int} ${double}", data);
```

2. Use string processing method(without arguments) before replacing placeholder.
```java
String substitute = simpleStringSubstitutor.substitute("Hello ${string | trim}", data);
```

3. Use string processing method(with arguments) before replacing placeholder.
```java
String substitute = simpleStringSubstitutor.substitute("Hello ${string | prefix 'pre:'}", data);
```

4. Use chained string processing method.
```java
String substitute = simpleStringSubstitutor.substitute("Hello ${string | trim | prefix 'pre:'}", data);
```

# Author

Yao Zhang