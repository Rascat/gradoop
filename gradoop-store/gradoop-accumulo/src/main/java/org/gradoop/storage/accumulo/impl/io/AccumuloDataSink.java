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
package org.gradoop.storage.accumulo.impl.io;

import org.gradoop.common.model.impl.pojo.EPGMEdge;
import org.gradoop.common.model.impl.pojo.EPGMGraphHead;
import org.gradoop.common.model.impl.pojo.EPGMVertex;
import org.gradoop.flink.io.api.DataSink;
import org.gradoop.flink.model.impl.epgm.GraphCollection;
import org.gradoop.flink.model.impl.epgm.LogicalGraph;
import org.gradoop.flink.util.GradoopFlinkConfig;
import org.gradoop.storage.accumulo.impl.AccumuloEPGMStore;
import org.gradoop.storage.accumulo.impl.io.outputformats.ElementOutputFormat;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Write graph or graph collection into accumulo store
 */
public class AccumuloDataSink extends AccumuloBase implements DataSink {

  /**
   * Creates a new Accumulo data sink.
   *
   * @param store     store implementation
   * @param flinkConfig gradoop flink configuration
   */
  public AccumuloDataSink(
    @Nonnull AccumuloEPGMStore store,
    @Nonnull GradoopFlinkConfig flinkConfig
  ) {
    super(store, flinkConfig);
  }

  @Override
  public void write(LogicalGraph logicalGraph) throws IOException {
    write(logicalGraph, false);
  }

  @Override
  public void write(GraphCollection graphCollection) throws IOException {
    write(graphCollection, false);
  }

  @Override
  public void write(LogicalGraph logicalGraph, boolean overwrite) throws IOException {
    write(logicalGraph.getCollectionFactory().fromGraph(logicalGraph), overwrite);
  }

  @Override
  public void write(GraphCollection graphCollection, boolean overWrite) throws IOException {
    if (overWrite) {
      getStore().truncateTables();
    }
    graphCollection.getGraphHeads()
      .output(new ElementOutputFormat<>(EPGMGraphHead.class, getAccumuloConfig()));
    graphCollection.getVertices()
      .output(new ElementOutputFormat<>(EPGMVertex.class, getAccumuloConfig()));
    graphCollection.getEdges()
      .output(new ElementOutputFormat<>(EPGMEdge.class, getAccumuloConfig()));
  }

}
