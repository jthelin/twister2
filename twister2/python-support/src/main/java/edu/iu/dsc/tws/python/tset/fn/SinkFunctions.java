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
package edu.iu.dsc.tws.python.tset.fn;

import edu.iu.dsc.tws.api.tset.fn.SinkFunc;
import edu.iu.dsc.tws.python.processors.PythonLambdaProcessor;

public class SinkFunctions extends TFunc<SinkFunc> {

  private static final SinkFunctions INSTANCE = new SinkFunctions();

  static SinkFunctions getInstance() {
    return INSTANCE;
  }

  @Override
  public SinkFunc build(byte[] pyBinary) {
    PythonLambdaProcessor pythonLambdaProcessor = new PythonLambdaProcessor(pyBinary);
    return (SinkFunc) value -> {
      Object invoke = pythonLambdaProcessor.invoke(value);
      // in python lambda, use can return nothing
      if (invoke == null) {
        return false;
      }
      return (Boolean) invoke;
    };
  }
}
