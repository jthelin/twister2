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
package edu.iu.dsc.tws.executor.threading.ft;

import edu.iu.dsc.tws.api.comms.channel.TWSChannel;
import edu.iu.dsc.tws.api.compute.executor.ExecutionPlan;
import edu.iu.dsc.tws.api.compute.executor.IExecutionHook;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.exceptions.Twister2Exception;
import edu.iu.dsc.tws.api.faulttolerance.Fault;
import edu.iu.dsc.tws.executor.threading.BatchSharingExecutor;

public class DedidatedBatchExecutor extends BatchSharingExecutor {
  private boolean isError = false;

  public DedidatedBatchExecutor(Config cfg, int workerId, TWSChannel channel,
                                ExecutionPlan plan, IExecutionHook hook) {
    super(cfg, workerId, channel, plan, hook);
  }

  @Override
  public boolean isNotStopped() {
    return notStopped && !isError;
  }

  @Override
  public void onFault(Fault fault) throws Twister2Exception {
    isError = true;
  }
}
