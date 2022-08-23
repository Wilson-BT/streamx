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

package com.streamxhub.streamx.flink.connector.hbase.internal

import com.streamxhub.streamx.common.enums.ApiType
import com.streamxhub.streamx.common.enums.ApiType.ApiType
import com.streamxhub.streamx.common.util.{FlinkUtils, Logger}
import com.streamxhub.streamx.flink.connector.function.RunningFunction
import com.streamxhub.streamx.flink.connector.hbase.bean.HBaseQuery
import com.streamxhub.streamx.flink.connector.hbase.function.{HBaseQueryFunction, HBaseResultFunction}
import org.apache.flink.api.common.state.ListState
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.runtime.state.{CheckpointListener, FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.source.RichSourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.hadoop.hbase.client.{Result, Table}

import java.lang
import java.util.Properties
import scala.util.{Success, Try}
import scala.collection.JavaConversions._

class HBaseSourceFunction[R: TypeInformation](apiType: ApiType = ApiType.scala, prop: Properties) extends RichSourceFunction[R] with CheckpointedFunction with CheckpointListener with Logger {


  @volatile private[this] var running = true
  private[this] var scalaRunningFunc: Unit => Boolean = _
  private[this] var javaRunningFunc: RunningFunction = _

  @transient private[this] var table: Table = _

  @volatile var query: HBaseQuery = _

  private[this] var scalaQueryFunc: R => HBaseQuery = _
  private[this] var scalaResultFunc: Result => R = _

  private[this] var javaQueryFunc: HBaseQueryFunction[R] = _
  private[this] var javaResultFunc: HBaseResultFunction[R] = _

  @transient private var state: ListState[R] = _
  private val OFFSETS_STATE_NAME: String = "hbase-source-query-states"
  private[this] var last: R = _

  //for Scala
  def this(prop: Properties,
           queryFunc: R => HBaseQuery,
           resultFunc: Result => R,
           runningFunc: Unit => Boolean) = {

    this(ApiType.scala, prop)
    this.scalaQueryFunc = queryFunc
    this.scalaResultFunc = resultFunc
    this.scalaRunningFunc = if (runningFunc == null) _ => true else runningFunc

  }

  //for JAVA
  def this(prop: Properties,
           queryFunc: HBaseQueryFunction[R],
           resultFunc: HBaseResultFunction[R],
           runningFunc: RunningFunction) {

    this(ApiType.java, prop)
    this.javaQueryFunc = queryFunc
    this.javaResultFunc = resultFunc
    this.javaRunningFunc = if (runningFunc != null) runningFunc else new RunningFunction {
      override def running(): lang.Boolean = true
    }

  }

  @throws[Exception]
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
  }

  override def run(ctx: SourceContext[R]): Unit = {
    while (this.running) {
      apiType match {
        case ApiType.scala =>
          if (scalaRunningFunc()) {
            ctx.getCheckpointLock.synchronized {
              //将上次(或者从checkpoint中恢复)的query查询对象返回用户,用户根据这个构建下次要查询的条件.
              query = scalaQueryFunc(last)
              require(query != null && query.getTable != null, "[StreamX] HBaseSource query and query's param table must not be null ")
              table = query.getTable(prop)
              table.getScanner(query).foreach(x => {
                last = scalaResultFunc(x)
                ctx.collectWithTimestamp(last, System.currentTimeMillis())
              })
            }
          }
        case ApiType.java =>
          if (javaRunningFunc.running()) {
            ctx.getCheckpointLock.synchronized {
              //将上次(或者从checkpoint中恢复)的query查询对象返回用户,用户根据这个构建下次要查询的条件.
              query = javaQueryFunc.query(last)
              require(query != null && query.getTable != null, "[StreamX] HBaseSource query and query's param table must not be null ")
              table = query.getTable(prop)
              table.getScanner(query).foreach(x => {
                last = javaResultFunc.result(x)
                ctx.collectWithTimestamp(last, System.currentTimeMillis())
              })
            }
          }
      }
    }
  }

  override def cancel(): Unit = this.running = false

  override def close(): Unit = {
    super.close()
    if (table != null) {
      table.close()
    }
  }

  override def snapshotState(context: FunctionSnapshotContext): Unit = {
    if (running) {
      state.clear()
      if (last != null) {
        state.add(last)
      }
    } else {
      logError("HBaseSource snapshotState called on closed source")
    }
  }

  override def initializeState(context: FunctionInitializationContext): Unit = {
    //从checkpoint中恢复...
    logInfo("HBaseSource snapshotState initialize")
    state = FlinkUtils.getUnionListState[R](context, OFFSETS_STATE_NAME)
    Try(state.get.head) match {
      case Success(q) => last = q
      case _ =>
    }
  }

  override def notifyCheckpointComplete(checkpointId: Long): Unit = {
    logInfo(s"HBaseSource checkpointComplete: $checkpointId")
  }

}