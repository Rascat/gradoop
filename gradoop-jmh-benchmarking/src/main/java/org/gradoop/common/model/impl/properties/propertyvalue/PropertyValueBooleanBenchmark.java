package org.gradoop.common.model.impl.properties.propertyvalue;

import org.gradoop.common.model.impl.properties.PropertyValue;
import org.openjdk.jmh.annotations.*;

import java.util.Arrays;
import java.util.HashSet;

@Warmup(time = 1)
@Measurement(time = 1)
@State(Scope.Thread)
public class PropertyValueBooleanBenchmark {

    private PropertyValue BOOLEAN_VALUE;
    private PropertyValue VALUE;

    @Setup
    public void setup() {
        VALUE = new PropertyValue();
        BOOLEAN_VALUE = PropertyValue.fromRawBytes(new byte[] {0x1, 0xf});
    }

    @Benchmark
    public void create() {
        PropertyValue.create(Boolean.TRUE);
    }

    @Benchmark
    public void set() {
        VALUE.setBoolean(true);
    }

    @Benchmark
    public void is() {
        BOOLEAN_VALUE.isBoolean();
    }

    @Benchmark
    public void get() {
        BOOLEAN_VALUE.getBoolean();
    }

    @Benchmark
    public void setObject() {
        VALUE.setObject(Boolean.TRUE);
    }

    @Benchmark
    public void getType() {
        BOOLEAN_VALUE.getType();
    }
}
