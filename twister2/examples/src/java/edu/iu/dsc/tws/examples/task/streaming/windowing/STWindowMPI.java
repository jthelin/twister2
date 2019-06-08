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
package edu.iu.dsc.tws.examples.task.streaming.windowing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.data.api.DataType;
import edu.iu.dsc.tws.examples.task.BenchTaskWorker;
import edu.iu.dsc.tws.examples.utils.math.MathUtils;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.window.BaseWindowSource;
import edu.iu.dsc.tws.task.api.window.api.IWindowMessage;
import edu.iu.dsc.tws.task.api.window.core.BaseWindowedSink;

import mpi.MPI;
import mpi.MPIException;

public class STWindowMPI extends BenchTaskWorker {

  private static final Logger LOG = Logger.getLogger(STWindowMPI.class.getName());

  private static int worldRank = 0;

  private static int worldSize = 0;

  @Override
  public TaskGraphBuilder buildTaskGraph() {
    List<Integer> taskStages = jobParameters.getTaskStages();
    int sourceParallelism = taskStages.get(0);
    int sinkParallelism = taskStages.get(1);

    String edge = "edge";
    BaseWindowSource g = new SourceWindowTask(edge);

    // Tumbling Window
    BaseWindowedSink dw = new DirectWindowedReceivingTask()
        .withTumblingCountWindow(1);

    taskGraphBuilder.addSource(SOURCE, g, sourceParallelism);
    computeConnection = taskGraphBuilder.addSink(SINK, dw, sinkParallelism);
    computeConnection.direct(SOURCE).viaEdge(edge).withDataType(DataType.INTEGER_ARRAY);

    return taskGraphBuilder;
  }

  protected static class DirectWindowedReceivingTask extends BaseWindowedSink<int[]> {

    public DirectWindowedReceivingTask() {
    }

    /**
     * This method returns the final windowing message
     *
     * @param windowMessage Aggregated IWindowMessage is obtained here
     * windowMessage contains [expired-tuples, current-tuples]
     */
    @Override
    public boolean execute(IWindowMessage<int[]> windowMessage) {
      LOG.info(String.format("Items : %d ", windowMessage.getWindow().size()));


      try {
        worldRank = MPI.COMM_WORLD.getRank();
        worldSize = MPI.COMM_WORLD.getSize();
        List<IMessage<int[]>> messages = windowMessage.getWindow();
        List<int[]> newMessages = new ArrayList<>(messages.size());
        for (IMessage<int[]> msg : messages) {
          int[] m = msg.getContent();
          newMessages.add(m);
        }
        int[] res = MathUtils.sumList(newMessages);
        Arrays.fill(res, context.taskIndex());
        LOG.info(String.format("Win Size : [%d] ,Rank[%d], Worker Id[%d] , Before Reduce : "
                + "Array = %s", newMessages.size(), worldRank, context.getWorkerId(),
            Arrays.toString(res)));

        int[] globalSum = new int[res.length];
        MPI.COMM_WORLD.reduce(res, globalSum, res.length, MPI.INT, MPI.SUM, 0);

        if (worldRank == 0) {
          LOG.info(String.format("Rank[%d], Worker Id[%d] , After Reduce : "
              + "Array = %s", worldRank, context.getWorkerId(), Arrays.toString(globalSum)));
        }

      } catch (MPIException e) {
        e.printStackTrace();
      }
      //LOG.info(String.format("World Rank [%d], World Size [%d]", worldRank, worldSize));
      return true;
    }

  }
}
