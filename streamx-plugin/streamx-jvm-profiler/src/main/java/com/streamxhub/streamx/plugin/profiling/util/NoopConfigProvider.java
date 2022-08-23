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

package com.streamxhub.streamx.plugin.profiling.util;

import com.streamxhub.streamx.plugin.profiling.ConfigProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
public class NoopConfigProvider implements ConfigProvider {
    @Override
    public Map<String, Map<String, List<String>>> getConfig() {
        Map<String, Map<String, List<String>>> configMap = new HashMap<>();

        Map<String, List<String>> values = new HashMap<>();
        values.put("noop", Arrays.asList("true"));

        configMap.put("", values);

        return configMap;
    }
}
