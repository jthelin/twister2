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

package edu.iu.dsc.tws.api.tset.fn;

/**
 * Represents a source producing values
 *
 * @param <T> type of values produced by source
 */
public interface SourceFunc<T> extends TFunction<T, T> {

  /**
   * Weather we have more data
   *
   * @return true if there is more data to be read
   */
  boolean hasNext();

  /**
   * Get the next value
   *
   * @return the next value
   */
  T next();

  /**
   * Closes the source
   */
  @Override
  default void close() {

  }
}
