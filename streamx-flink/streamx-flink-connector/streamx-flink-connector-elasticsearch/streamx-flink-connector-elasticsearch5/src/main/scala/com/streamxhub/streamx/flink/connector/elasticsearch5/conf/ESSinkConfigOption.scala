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

package com.streamxhub.streamx.flink.connector.elasticsearch5.conf

import com.streamxhub.streamx.common.conf.ConfigOption
import com.streamxhub.streamx.common.util.ConfigUtils

import java.net.InetSocketAddress
import java.util.{Properties, Map => JavaMap}
import scala.collection.JavaConverters._

/**
 * @author benjobs
 */
object ESSinkConfigOption {
  val ES_SINK_PREFIX = "es.sink"

  /**
   *
   * @param properties
   * @return
   */
  def apply(prefixStr: String = ES_SINK_PREFIX, properties: Properties = new Properties): ESSinkConfigOption = new ESSinkConfigOption(prefixStr, properties)

}

class ESSinkConfigOption(prefixStr: String, properties: Properties) extends Serializable {

  implicit val (prefix, prop) = (prefixStr, properties)


  val SIGN_COMMA = ","

  val SIGN_COLON = ":"

  val disableFlushOnCheckpoint: ConfigOption[Boolean] = ConfigOption(
    key = "es.disableFlushOnCheckpoint",
    required = false,
    classType = classOf[Boolean],
    defaultValue = false
  )

  val host: ConfigOption[Array[InetSocketAddress]] = ConfigOption(
    key = "host",
    required = true,
    classType = classOf[Array[InetSocketAddress]],
    handle = key => properties.getProperty(key).split(SIGN_COMMA).map(x => {
      x.split(SIGN_COLON) match {
        case Array(host, port) => new InetSocketAddress(host, port.toInt)
      }
    })
  )

  def getInternalConfig(): JavaMap[String, String] = {
    ConfigUtils.getConf(prop.asScala.asJava, prefix)(alias = "").asScala.asJava
  }


}
