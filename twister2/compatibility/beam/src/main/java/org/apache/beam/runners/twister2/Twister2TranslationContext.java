/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.twister2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.beam.runners.core.construction.SerializablePipelineOptions;
import org.apache.beam.runners.core.construction.TransformInputs;
import org.apache.beam.runners.twister2.translators.functions.SideInputSinkFunction;
import org.apache.beam.runners.twister2.translators.functions.Twister2SinkFunction;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.runners.AppliedPTransform;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.util.WindowedValue;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PValue;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.beam.vendor.guava.v26_0_jre.com.google.common.collect.Iterables;

import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.tset.sets.TSet;
import edu.iu.dsc.tws.tset.env.TSetEnvironment;
import edu.iu.dsc.tws.tset.sets.batch.BBaseTSet;
import edu.iu.dsc.tws.tset.sets.batch.CachedTSet;

/**
 * Twister2TranslationContext.
 */
public class Twister2TranslationContext {
  private final Twister2PipelineOptions options;
  protected final Map<PValue, TSet<?>> dataSets = new LinkedHashMap<>();
  private final Set<TSet> leaves = new LinkedHashSet<>();
  private final Map<PCollectionView<?>, BBaseTSet<?>> sideInputDataSets;
  private final Map<PCollectionView<?>, SideInputSinkFunction> sideInputSinks;
  private AppliedPTransform<?, ?, ?> currentTransform;
  private final TSetEnvironment environment;
  private final Twister2RuntimeContext runtimeContext;
  private final SerializablePipelineOptions serializableOptions;

  public SerializablePipelineOptions getSerializableOptions() {
    return serializableOptions;
  }

  public Twister2TranslationContext(Twister2PipelineOptions options,
                                    Twister2RuntimeContext twister2RuntimeContext) {
    this.options = options;
    this.environment = options.getTSetEnvironment();
    this.sideInputDataSets = new HashMap<>();
    this.sideInputSinks = new HashMap<>();
    this.serializableOptions = new SerializablePipelineOptions(options);
    this.runtimeContext = twister2RuntimeContext;
  }

  @SuppressWarnings("unchecked")
  public <T extends PValue> T getOutput(PTransform<?, T> transform) {
    return (T) Iterables.getOnlyElement(currentTransform.getOutputs().values());
  }

  public PipelineOptions getOptions() {
    return options;
  }

  public <T> void setOutputDataSet(PCollection<T> output, TSet<WindowedValue<T>> tset) {
    if (!dataSets.containsKey(output)) {
      dataSets.put(output, tset);
      leaves.add(tset);
    }
  }

  public <T> TSet<WindowedValue<T>> getInputDataSet(PValue input) {
    TSet<WindowedValue<T>> tSet = (TSet<WindowedValue<T>>) dataSets.get(input);
    leaves.remove(tSet);
    return tSet;
  }

  public Twister2RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  public <T> Map<TupleTag<?>, PValue> getInputs() {
    return currentTransform.getInputs();
  }

  public <T extends PValue> T getInput(PTransform<T, ?> transform) {
    return (T) Iterables.getOnlyElement(TransformInputs.nonAdditionalInputs(currentTransform));
  }

  public void setCurrentTransform(AppliedPTransform<?, ?, ?> transform) {
    this.currentTransform = transform;
  }

  public AppliedPTransform<?, ?, ?> getCurrentTransform() {
    return currentTransform;
  }

  public Map<TupleTag<?>, PValue> getOutputs() {
    return getCurrentTransform().getOutputs();
  }

  public Map<TupleTag<?>, Coder<?>> getOutputCoders() {
    return currentTransform.getOutputs().entrySet().stream()
        .filter(e -> e.getValue() instanceof PCollection)
        .collect(Collectors.toMap(Map.Entry::getKey, e -> ((PCollection) e.getValue()).getCoder()));
  }

  public TSetEnvironment getEnvironment() {
    return environment;
  }

  public void execute() {
    for (Map.Entry<PCollectionView<?>, BBaseTSet<?>> sides : sideInputDataSets.entrySet()) {
      CachedTSet tempCache = sides.getValue().cache();
      DataPartition<?> centroidPartition = tempCache.getDataObject().getPartition(0);
      //sides.getValue().direct().sink(new SideInputSinkFunction(runtimeContext, sides.getKey()));
    }
    for (TSet leaf : leaves) {
      leaf.direct().sink(new Twister2SinkFunction());
    }
  }

  public <VT, ET> void setSideInputDataSet(
      PCollectionView<VT> value, BBaseTSet<WindowedValue<ET>> set) {
    if (!sideInputDataSets.containsKey(value)) {
      sideInputDataSets.put(value, set);
      //sideInputSinks.put(value, new SideInputSinkFunction<Object, VT>(runtimeContext, value));
    }
  }
}
