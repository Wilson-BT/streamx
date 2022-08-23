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

package com.streamxhub.streamx.console.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_access_token")
public class AccessToken implements Serializable {

    private static final long serialVersionUID = 7187628714679791772L;
    public static final String DEFAULT_EXPIRE_TIME = "9999-01-01 00:00:00";
    public static final String IS_API_TOKEN = "is_api_token";

    /**
     * token状态
     */
    public static final Integer STATUS_ENABLE = 1;
    public static final Integer STATUS_DISABLE = 0;

    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "{required}")
    private Long userId;

    @NotBlank(message = "{required}")
    private String token;

    @NotNull(message = "{required}")
    private Integer status;

    @NotNull(message = "{required}")
    private Date expireTime;

    private String description;

    private Date createTime;

    private Date modifyTime;

    private transient String username;

    private transient String userStatus;

    /**
     * token最终可用状态  token&user 同时可用 1:可用，0：不可用
     */
    private transient Integer finalStatus;

    public AccessToken setStatus(Integer status) {
        this.status = status;
        return this;
    }

}