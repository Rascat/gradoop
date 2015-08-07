package org.gradoop.model.impl;

import org.apache.flink.api.common.functions.CrossFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.graph.Edge;
import org.apache.flink.graph.Graph;
import org.apache.flink.graph.Vertex;
import org.apache.flink.util.Collector;
import org.gradoop.model.helper.LongSetFromVertexFunction;
import org.gradoop.model.operators.UnaryGraphToCollectionOperator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Todo: Description
 */
public class OverlapSplitBy implements UnaryGraphToCollectionOperator,
  Serializable {
  ExecutionEnvironment env;
  final LongSetFromVertexFunction function;

  public OverlapSplitBy(LongSetFromVertexFunction function,
    ExecutionEnvironment env) {
    this.env = env;
    this.function = function;
  }

  @Override
  public EPGraphCollection execute(EPGraph epGraph) {
    final Graph graph = epGraph.getGellyGraph();
    DataSet<Vertex<Long, EPFlinkVertexData>> vertices = graph.getVertices();
    vertices = vertices.map(new AddNewGraphsToVertexMapper(function));
    DataSet<Tuple1<Long>> newSubgraphIDs =
      vertices.flatMap(new VertexToGraphIDFlatMapper(function)).distinct();
    DataSet<Subgraph<Long, EPFlinkGraphData>> newSubgraphs =
      newSubgraphIDs.map(new SubgraphMapper());
    DataSet<Tuple3<Long, Long, Long>> edgeVertexVertex = graph.getEdges().map(
      new MapFunction<Edge<Long, EPFlinkEdgeData>, Tuple3<Long, Long, Long>>() {
        @Override
        public Tuple3<Long, Long, Long> map(
          Edge<Long, EPFlinkEdgeData> edge) throws Exception {
          return new Tuple3<>(edge.getValue().getId(),
            edge.getValue().getSourceVertex(),
            edge.getValue().getTargetVertex());
        }
      });
    DataSet<Tuple3<Long, Set<Long>, Long>> edgeGraphsVertex =
      edgeVertexVertex.join(vertices).where(1).equalTo(0).with(
        new JoinFunction<Tuple3<Long, Long, Long>, Vertex<Long,
          EPFlinkVertexData>, Tuple3<Long, Set<Long>, Long>>() {
          @Override
          public Tuple3<Long, Set<Long>, Long> join(
            Tuple3<Long, Long, Long> tuple3,
            Vertex<Long, EPFlinkVertexData> vertex) throws Exception {
            return new Tuple3<>(tuple3.f0, vertex.getValue().getGraphs(),
              tuple3.f2);
          }
        });
    //replace the target vertex id by the graph list of this vertex
    DataSet<Tuple3<Long, Set<Long>, Set<Long>>> edgeGraphsGraphs =
      edgeGraphsVertex.join(vertices).where(2).equalTo(0).with(
        new JoinFunction<Tuple3<Long, Set<Long>, Long>, Vertex<Long,
          EPFlinkVertexData>, Tuple3<Long, Set<Long>, Set<Long>>>() {
          @Override
          public Tuple3<Long, Set<Long>, Set<Long>> join(
            Tuple3<Long, Set<Long>, Long> tuple3,
            Vertex<Long, EPFlinkVertexData> vertex) throws Exception {
            return new Tuple3<>(tuple3.f0, tuple3.f1,
              vertex.getValue().getGraphs());
          }
        });
    //transform the new subgraphs into a single set of long, containing all
    // the identifiers
    DataSet<Set<Long>> newSubgraphIdentifiers = newSubgraphs
      .map(new MapFunction<Subgraph<Long, EPFlinkGraphData>, Set<Long>>() {
        @Override
        public Set<Long> map(Subgraph<Long, EPFlinkGraphData> subgraph) throws
          Exception {
          Set<Long> id = new HashSet<Long>();
          id.add(subgraph.getId());
          return id;
        }
      }).reduce(new ReduceFunction<Set<Long>>() {
        @Override
        public Set<Long> reduce(Set<Long> set1, Set<Long> set2) throws
          Exception {
          set1.addAll(set2);
          return set1;
        }
      });
    //construct new tuples containing the edge, the graphs of its source and
    //target vertex and the list of new graphs
    DataSet<Tuple4<Long, Set<Long>, Set<Long>, Set<Long>>> edgesWithSubgraphs =
      edgeGraphsGraphs.crossWithTiny(newSubgraphIdentifiers).with(
        new CrossFunction<Tuple3<Long, Set<Long>, Set<Long>>, Set<Long>,
          Tuple4<Long, Set<Long>, Set<Long>, Set<Long>>>() {
          @Override
          public Tuple4<Long, Set<Long>, Set<Long>, Set<Long>> cross(
            Tuple3<Long, Set<Long>, Set<Long>> tuple3,
            Set<Long> subgraphs) throws Exception {
            return new Tuple4<>(tuple3.f0, tuple3.f1, tuple3.f2, subgraphs);
          }
        });
    //remove all edges which source and target are not in at least one common
    // graph
    DataSet<Tuple2<Long, Set<Long>>> subgraphs = edgesWithSubgraphs.flatMap(
      new FlatMapFunction<Tuple4<Long, Set<Long>, Set<Long>, Set<Long>>,
        Tuple2<Long, Set<Long>>>() {
        @Override
        public void flatMap(
          Tuple4<Long, Set<Long>, Set<Long>, Set<Long>> tuple4,
          Collector<Tuple2<Long, Set<Long>>> collector) throws Exception {
          Set<Long> sourceGraphs = tuple4.f1;
          Set<Long> targetGraphs = tuple4.f2;
          Set<Long> newSubgraphs = tuple4.f3;
          boolean newGraphAdded = false;
          Set<Long> toBeAddedGraphs = new HashSet<Long>();
          for (Long graph : newSubgraphs) {
            if (targetGraphs.contains(graph) && sourceGraphs.contains(graph)) {
              toBeAddedGraphs.add(graph);
              newGraphAdded = true;
            }
          }
          if (newGraphAdded) {
            collector.collect(new Tuple2<>(tuple4.f0, toBeAddedGraphs));
          }
        }
      });
    //join the graph set tuples with the edges, add all new graphs to the
    //edge graph sets
    DataSet<Edge<Long, EPFlinkEdgeData>> edges =
      graph.getEdges().join(subgraphs).where(new EdgeKeySelector())
        .equalTo(0).with(
        new JoinFunction<Edge<Long, EPFlinkEdgeData>, Tuple2<Long,
          Set<Long>>, Edge<Long, EPFlinkEdgeData>>() {
          @Override
          public Edge<Long, EPFlinkEdgeData> join(
            Edge<Long, EPFlinkEdgeData> edge,
            Tuple2<Long, Set<Long>> tuple2) throws Exception {
            return edge;
          }
        });
    Graph<Long, EPFlinkVertexData, EPFlinkEdgeData> newGraph =
      Graph.fromDataSet(vertices, edges, env);
    return new EPGraphCollection(newGraph, newSubgraphs, env);
  }

  /**
   * adds the graph ids extracted by the LongFromVertexFunction to the
   * vertex graph set
   */
  private static class AddNewGraphsToVertexMapper implements
    MapFunction<Vertex<Long, EPFlinkVertexData>, Vertex<Long,
      EPFlinkVertexData>> {
    private LongSetFromVertexFunction function;

    public AddNewGraphsToVertexMapper(LongSetFromVertexFunction function) {
      this.function = function;
    }

    @Override
    public Vertex<Long, EPFlinkVertexData> map(
      Vertex<Long, EPFlinkVertexData> vertex) throws Exception {
      Set<Long> labelPropIndex = function.extractLongSet(vertex);
      vertex.getValue().getGraphs().addAll(labelPropIndex);
      return vertex;
    }
  }

  private static class VertexToGraphIDFlatMapper implements
    FlatMapFunction<Vertex<Long, EPFlinkVertexData>, Tuple1<Long>> {
    LongSetFromVertexFunction function;

    public VertexToGraphIDFlatMapper(LongSetFromVertexFunction function) {
      this.function = function;
    }

    @Override
    public void flatMap(Vertex<Long, EPFlinkVertexData> vertex,
      Collector<Tuple1<Long>> collector) throws Exception {
      Set<Long> graphIDSet = function.extractLongSet(vertex);
      for (Long id : graphIDSet) {
        collector.collect(new Tuple1<>(id));
      }
    }
  }

  private static class SubgraphMapper implements
    MapFunction<Tuple1<Long>, Subgraph<Long, EPFlinkGraphData>> {
    @Override
    public Subgraph<Long, EPFlinkGraphData> map(Tuple1<Long> idTuple) throws
      Exception {
      Long id = idTuple.f0;
      EPFlinkGraphData subgraphData =
        new EPFlinkGraphData(id, "split graph " + id);
      return new Subgraph<>(id, subgraphData);
    }
  }

  /**
   * Used for distinction of edges based on their unique id.
   */
  private static class EdgeKeySelector implements
    KeySelector<Edge<Long, EPFlinkEdgeData>, Long> {
    @Override
    public Long getKey(
      Edge<Long, EPFlinkEdgeData> longEPFlinkEdgeDataEdge) throws Exception {
      return longEPFlinkEdgeDataEdge.getValue().getId();
    }
  }

  @Override
  public String getName() {
    return "OverlapSplitBy";
  }
}
