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
package edu.iu.dsc.tws.executor.core.streaming;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.executor.api.INodeInstance;
import edu.iu.dsc.tws.executor.api.IParallelOperation;
import edu.iu.dsc.tws.executor.core.DefaultOutputCollection;
import edu.iu.dsc.tws.executor.core.ExecutorContext;
import edu.iu.dsc.tws.executor.core.TaskContextImpl;
import edu.iu.dsc.tws.task.api.Closable;
import edu.iu.dsc.tws.task.api.IMessage;
import edu.iu.dsc.tws.task.api.INode;
import edu.iu.dsc.tws.task.api.ISource;
import edu.iu.dsc.tws.task.api.OutputCollection;
import edu.iu.dsc.tws.tsched.spi.taskschedule.TaskSchedulePlan;

public class SourceStreamingInstance implements INodeInstance {
  /**
   * The actual streamingTask executing
   */
  private ISource streamingTask;

  /**
   * Output will go throuh a single queue
   */
  private BlockingQueue<IMessage> outStreamingQueue;

  /**
   * The configuration
   */
  private Config config;

  /**
   * The output collection to be used
   */
  private OutputCollection outputStreamingCollection;

  /**
   * Parallel operations
   */
  private Map<String, IParallelOperation> outStreamingParOps = new HashMap<>();

  /**
   * The globally unique streamingTask id
   */
  private int globalTaskId;

  /**
   * The task id
   */
  private int taskId;

  /**
   * Task index that goes from 0 to parallism - 1
   */
  private int streamingTaskIndex;

  /**
   * Number of parallel tasks
   */
  private int parallelism;

  /**
   * Name of the streamingTask
   */
  private String taskName;

  /**
   * Node configurations
   */
  private Map<String, Object> nodeConfigs;

  /**
   * Worker id
   */
  private int workerId;

  /**
   * The low watermark for queued messages
   */
  private int lowWaterMark;

  /**
   * The high water mark for messages
   */
  private int highWaterMark;

  /**
   * The output edges
   */
  private Map<String, String> outEdges;
  private TaskSchedulePlan taskSchedule;

  /**
   * Keep an array for iteration
   */
  private IParallelOperation[] outOpArray;

  /**
   * Keep an array out out edges for iteration
   */
  private String[] outEdgeArray;

  public SourceStreamingInstance(ISource streamingTask, BlockingQueue<IMessage> outStreamingQueue,
                                 Config config, String tName, int taskId,
                                 int globalTaskId, int tIndex, int parallel,
                                 int wId, Map<String, Object> cfgs, Map<String, String> outEdges,
                                 TaskSchedulePlan taskSchedule) {
    this.streamingTask = streamingTask;
    this.taskId = taskId;
    this.outStreamingQueue = outStreamingQueue;
    this.config = config;
    this.globalTaskId = globalTaskId;
    this.streamingTaskIndex = tIndex;
    this.parallelism = parallel;
    this.taskName = tName;
    this.nodeConfigs = cfgs;
    this.workerId = wId;
    this.lowWaterMark = ExecutorContext.instanceQueueLowWaterMark(config);
    this.highWaterMark = ExecutorContext.instanceQueueHighWaterMark(config);
    this.outEdges = outEdges;
    this.taskSchedule = taskSchedule;
  }

  public void prepare(Config cfg) {
    outputStreamingCollection = new DefaultOutputCollection(outStreamingQueue);

    streamingTask.prepare(cfg, new TaskContextImpl(streamingTaskIndex, taskId,
        globalTaskId, taskName, parallelism, workerId,
        outputStreamingCollection, nodeConfigs, outEdges, taskSchedule));

    /// we will use this array for iteration
    this.outOpArray = new IParallelOperation[outStreamingParOps.size()];
    int index = 0;
    for (Map.Entry<String, IParallelOperation> e : outStreamingParOps.entrySet()) {
      this.outOpArray[index++] = e.getValue();
    }

    this.outEdgeArray = new String[outEdges.size()];
    index = 0;
    for (String e : outEdges.keySet()) {
      this.outEdgeArray[index++] = e;
    }
  }

  /**
   * Execution Method calls the SourceTasks run method to get context
   **/
  public boolean execute() {
    if (outStreamingQueue.size() < lowWaterMark) {
      // lets execute the task
      streamingTask.execute();
    }
    // now check the output queue
    while (!outStreamingQueue.isEmpty()) {
      IMessage message = outStreamingQueue.peek();
      if (message != null) {
        String edge = message.edge();
        IParallelOperation op = outStreamingParOps.get(edge);
        // if we successfully send remove message
        if (op.send(globalTaskId, message, 0)) {
          outStreamingQueue.poll();
        } else {
          // we need to break
          break;
        }
      } else {
        break;
      }
    }

    for (int i = 0; i < outOpArray.length; i++) {
      IParallelOperation op = outOpArray[i];
      op.progress();
    }

    return true;
  }

  @Override
  public INode getNode() {
    return streamingTask;
  }

  @Override
  public void close() {
    if (streamingTask instanceof Closable) {
      ((Closable) streamingTask).close();
    }
  }

  public BlockingQueue<IMessage> getOutStreamingQueue() {
    return outStreamingQueue;
  }

  public void registerOutParallelOperation(String edge, IParallelOperation op) {
    outStreamingParOps.put(edge, op);
  }
}
