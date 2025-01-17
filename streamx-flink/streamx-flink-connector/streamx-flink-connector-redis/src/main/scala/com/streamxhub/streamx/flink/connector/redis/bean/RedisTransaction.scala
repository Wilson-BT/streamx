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

package com.streamxhub.streamx.flink.connector.redis.bean

import com.streamxhub.streamx.common.util.Utils

import scala.collection.mutable

case class RedisTransaction[T](
                                transactionId: String = Utils.uuid(),
                                mapper: mutable.MutableList[(RedisMapper[T], T, Int)] = mutable.MutableList.empty[(RedisMapper[T], T, Int)],
                                var invoked: Boolean = false) extends Serializable {
  def +(redisMapper: (RedisMapper[T], T, Int)): Unit = mapper += redisMapper

  override def toString: String = s"(transactionId:$transactionId,size:${mapper.size},invoked:$invoked)"
}
