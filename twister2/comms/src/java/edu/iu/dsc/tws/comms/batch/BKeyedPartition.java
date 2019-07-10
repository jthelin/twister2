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
package edu.iu.dsc.tws.comms.batch;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import edu.iu.dsc.tws.api.comms.BulkReceiver;
import edu.iu.dsc.tws.api.comms.Communicator;
import edu.iu.dsc.tws.api.comms.DestinationSelector;
import edu.iu.dsc.tws.api.comms.LogicalPlan;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageType;
import edu.iu.dsc.tws.api.comms.messaging.types.MessageTypes;
import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.comms.dfw.MToNSimple;
import edu.iu.dsc.tws.comms.dfw.io.partition.DPartitionBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionBatchFinalReceiver;
import edu.iu.dsc.tws.comms.dfw.io.partition.PartitionPartialReceiver;

public class BKeyedPartition {
  private MToNSimple partition;

  private DestinationSelector destinationSelector;

  public BKeyedPartition(Communicator comm, LogicalPlan plan,
                         Set<Integer> sources, Set<Integer> destinations,
                         MessageType keyType, MessageType dataType,
                         BulkReceiver rcvr, DestinationSelector destSelector, int edgeId) {
    this.destinationSelector = destSelector;
    this.partition = new MToNSimple(comm.getChannel(), sources, destinations,
        new PartitionBatchFinalReceiver(rcvr),
        new PartitionPartialReceiver(), dataType, keyType);
    this.partition.init(comm.getConfig(), dataType, plan, edgeId);
    this.destinationSelector.prepare(comm, partition.getSources(), partition.getTargets());
  }

  public BKeyedPartition(Communicator comm, LogicalPlan plan,
                         Set<Integer> sources, Set<Integer> destinations,
                         MessageType keyType, MessageType dataType,
                         BulkReceiver rcvr, DestinationSelector destSelector) {
    this(comm, plan, sources, destinations, keyType, dataType, rcvr, destSelector, comm.nextEdge());
  }

  public BKeyedPartition(Communicator comm, LogicalPlan plan,
                         Set<Integer> sources, Set<Integer> destinations,
                         MessageType dataType, MessageType keyType, BulkReceiver rcvr,
                         DestinationSelector destSelector, Comparator<Object> comparator) {
    this.destinationSelector = destSelector;
    List<String> shuffleDirs = comm.getPersistentDirectories();
    int e = comm.nextEdge();
    this.partition = new MToNSimple(comm.getConfig(), comm.getChannel(), plan,
        sources, destinations,
        new DPartitionBatchFinalReceiver(rcvr, true, shuffleDirs, comparator, true),
        new PartitionPartialReceiver(), dataType, MessageTypes.BYTE_ARRAY, keyType,
        keyType, e);
    this.partition.init(comm.getConfig(), dataType, plan, e);
    this.destinationSelector.prepare(comm, partition.getSources(), partition.getTargets());
  }

  public boolean partition(int source, Object key, Object message, int flags) {
    int destinations = destinationSelector.next(source, key, message);

    return partition.send(source, Tuple.of(key, message, partition.getKeyType(),
        partition.getDataType()), flags, destinations);
  }

  public boolean hasPending() {
    return !partition.isComplete();
  }

  public void finish(int source) {
    partition.finish(source);
  }

  public boolean progress() {
    return partition.progress();
  }

  public void close() {
    // deregister from the channel
    partition.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void reset() {
    partition.reset();
  }
}
