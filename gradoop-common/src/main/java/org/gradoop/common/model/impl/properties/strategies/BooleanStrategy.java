/*
 * Copyright © 2014 - 2019 Leipzig University (Database Research Group)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradoop.common.model.impl.properties.strategies;

import org.apache.flink.core.memory.DataInputView;
import org.apache.flink.core.memory.DataOutputView;
import org.apache.hadoop.hbase.util.Bytes;
import org.gradoop.common.model.impl.properties.PropertyValue;
import org.gradoop.common.model.api.strategies.PropertyValueStrategy;

import java.io.IOException;

/**
 * Strategy class for handling {@code PropertyValue} operations with a value of the type
 * {@code Boolean}.
 */
public class BooleanStrategy implements PropertyValueStrategy<Boolean> {

  @Override
  public boolean write(Boolean value, DataOutputView outputView) throws IOException {
    outputView.write(getRawBytes(value));
    return true;
  }

  @Override
  public Boolean read(DataInputView inputView, byte typeByte) throws IOException {
    return inputView.readByte() == -1;
  }

  @Override
  public int compare(Boolean value, Object other) {
    if (other.getClass() == Boolean.class) {
      return Boolean.compare(value, (boolean) other);
    }
    throw new IllegalArgumentException(String.format(
      "Incompatible types: %s, %s", value.getClass(), other.getClass()));
  }

  @Override
  public boolean is(Object value) {
    return value instanceof Boolean;
  }

  @Override
  public Class<Boolean> getType() {
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
    Bytes.putByte(rawBytes, PropertyValue.OFFSET, (byte) (value ? -1 : 0));
    return rawBytes;
  }
}
