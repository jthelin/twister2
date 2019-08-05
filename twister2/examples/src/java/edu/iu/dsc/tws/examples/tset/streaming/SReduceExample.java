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

package edu.iu.dsc.tws.examples.tset.streaming;

import java.util.HashMap;
import java.util.logging.Logger;

import edu.iu.dsc.tws.api.JobConfig;
import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.tset.env.StreamingTSetEnvironment;
import edu.iu.dsc.tws.api.tset.link.streaming.SReduceTLink;
import edu.iu.dsc.tws.api.tset.sets.streaming.SSourceTSet;
import edu.iu.dsc.tws.examples.tset.batch.BatchTsetExample;
import edu.iu.dsc.tws.rsched.core.ResourceAllocator;


public class SReduceExample extends StreamingTsetExample {
  private static final long serialVersionUID = -2753072757838198105L;
  private static final Logger LOG = Logger.getLogger(SReduceExample.class.getName());

  @Override
  public void buildGraph(StreamingTSetEnvironment env) {
    SSourceTSet<Integer> src = dummySource(env, COUNT, PARALLELISM);

    SReduceTLink<Integer> link = src.reduce(Integer::sum);

    link.map(i -> i * 2).direct().forEach(i -> LOG.info("m" + i.toString()));

    link.flatmap((i, c) -> c.collect("fm" + i))
        .direct().forEach(i -> LOG.info(i.toString()));

    link.compute(i -> i + "C")
        .direct().forEach(i -> LOG.info(i));

    link.compute((input, output) -> output.collect(input + "DD"))
        .direct().forEach(s -> LOG.info(s.toString()));
  }


  public static void main(String[] args) {
    Config config = ResourceAllocator.loadConfig(new HashMap<>());

    JobConfig jobConfig = new JobConfig();
    BatchTsetExample.submitJob(config, PARALLELISM, jobConfig, SReduceExample.class.getName());
  }
}
