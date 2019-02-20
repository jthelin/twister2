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
package edu.iu.dsc.tws.examples.comms.stream;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.iu.dsc.tws.common.config.Config;
import edu.iu.dsc.tws.comms.api.MessageType;
import edu.iu.dsc.tws.comms.api.Op;
import edu.iu.dsc.tws.comms.api.SingularReceiver;
import edu.iu.dsc.tws.comms.api.TaskPlan;
import edu.iu.dsc.tws.comms.api.functions.reduction.ReduceOperationFunction;
import edu.iu.dsc.tws.comms.api.stream.SReduce;
import edu.iu.dsc.tws.examples.Utils;
import edu.iu.dsc.tws.examples.comms.BenchWorker;
import edu.iu.dsc.tws.examples.utils.bench.Timing;
import edu.iu.dsc.tws.examples.verification.IntArrayWrapper;
import edu.iu.dsc.tws.examples.verification.ResultsVerifier;

public class SReduceExample extends BenchWorker {
  private static final Logger LOG = Logger.getLogger(SReduceExample.class.getName());

  private SReduce reduce;

  private boolean reduceDone = false;

  private ResultsVerifier<IntArrayWrapper, IntArrayWrapper> resultsVerifier;

  @Override
  protected void execute() {
    TaskPlan taskPlan = Utils.createStageTaskPlan(config, workerId,
        jobParameters.getTaskStages(), workerList);

    Set<Integer> sources = new HashSet<>();
    Integer noOfSourceTasks = jobParameters.getTaskStages().get(0);
    for (int i = 0; i < noOfSourceTasks; i++) {
      sources.add(i);
    }
    int target = noOfSourceTasks;

    // create the communication
    reduce = new SReduce(communicator, taskPlan, sources, target, MessageType.INTEGER,
        new ReduceOperationFunction(Op.SUM, MessageType.INTEGER),
        new FinalSingularReceiver(jobParameters.getIterations()));

    Set<Integer> tasksOfExecutor = Utils.getTasksOfExecutor(workerId, taskPlan,
        jobParameters.getTaskStages(), 0);
    for (int t : tasksOfExecutor) {
      finishedSources.put(t, false);
    }
    if (tasksOfExecutor.size() == 0) {
      sourcesDone = true;
    }

    if (!taskPlan.getChannelsOfExecutor(workerId).contains(target)) {
      reduceDone = true;
    }

    //generating the expected results at the end
    this.resultsVerifier = new ResultsVerifier<>(inputDataArray, arrayWrapper -> {
      int sourcesCount = jobParameters.getTaskStages().get(0);
      int[] outArray = new int[arrayWrapper.getSize()];
      for (int i = 0; i < arrayWrapper.getSize(); i++) {
        outArray[i] = arrayWrapper.getArray()[i] * sourcesCount;
      }
      return IntArrayWrapper.wrap(outArray);
    });

    // now initialize the workers
    for (int t : tasksOfExecutor) {
      // the map thread where data is produced
      Thread mapThread = new Thread(new MapWorker(t));
      mapThread.start();
    }
  }

  @Override
  protected void progressCommunication() {
    reduce.progress();
  }

  @Override
  protected boolean sendMessages(int task, Object data, int flag) {
    while (!reduce.reduce(task, data, flag)) {
      // lets wait a little and try again
      reduce.progress();
    }
    return true;
  }

  @Override
  protected boolean isDone() {
//    LOG.log(Level.INFO, String.format("%d Reduce %b sources %b pending %b",
//        workerId, reduceDone, sourcesDone, reduce.hasPending()));
    return reduceDone && sourcesDone && !reduce.hasPending();
  }

  public class FinalSingularReceiver implements SingularReceiver {
    private int count = 0;
    private int expected;

    public FinalSingularReceiver(int expected) {
      this.expected = expected;
    }

    @Override
    public void init(Config cfg, Set<Integer> expectedIds) {
      Timing.defineFlag(TIMING_MESSAGE_RECV, jobParameters.getIterations(), workerId == 0);
    }

    @Override
    public boolean receive(int target, Object object) {
      Timing.markMili(TIMING_MESSAGE_RECV, workerId == 0);
      count++;
      LOG.log(Level.INFO, String.format("Target %d received count %d", target, count));

      verifyResults(resultsVerifier, IntArrayWrapper.wrap(object));

      if (count == expected) {
        Timing.markMili(TIMING_ALL_RECV, workerId == 0);
        resultsRecorder.recordColumn(
            "Total Time",
            Timing.averageDiff(TIMING_ALL_SEND, TIMING_ALL_RECV, workerId == 0)
        );
        resultsRecorder.recordColumn(
            "Average Time",
            Timing.averageDiff(TIMING_MESSAGE_SEND, TIMING_MESSAGE_RECV, workerId == 0)
        );
        resultsRecorder.writeToCSV();

        LOG.info("Total time for all iterations : "
            + Timing.averageDiff(TIMING_ALL_SEND, TIMING_ALL_RECV, workerId == 0));

        LOG.info("Average time for reduce : "
            + Timing.averageDiff(TIMING_MESSAGE_SEND, TIMING_MESSAGE_RECV, workerId == 0));

        reduceDone = true;
      }
      experimentData.setOutput(object);
//      try {
//        verify();
//      } catch (VerificationException e) {
//        LOG.info("Exception Message : " + e.getMessage());
//      }
      return true;
    }
  }

//  public void verify() throws VerificationException {
//    boolean doVerify = jobParameters.isDoVerify();
//    boolean isVerified = false;
//    if (doVerify) {
//      LOG.info("Verifying results ...");
//      ExperimentVerification experimentVerification
//          = new ExperimentVerification(experimentData, OperationNames.REDUCE);
//      isVerified = experimentVerification.isVerified();
//      if (isVerified) {
//        LOG.info("Results generated from the experiment are verified.");
//      } else {
//        throw new VerificationException("Results do not match");
//      }
//    }
//  }
}
