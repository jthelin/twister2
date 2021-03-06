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
package edu.iu.dsc.tws.api.comms;

import java.util.HashMap;
import java.util.Map;

import edu.iu.dsc.tws.api.comms.channel.TWSChannel;
import edu.iu.dsc.tws.api.comms.messaging.MessageFlags;

public abstract class BaseOperation {
  /**
   * The actual operation
   */
  protected DataFlowOperation op;

  /**
   * THe underlying channel
   */
  protected TWSChannel channel;

  /**
   * Create the base operation
   */
  public BaseOperation(Communicator comm, boolean stream, String opName) {
    Map<String, Object> newConfigs = new HashMap<>();
    newConfigs.put(CommunicationContext.STREAMING, stream);
    newConfigs.put(CommunicationContext.OPERATION_NAME, opName);
    comm.updateConfig(newConfigs);
    this.channel = comm.getChannel();
  }

  /**
   * Weather we have messages pending
   *
   * @return true if there are messages pending
   */
  public boolean isComplete() {
    return op.isComplete();
  }

  /**
   * Indicate the end of the communication
   *
   * @param src the source that is ending
   */
  public void finish(int src) {
    op.finish(src);
  }

  /**
   * Progress the operation, if not called, messages will not be processed
   *
   * @return true if further progress is needed
   */
  public boolean progress() {
    return op.progress();
  }

  public void close() {
    // deregister from the channel
    op.close();
  }

  /**
   * Clean the operation, this doesn't close it
   */
  public void reset() {
    op.reset();
  }

  /**
   * Progress the channel and the operation
   *
   * @return true if further progress is required
   */
  public boolean progressChannel() {
    boolean p = op.progress();
    channel.progress();

    return p;
  }

  public boolean sendBarrier(int src, byte[] barrierId) {
    return this.op.send(src, barrierId, MessageFlags.SYNC_BARRIER);
  }

  /**
   * Wait for the operation to complete
   */
  public void waitForCompletion() {
    while (!op.isComplete()) {
      op.progress();
    }
  }
}
