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
package edu.iu.dsc.tws.examples.utils.bench;

public final class BenchmarkConstants {

  /**
   * Sender should report M_SEND just before sending a message. For n messages, sender will
   * report n times.
   */
  public static final String TIMING_MESSAGE_SEND = "MESSAGE_SEND";

  /**
   * Receiver will report M_RECV just after receiving a message. For n messages, receiver will
   * report n times.
   */
  public static final String TIMING_MESSAGE_RECV = "MESSAGE_RECV";

  /**
   * Sender will report ALL_SEND just before it send very first message. For n message, sender will
   * report only once.
   */
  public static final String TIMING_ALL_SEND = "ALL_SEND";

  /**
   * Receiver will report ALL_RECV just after receiving the last message. For n messages, sender
   * will report only once.
   */
  public static final String TIMING_ALL_RECV = "ALL_RECV";

  public static final String COLUMN_TOTAL_TIME = "Total Time";
  public static final String COLUMN_AVERAGE_TIME = "Average Time";

  private BenchmarkConstants() {

  }
}
