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
package edu.iu.dsc.tws.examples.task.batch;

import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.comms.Op;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.compute.TaskContext;
import edu.iu.dsc.tws.api.compute.nodes.BaseSource;
import edu.iu.dsc.tws.api.compute.nodes.ICompute;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.examples.task.BenchTaskWorker;
import edu.iu.dsc.tws.examples.task.batch.verifiers.ReduceVerifier;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkConstants;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkUtils;
import edu.iu.dsc.tws.examples.utils.bench.Timing;
import edu.iu.dsc.tws.examples.verification.ResultsVerifier;
import edu.iu.dsc.tws.task.impl.ComputeGraphBuilder;
import edu.iu.dsc.tws.task.typed.AllReduceCompute;

public class BTAllReduceExample extends BenchTaskWorker {
  private static final Logger LOG = Logger.getLogger(BTAllReduceExample.class.getName());

  @Override
  public ComputeGraphBuilder buildTaskGraph() {
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);

    String edge = "edge";
    BaseSource g = new SourceTask(edge);
    ICompute r = new AllReduceSinkTask();

    computeGraphBuilder.addSource(SOURCE, g, sourceParallelism);
    computeConnection = computeGraphBuilder.addCompute(SINK, r, sinkParallelism);
    computeConnection.allreduce(SOURCE)
        .viaEdge(edge)
        .withOperation(Op.SUM, MessageTypes.INTEGER_ARRAY);
    return computeGraphBuilder;
  }

  protected static class AllReduceSinkTask extends AllReduceCompute<int[]> {
    private static final long serialVersionUID = -254264903510284798L;
    private boolean timingCondition;

    private ResultsVerifier<int[], int[]> resultsVerifier;
    private boolean verified = true;

    @Override
    public void prepare(Config cfg, TaskContext ctx) {
      super.prepare(cfg, ctx);
      this.timingCondition = getTimingCondition(SINK, context);
      resultsVerifier = new ReduceVerifier(inputDataArray, ctx, SOURCE, jobParameters);
    }

    @Override
    public boolean allReduce(int[] content) {
      Timing.mark(BenchmarkConstants.TIMING_ALL_RECV, this.timingCondition);
      LOG.info(String.format("%d received allreduce %d", context.getWorkerId(),
          context.globalTaskId()));
      BenchmarkUtils.markTotalTime(resultsRecorder, this.timingCondition);
      resultsRecorder.writeToCSV();
      this.verified = verifyResults(resultsVerifier, content, null, verified);
      return true;
    }
  }
}
