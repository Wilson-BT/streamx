/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.common.enums

object RestartStrategy extends Enumeration {
  type RestartStrategy = Value
  val `fixed-delay`, `failure-rate`, `none` = Value

  /**
   *
   * @param name
   * @return
   */
  def byName(name: String): Value = {
    if (name == null) null else {
      values.find(_.toString.replace("$minus", "-").equalsIgnoreCase(name)) match {
        case Some(v) => v
        case _ => throw new IllegalArgumentException("[StreamX] RestartStrategy must be (fixed-delay|failure-rate|none)")
      }
    }
  }

}
