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

import java.util.Iterator;
import java.util.Set;

import edu.iu.dsc.tws.api.comms.BulkReceiver;
import edu.iu.dsc.tws.api.comms.Communicator;
import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.task.IMessage;
import edu.iu.dsc.tws.api.task.TaskMessage;
import edu.iu.dsc.tws.api.task.graph.Edge;
import edu.iu.dsc.tws.comms.stream.SGather;
import edu.iu.dsc.tws.executor.comms.AbstractParallelOperation;

public class GatherStreamingOperation extends AbstractParallelOperation {
  private SGather op;

  public GatherStreamingOperation(Config config, Communicator network, LogicalPlan tPlan,
                                  Set<Integer> srcs, Set<Integer> dests, Edge edge) {
    super(config, network, tPlan, edge.getName());

    if (srcs.size() == 0) {
      throw new IllegalArgumentException("Sources should have more than 0 elements");
    }

    if (dests.size() > 1) {
      throw new RuntimeException("Gather can only have one target: " + dests);
    }

    Communicator newComm = channel.newWithConfig(edge.getProperties());
    op = new SGather(newComm, logicalPlan, srcs, dests.iterator().next(),
        edge.getDataType(), new GatherRcvr(), edge.getEdgeID().nextId());
  }

  @Override
  public boolean send(int source, IMessage message, int flags) {
    return op.gather(source, message.getContent(), flags);
  }

  @Override
  public boolean progress() {
    return op.progress();
  }


  private class GatherRcvr implements BulkReceiver {
    @Override
    public void init(Config cfg, Set<Integer> targets) {

    }

    @Override
    public boolean receive(int target, Iterator<Object> it) {
      TaskMessage msg = new TaskMessage<>(it, inEdge, target);
      return outMessages.get(target).offer(msg);
    }
  }

  @Override
  public void close() {
    op.close();
  }

  @Override
  public void reset() {
    op.reset();
  }

  @Override
  public boolean isComplete() {
    return !op.hasPending();
  }
}
