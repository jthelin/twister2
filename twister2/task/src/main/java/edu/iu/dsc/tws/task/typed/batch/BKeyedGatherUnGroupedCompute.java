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
package edu.iu.dsc.tws.task.typed.batch;

import java.util.Iterator;

import edu.iu.dsc.tws.api.comms.structs.Tuple;
import edu.iu.dsc.tws.api.compute.IMessage;
import edu.iu.dsc.tws.task.typed.KeyedGatherCompute;

public abstract class BKeyedGatherUnGroupedCompute<K, T> extends KeyedGatherCompute<K, T> {

  public abstract boolean keyedGather(Iterator<Tuple<K, T>> content);

  public boolean execute(IMessage<Iterator<Tuple<K, T>>> content) {
    return this.keyedGather(content.getContent());
  }
}
