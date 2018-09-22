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
package edu.iu.dsc.tws.comms.dfw.io.reduce.keyed;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.ReduceFunction;
import edu.iu.dsc.tws.comms.dfw.io.KeyedContent;
import edu.iu.dsc.tws.comms.dfw.io.KeyedReceiver;

/**
 * Keyed reduce receiver for batch mode
 */
public class KReduceBatchPartialReceiver extends KReduceReceiver {
  private static final Logger LOG = Logger.getLogger(KReduceBatchPartialReceiver.class.getName());


  public KReduceBatchPartialReceiver(int dest, ReduceFunction function) {
    this.reduceFunction = function;
    this.destination = dest;
    this.limitPerKey = 1;
  }

  @Override
  public boolean progress() {
    boolean needsFurtherProgress = false;
    boolean sourcesFinished = false;
    boolean isAllQueuesEmpty = false;
    for (int target : messages.keySet()) {

      //If the batch is done skip progress for this target
      if (batchDone.get(target)) {
        needsFurtherProgress = !checkIfEmptyIsSent(target);
        continue;
      }

      // now check weather we have the messages for this source to be sent
      BlockingQueue<Object> targetSendQueue = sendQueue.get(target);
      sourcesFinished = isSourcesFinished(target);

      if (!sourcesFinished) {
        needsFurtherProgress = true;
      }

      if (!targetSendQueue.isEmpty() || sourcesFinished) {
        int flags = 0;

        //In reduce a flush is only needed when the number of keys exceed the key limit
        //So we flush the keys and remove them from the messages list.This would be the same
        //when the sources are finished.
        Object current = targetSendQueue.peek();
        if (current != null) {
          if (sourcesFinished && targetSendQueue.size() == 1) {
            flags = MessageFlags.LAST;
          }

          if (dataFlowOperation.sendPartial(target, current, flags, destination)) {
            targetSendQueue.poll();
          } else {
            needsFurtherProgress = true;
          }

        }
      }

      //In reduce since we remove the key entry once we send it we only need to check if the map is
      //Empty
      isAllQueuesEmpty = targetSendQueue.isEmpty();
      if (!isAllQueuesEmpty) {
        needsFurtherProgress = true;
      }

      if (dataFlowOperation.isDelegeteComplete() && sourcesFinished && isAllQueuesEmpty) {
        if (dataFlowOperation.sendPartial(target, new byte[0],
            MessageFlags.END, destination)) {
          isEmptySent.put(target, true);
        } else {
          needsFurtherProgress = true;
        }
        batchDone.put(target, true);
        // we don'target want to go through the while loop for this one
        break;
      } else {
        needsFurtherProgress = true;
      }
    }

    return needsFurtherProgress;
  }


  /**
   * checks if the Empty message was sent for this target and sends it if not sent and possible to
   * send
   *
   * @param target target for which the check is done
   * @return false if Empty is sent
   */
  private boolean checkIfEmptyIsSent(int target) {
    boolean isSent = true;
    if (!isEmptySent.get(target)) {
      if (dataFlowOperation.isDelegeteComplete() && dataFlowOperation.sendPartial(target,
          new byte[0], MessageFlags.END, destination)) {
        isEmptySent.put(target, true);
      } else {
        isSent = false;
      }
    }
    return isSent;
  }

}
