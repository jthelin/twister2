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
package edu.iu.dsc.tws.examples.comms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.net.Network;
import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.common.controller.IWorkerController;
import edu.iu.dsc.tws.common.exceptions.TimeoutException;
import edu.iu.dsc.tws.common.worker.IPersistentVolume;
import edu.iu.dsc.tws.common.worker.IVolatileVolume;
import edu.iu.dsc.tws.common.worker.IWorker;
import edu.iu.dsc.tws.comms.api.Communicator;
import edu.iu.dsc.tws.comms.api.MessageFlags;
import edu.iu.dsc.tws.comms.api.TWSChannel;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.examples.utils.bench.BenchmarkResultsRecorder;
import edu.iu.dsc.tws.examples.utils.bench.Timing;
import edu.iu.dsc.tws.examples.verification.ExperimentData;
import edu.iu.dsc.tws.examples.verification.IntArrayWrapper;
import edu.iu.dsc.tws.examples.verification.ResultsVerifier;
import edu.iu.dsc.tws.proto.jobmaster.JobMasterAPI;
import edu.iu.dsc.tws.task.graph.OperationMode;

public abstract class BenchWorker implements IWorker {

  private static final Logger LOG = Logger.getLogger(BenchWorker.class.getName());

  protected static final String TIMING_MESSAGE_SEND = "M_SEND";
  protected static final String TIMING_MESSAGE_RECV = "M_RECV";
  protected static final String TIMING_ALL_SEND = "ALL_SEND";
  protected static final String TIMING_ALL_RECV = "ALL_RECV";

  protected int workerId;

  protected Config config;

  protected TaskPlan taskPlan;

  protected JobParameters jobParameters;

  protected TWSChannel channel;

  protected Communicator communicator;

  protected final Map<Integer, Boolean> finishedSources = new ConcurrentHashMap<>();

  protected boolean sourcesDone = false;

  protected List<JobMasterAPI.WorkerInfo> workerList = null;

  protected ExperimentData experimentData;

  //for verification
  protected IntArrayWrapper inputDataArray;
  private boolean verified = true;

  //to capture benchmark results
  protected BenchmarkResultsRecorder resultsRecorder;

  @Override
  public void execute(Config cfg, int workerID,
                      IWorkerController workerController, IPersistentVolume persistentVolume,
                      IVolatileVolume volatileVolume) {

    // create the job parameters
    this.jobParameters = JobParameters.build(cfg);

    this.resultsRecorder = new BenchmarkResultsRecorder(
        cfg,
        workerID == 0
    );

    this.config = cfg;
    this.workerId = workerID;
    try {
      this.workerList = workerController.getAllWorkers();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
      return;
    }

    // lets create the task plan
    this.taskPlan = Utils.createStageTaskPlan(cfg, workerID,
        jobParameters.getTaskStages(), workerList);

    // create the channel
    channel = Network.initializeChannel(config, workerController);
    // create the communicator
    communicator = new Communicator(cfg, channel);

    //todo collect experiment data : will be removed
    experimentData = new ExperimentData();
    if (jobParameters.isStream()) {
      experimentData.setOperationMode(OperationMode.STREAMING);
    } else {
      experimentData.setOperationMode(OperationMode.BATCH);
    }
    //todo above will be removed

    this.inputDataArray = IntArrayWrapper.wrap(generateData());

    //todo below will be removed
    experimentData.setInput(this.inputDataArray.getArray());
    experimentData.setTaskStages(jobParameters.getTaskStages());
    experimentData.setIterations(jobParameters.getIterations());
    //todo above will be removed

    // now lets execute
    execute();
    // now communicationProgress
    progress();
    // wait for the sync
    try {
      workerController.waitOnBarrier();
    } catch (TimeoutException timeoutException) {
      LOG.log(Level.SEVERE, timeoutException.getMessage(), timeoutException);
    }
    // let allows the specific example to close
    close();
    // lets terminate the communicator
    communicator.close();
  }

  protected abstract void execute();

  protected void progress() {
    // we need to progress the communication

    while (!isDone()) {
      // communicationProgress the channel
      channel.progress();
      // we should communicationProgress the communication directive
      progressCommunication();
    }

    LOG.log(Level.INFO, workerId + " FINISHED PROGRESS");
  }

  protected abstract void progressCommunication();

  protected abstract boolean isDone();

  protected abstract boolean sendMessages(int task, Object data, int flag);

  public void close() {
  }

  protected void finishCommunication(int src) {
  }

  protected int[] generateData() {
    return DataGenerator.generateIntData(jobParameters.getSize());
  }

  /**
   * This method will verify results and append the output to the results recorder
   */
  protected void verifyResults(ResultsVerifier resultsVerifier, Comparable results) {
    if (jobParameters.isDoVerify()) {
      verified = verified && resultsVerifier.verify(results);
      //this will record verification failed if any of the iteration fails to verify
      this.resultsRecorder.recordColumn("Verified", verified);
    } else {
      this.resultsRecorder.recordColumn("Verified", "Not Performed");
    }
  }

  protected class MapWorker implements Runnable {
    private int task;

    private boolean timingCondition;

    public MapWorker(int task) {
      this.task = task;
      this.timingCondition = workerId == 0 && task == 0;
      Timing.defineFlag(
          TIMING_MESSAGE_SEND,
          jobParameters.getIterations(),
          this.timingCondition
      );
    }

    @Override
    public void run() {
      LOG.log(Level.INFO, "Starting map worker: " + workerId + " task: " + task);
      Timing.markMili(TIMING_ALL_SEND, this.timingCondition);

      for (int i = 0; i < jobParameters.getIterations(); i++) {
        // lets generate a message
        int flag = (i == jobParameters.getIterations() - 1) ? MessageFlags.LAST : 0;

        Timing.markMili(TIMING_MESSAGE_SEND, this.timingCondition);

        sendMessages(task, inputDataArray.getArray(), flag);
      }

      LOG.info(String.format("%d Done sending", workerId));

      synchronized (finishedSources) {
        finishedSources.put(task, true);
        boolean allDone = !finishedSources.values().contains(false);
        finishCommunication(task);
        sourcesDone = allDone;
      }
    }
  }
}
