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
package org.apache.storm.topology.twister2;

import org.apache.storm.tuple.Fields;

import edu.iu.dsc.tws.api.task.nodes.INode;

public interface Twister2StormNode extends INode {

  String getId();

  Fields getOutFieldsForEdge(String edge);

  void setKeyedOutEdges(String edge, Fields keys);
}
