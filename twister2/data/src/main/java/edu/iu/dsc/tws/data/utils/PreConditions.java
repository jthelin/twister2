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
package edu.iu.dsc.tws.data.utils;

import javax.annotation.Nullable;

public final class PreConditions {

  private PreConditions() {
  }

  public static <T> T checkNotNull(T reference, @Nullable String errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  public static String checkNotNull(String charset, @Nullable String errorMessage) {
    if (charset == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return charset;
  }
}
