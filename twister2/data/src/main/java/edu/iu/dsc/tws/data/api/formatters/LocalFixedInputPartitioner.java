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
package edu.iu.dsc.tws.data.api.formatters;

import java.util.logging.Logger;

import edu.iu.dsc.tws.api.config.Config;
import edu.iu.dsc.tws.api.data.Path;
import edu.iu.dsc.tws.data.api.assigner.OrderedInputSplitAssigner;
import edu.iu.dsc.tws.data.api.splits.FileInputSplit;
import edu.iu.dsc.tws.data.api.splits.TextInputSplit;

public class LocalFixedInputPartitioner<T> extends FixedInputPartitioner<T> {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger(LocalTextInputPartitioner.class.getName());

  private int nTasks;

  private OrderedInputSplitAssigner<T> assigner;

  public LocalFixedInputPartitioner(Path filePath, int numTasks) {
    super(filePath);
    this.nTasks = numTasks;
  }

  public LocalFixedInputPartitioner(Path filePath, int numTasks, Config config, int datasize) {
    super(filePath, config, datasize);
    this.nTasks = numTasks;
  }

  @Override
  protected TextInputSplit createSplit(int num, Path file, long start, long length,
                                       String[] hosts) {
    return new TextInputSplit(num, file, start, length, hosts);
  }

  @Override
  public OrderedInputSplitAssigner<T> getInputSplitAssigner(FileInputSplit<T>[] inputSplits) {
    if (assigner == null) {
      assigner = new OrderedInputSplitAssigner<T>(inputSplits, nTasks);
    }
    return assigner;
  }
}
