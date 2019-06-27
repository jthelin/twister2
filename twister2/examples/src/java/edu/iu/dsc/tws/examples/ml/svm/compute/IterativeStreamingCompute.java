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
package edu.iu.dsc.tws.examples.ml.svm.compute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.dataset.DataPartition;
import edu.iu.dsc.tws.api.task.IFunction;
import edu.iu.dsc.tws.api.task.IMessage;
import edu.iu.dsc.tws.api.task.graph.OperationMode;
import edu.iu.dsc.tws.api.task.nodes.BaseSink;
import edu.iu.dsc.tws.dataset.impl.EntityPartition;
import edu.iu.dsc.tws.examples.ml.svm.integration.test.ICollector;

public class IterativeStreamingCompute extends BaseSink<double[]> implements ICollector<double[]> {
  private static final long serialVersionUID = 332173590941256461L;
  private static final Logger LOG = Logger.getLogger(IterativeStreamingCompute.class.getName());
  private List<double[]> aggregatedModels = new ArrayList<>();

  private double[] newWeightVector;

  private boolean debug = false;

  private OperationMode operationMode;

  private IFunction<double[]> reduceFn;

  private int evaluationInterval = 10;

  public IterativeStreamingCompute(OperationMode operationMode) {
    this.operationMode = operationMode;
  }

  public IterativeStreamingCompute(OperationMode operationMode, IFunction<double[]> reduceFn) {
    this.operationMode = operationMode;
    this.reduceFn = reduceFn;
  }

  @Override
  public DataPartition<double[]> get() {
    return new EntityPartition<>(context.taskIndex(), newWeightVector);
  }

  @Override
  public boolean execute(IMessage<double[]> message) {
    if (message.getContent() == null) {
      LOG.info("Something Went Wrong !!!");
    } else {
      if (debug) {
        LOG.info(String.format("Received Sink Value : %d, %s, %f", this.newWeightVector.length,
            Arrays.toString(this.newWeightVector), this.newWeightVector[0]));
      }
      this.newWeightVector = message.getContent();
      aggregatedModels.add(this.newWeightVector);
      if (aggregatedModels.size() % evaluationInterval == 0) {
        double[] w = new double[this.aggregatedModels.get(0).length];
        int size = aggregatedModels.size();
        for (int i = 0; i < size; i++) {
          w = reduceFn.onMessage(w, aggregatedModels.get(i));
        }
        LOG.info(String.format("Evaluation TimeStamp [%d] Model : %s", size, Arrays.toString(w)));
      }
    }
    return true;
  }


}
