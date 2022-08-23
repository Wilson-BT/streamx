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

package com.streamxhub.streamx.flink.kubernetes.event

import com.streamxhub.streamx.flink.kubernetes.model.{CheckpointCV, TrackId}

/**
 * held internally by K8sFlinkMonitor.
 *
 * @author benjobs
 */
case class FlinkJobCheckpointChangeEvent(trackId: TrackId, checkpoint: CheckpointCV) extends BuildInEvent
