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
package org.gradoop.flink.model.impl.operators.matching.common.query.predicates.compareables;

import org.gradoop.common.model.impl.id.GradoopId;
import org.gradoop.common.model.impl.properties.PropertyValue;
import org.gradoop.flink.model.impl.operators.matching.common.query.predicates.comparables.ElementSelectorComparable;
import org.gradoop.flink.model.impl.operators.matching.single.cypher.pojos.Embedding;
import org.gradoop.flink.model.impl.operators.matching.single.cypher.pojos.EmbeddingMetaData;
import org.gradoop.flink.model.impl.operators.matching.single.cypher.pojos.EmbeddingMetaData.EntryType;
import org.junit.Test;
import org.s1ck.gdl.model.comparables.ElementSelector;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ElementSelectorComparableTest {
  @Test
  public void testEvaluationReturnsPropertyValue() {
    ElementSelector selector = new ElementSelector("a");
    ElementSelectorComparable wrapper = new ElementSelectorComparable(selector);

    Embedding embedding = new Embedding();
    embedding.add(GradoopId.get());
    PropertyValue reference = PropertyValue.create(embedding.getId(0));

    EmbeddingMetaData metaData = new EmbeddingMetaData();
    metaData.setEntryColumn("a", EntryType.VERTEX, 0);

    assertEquals(reference, wrapper.evaluate(embedding, metaData));
    assertNotEquals(PropertyValue.create("42"), wrapper.evaluate(embedding, metaData));
  }

  @Test(expected = NoSuchElementException.class)
  public void testThrowErrorIfElementNotPresent() {
    ElementSelector selector = new ElementSelector("a");
    ElementSelectorComparable wrapper = new ElementSelectorComparable(selector);

    Embedding embedding = new Embedding();
    embedding.add(GradoopId.get());

    EmbeddingMetaData metaData = new EmbeddingMetaData();
    metaData.setEntryColumn("b", EntryType.VERTEX, 0);

    wrapper.evaluate(embedding, metaData);
  }
}
