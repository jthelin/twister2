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
package edu.iu.dsc.tws.api.tset.impl;

import edu.iu.dsc.tws.api.task.TaskGraphBuilder;
import edu.iu.dsc.tws.api.tset.ReduceFunction;
import edu.iu.dsc.tws.common.config.Config;

public class ReduceTSet<T> extends BaseTSet<T> {
  private BaseTSet<T> parent;

  public ReduceTSet(Config cfg, TaskGraphBuilder bldr,
                    BaseTSet<T> prnt, ReduceFunction<T> reduceFn) {
    super(cfg, bldr);
    this.parent = prnt;
  }

  public void build() {
    // we cannot do anything here as reduce needs to be connected to another operation
  }

  @Override
  protected Op getOp() {
    return null;
  }
}
