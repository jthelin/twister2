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
package edu.iu.dsc.tws.examples.basic.comms.batch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.BatchReceiver;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.core.TaskPlan;
  import edu.iu.dsc.tws.comms.op.SimpleKeyBasedPartitionSelector;
import edu.iu.dsc.tws.comms.op.batch.BKeyedPartition;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.examples.basic.KeyedBenchWorker;

/**
 * This class is a example use of the keyed partition function
 */
public class BKeyedPartitionExample extends KeyedBenchWorker {

  private static final Logger LOG = Logger.getLogger(BKeyedPartitionExample.class.getName());

  private BKeyedPartition partition;

  private boolean partitionDone = false;

  @Override
  protected void execute() {
    TaskPlan taskPlan = Utils.createStageTaskPlan(config, resourcePlan,
        jobParameters.getTaskStages());

    Set<Integer> sources = new HashSet<>();
    Set<Integer> targets = new HashSet<>();
    Integer noOfSourceTasks = jobParameters.getTaskStages().get(0);
    for (int i = 0; i < noOfSourceTasks; i++) {
      sources.add(i);
    }
    Integer noOfTargetTasks = jobParameters.getTaskStages().get(1);
    for (int i = 0; i < noOfTargetTasks; i++) {
      targets.add(noOfSourceTasks + i);
    }

    // create the communication
    partition = new BKeyedPartition(communicator, taskPlan, sources, targets, MessageType.INTEGER,
        MessageType.INTEGER, new PartitionReceiver(), new SimpleKeyBasedPartitionSelector());

    Set<Integer> tasksOfExecutor = Utils.getTasksOfExecutor(workerId, taskPlan,
        jobParameters.getTaskStages(), 0);
    // now initialize the workers
    for (int t : tasksOfExecutor) {
      // the map thread where data is produced
      Thread mapThread = new Thread(new KeyedBenchWorker.MapWorker(t));
      mapThread.start();
    }
  }

  @Override
  protected void progressCommunication() {

  }

  @Override
  protected boolean isDone() {
    return false;
  }

  @Override
  protected boolean sendMessages(int task, Object key, Object data, int flag) {
    return false;
  }

  public class PartitionReceiver implements BatchReceiver {
    private int count = 0;
    private int expected;

    @Override
    public void init(Config cfg, DataFlowOperation op, Map<Integer, List<Integer>> expectedIds) {
      expected = expectedIds.keySet().size() * jobParameters.getIterations();
    }

    @Override
    public void receive(int target, Iterator<Object> it) {
      LOG.log(Level.INFO, String.format("%d Received message %d count %d expected %d",
          workerId, target, count, expected));
      partitionDone = true;
    }
  }
}