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
package edu.iu.dsc.tws.executor.comms.streaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.DataFlowOperation;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.MessageReceiver;
import edu.iu.dsc.tws.comms.core.TaskPlan;
import edu.iu.dsc.tws.comms.dfw.DataFlowPartition;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionPartialReceiver;
import edu.iu.dsc.tws.comms.op.Communicator;
import edu.iu.dsc.tws.data.api.DataType;
import edu.iu.dsc.tws.executor.api.AbstractParallelOperation;
import edu.iu.dsc.tws.executor.api.EdgeGenerator;
import edu.iu.dsc.tws.executor.util.Utils;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.TaskMessage;

public class PartitionStreamingOperation extends AbstractParallelOperation {
  private static final Logger LOG = Logger.getLogger(PartitionStreamingOperation.class.getName());
  private HashMap<Integer, Boolean> barrierMap = new HashMap<>();

  private HashMap<Integer, ArrayList<Object>> incommingBuffer = new HashMap<>();
  private boolean checkpointStarted = false;

  protected DataFlowPartition op;

  public PartitionStreamingOperation(Config config, Communicator network, TaskPlan tPlan) {
    super(config, network, tPlan);
  }

  public void prepare(Set<Integer> srcs, Set<Integer> dests, EdgeGenerator e,
                      DataType dataType, String edgeName) {
    this.edge = e;
    //LOG.info("ParitionOperation Prepare 1");
    op = new DataFlowPartition(channel.getChannel(), srcs, dests, new PartitionReceiver(),
        new PartitionPartialReceiver(), DataFlowPartition.PartitionStratergy.DIRECT);
    communicationEdge = e.generate(edgeName);
    op.init(config, Utils.dataTypeToMessageType(dataType), taskPlan, communicationEdge);
  }

  public boolean send(int source, IMessage message, int flags) {
    return op.send(source, message.getContent(), flags);
  }

  public class PartitionReceiver implements MessageReceiver {
    @Override
    public void init(Config cfg, DataFlowOperation operation,
                     Map<Integer, List<Integer>> expectedIds) {

    }

    @Override
    public boolean onMessage(int source, int path, int target, int flags, Object object) {
      if ((flags & MessageFlags.BARRIER) == MessageFlags.BARRIER) {
        if (!checkpointStarted) {
          checkpointStarted = true;
        }
        barrierMap.putIfAbsent(source, true);
        if (barrierMap.keySet() == op.getSources()) {
          op.getDestinations();
          //start checkpoint and flush the buffering messages
        }
      } else {
        if (barrierMap.containsKey(source)) {
          if (incommingBuffer.containsKey(source)) {
            incommingBuffer.get(source).add(object);
          } else {
            ArrayList<Object> bufferMessege = new ArrayList<>();
            bufferMessege.add(object);
            incommingBuffer.put(source, bufferMessege);
          }
        } else {
          if (object instanceof List) {
            for (Object o : (List) object) {
              TaskMessage msg = new TaskMessage(o,
                  edge.getStringMapping(communicationEdge), target);
              outMessages.get(target).offer(msg);

            }
          }
        }
      }
      return true;
    }

    @Override
    public void onFinish(int source) {

    }

    @Override
    public boolean progress() {
      return true;
    }
  }

  public boolean progress() {
    return op.progress();
  }
}