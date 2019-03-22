//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package edu.iu.dsc.tws.api.tset.sets;


import edu.iu.dsc.tws.api.task.ComputeConnection;
import edu.iu.dsc.tws.api.tset.FlatMapFunction;
import edu.iu.dsc.tws.api.tset.IterableFlatMapFunction;
import edu.iu.dsc.tws.api.tset.IterableMapFunction;
import edu.iu.dsc.tws.api.tset.MapFunction;
import edu.iu.dsc.tws.api.tset.Sink;
import edu.iu.dsc.tws.api.tset.TSetEnv;
import edu.iu.dsc.tws.api.tset.TSetUtils;
import edu.iu.dsc.tws.api.tset.link.BaseTLink;
import edu.iu.dsc.tws.api.tset.link.DirectTLink;
import edu.iu.dsc.tws.api.tset.ops.MapOp;
import edu.iu.dsc.tws.common.config.Config;

public class MapTSet<T, P> extends BaseTSet<T> {
  private BaseTLink<P> parent;

  private MapFunction<P, T> mapFn;

  public MapTSet(Config cfg, TSetEnv tSetEnv, BaseTLink<P> parent, MapFunction<P, T> mapFunc) {
    super(cfg, tSetEnv);
    this.parent = parent;
    this.mapFn = mapFunc;
    this.parallel = 1;
  }

  public MapTSet(Config cfg, TSetEnv tSetEnv, BaseTLink<P> parent,
                 MapFunction<P, T> mapFunc, int parallelism) {
    super(cfg, tSetEnv);
    this.parent = parent;
    this.mapFn = mapFunc;
    this.parallel = parallelism;
  }

  public <P1> MapTSet<P1, T> map(MapFunction<T, P1> mFn) {
    DirectTLink<T> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.map(mFn);
  }

  public <P1> FlatMapTSet<P1, T> flatMap(FlatMapFunction<T, P1> mFn) {
    DirectTLink<T> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.flatMap(mFn);
  }

  public <P1> IterableMapTSet<P1, T> map(IterableMapFunction<T, P1> mFn) {
    DirectTLink<T> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.map(mFn);
  }

  public <P1> IterableFlatMapTSet<P1, T> flatMap(IterableFlatMapFunction<T, P1> mFn) {
    DirectTLink<T> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.flatMap(mFn);
  }

  public SinkTSet<T> sink(Sink<T> sink) {
    DirectTLink<T> direct = new DirectTLink<>(config, tSetEnv, this);
    children.add(direct);
    return direct.sink(sink);
  }

  @SuppressWarnings("unchecked")
  public boolean baseBuild() {
    boolean isIterable = TSetUtils.isIterableInput(parent, tSetEnv.getTSetBuilder().getOpMode());
    boolean keyed = TSetUtils.isKeyedInput(parent);
    int p = calculateParallelism(parent);
    if (inputMap.size() > 0) {
      mapFn.addInputs(inputMap);
    }
    ComputeConnection connection = tSetEnv.getTSetBuilder().getTaskGraphBuilder().
        addCompute(generateName("map",
            parent), new MapOp<>(mapFn, isIterable, keyed), p);

    parent.buildConnection(connection);
    return true;
  }

  @Override
  public void buildConnection(ComputeConnection connection) {
    throw new IllegalStateException("Build connections should not be called on a TSet");
  }

  @Override
  public MapTSet<T, P> setName(String n) {
    this.name = n;
    return this;
  }
}