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

package com.streamxhub.streamx.spark.core.serializable

import org.apache.hadoop.mapred.lib.MultipleTextOutputFormat

/**
  *
  *
  * 重载
  * generateFileNameForKeyValue 为 s"${key}_$name"
  * generateActualKey 为 null
  */
class TextOutputFormat extends MultipleTextOutputFormat[Any, Any] {

  override def generateFileNameForKeyValue(key: Any, value: Any, name: String): String = {
    s"${key}_$name"
  }

  override def generateActualKey(key: Any, value: Any): AnyRef = {
    null
  }
}
