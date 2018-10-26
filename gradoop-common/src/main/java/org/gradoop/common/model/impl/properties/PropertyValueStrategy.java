package org.gradoop.common.model.impl.properties;

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public interface PropertyValueStrategy<T> {

    boolean write(T value, DataOutputView outputView) throws IOException;

    class PropertyValueStrategyFactory {

        public static PropertyValueStrategyFactory INSTANCE =  new PropertyValueStrategyFactory();

        public static PropertyValueStrategy get(Class c) {
            PropertyValueStrategy strategy = INSTANCE.classStrategyMap.get(c);
            return strategy == null ? INSTANCE.noopPropertyValueStrategy : strategy ;
        }
        public static Object fromRawBytes(byte[] bytes) {
            PropertyValueStrategy strategy = INSTANCE.byteStrategyMap.get(bytes[0]);
            return strategy == null ? null : strategy.get(bytes);
        }
        private final Map<Class, PropertyValueStrategy> classStrategyMap;

        private final Map<Byte, PropertyValueStrategy> byteStrategyMap;

        private final NoopPropertyValueStrategy noopPropertyValueStrategy = new NoopPropertyValueStrategy();

        private PropertyValueStrategyFactory() {
            classStrategyMap = new HashMap<>();
            classStrategyMap.put(Boolean.class, new BooleanStrategy());

            byteStrategyMap = new HashMap<>(classStrategyMap.size());
            for (PropertyValueStrategy strategy : classStrategyMap.values()) {
                byteStrategyMap.put(strategy.getRawType(), strategy);
            }
        }

        public static int compare(Object value, Object other) {
            if (value != null) {
                PropertyValueStrategy strategy = get(value.getClass());
                if (strategy.is(other)) {
                    return strategy.compare(value, other);
                }
            }
            return 0;
        }

        public static byte[] getRawBytes(Object value) {
            if (value != null) {
                return get(value.getClass()).getRawBytes(value);
            }
            return new byte[0];
        }

        public static PropertyValueStrategy get(byte value) {
            return INSTANCE.byteStrategyMap.get(value);
        }

        public static PropertyValueStrategy get(Object value) {
            if (value != null) {
                return get(value.getClass());
            }
            return INSTANCE.noopPropertyValueStrategy;
        }

        public static Object from(DataInputView inputView) throws IOException {
            PropertyValueStrategy strategy = INSTANCE.byteStrategyMap.get(inputView.readByte());
            return strategy == null ? null : strategy.get(inputView);
        }
    }

    T get(DataInputView inputView) throws IOException;

    int compare(T value, T other);

    class BooleanStrategy implements PropertyValueStrategy<Boolean> {

        @Override
        public boolean write(Boolean value, DataOutputView outputView) throws IOException {
            outputView.write( getRawBytes(value) );
            return true;
        }

        @Override
        public Boolean get(DataInputView inputView) throws IOException {
            return inputView.readByte() == -1;
        }

        @Override
        public int compare(Boolean value, Boolean other) {
            return Boolean.compare(value, other);
        }

        @Override
        public boolean is(Object value) {
            return value instanceof Boolean;
        }
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        @Override
        public Boolean get(byte[] bytes) {
            return bytes[1] == -1;
        }

        @Override
        public Byte getRawType() {
            return PropertyValue.TYPE_BOOLEAN;
        }

        @Override
        public byte[] getRawBytes(Boolean value) {
            byte[] rawBytes = new byte[PropertyValue.OFFSET + Bytes.SIZEOF_BOOLEAN];
            rawBytes[0] = getRawType();
            Bytes.putByte(rawBytes, PropertyValue.OFFSET, (byte) ((boolean)value ? -1 : 0));
            return rawBytes;
        }



    }
    class NoopPropertyValueStrategy implements PropertyValueStrategy {
        @Override
        public boolean write(Object value, DataOutputView outputView) {
            return false;
        }

        @Override
        public Object get(DataInputView inputView) throws IOException {
            return null;
        }

        @Override
        public int compare(Object value, Object other) {
            return 0;
        }

        @Override
        public boolean is(Object value) {
            return false;
        }
        @Override
        public Class<?> getType() {
            return null;
        }

        @Override
        public Object get(byte[] bytes) {
            return null;
        }

        @Override
        public Byte getRawType() {
            return null;
        }

        @Override
        public byte[] getRawBytes(Object value) {
            return null;
        }


    }
    boolean is(Object value);
    Class<?> getType();
    T get(byte[] bytes);

    Byte getRawType();
    byte[] getRawBytes(T value);
}
