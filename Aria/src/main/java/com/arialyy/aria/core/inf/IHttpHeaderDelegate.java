/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.core.inf;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import java.net.Proxy;
import java.util.Map;

/**
 * Created by laoyuyu on 2018/3/9.
 * HTTP Header功能接口
 */
public interface IHttpHeaderDelegate<TARGET extends ITargetHandler> {

  /**
   * 给url请求添加Header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param key header对应的key
   * @param value header对应的value
   */
  @CheckResult
  TARGET addHeader(@NonNull String key, @NonNull String value);

  /**
   * 给url请求添加一组header数据
   * 如果新的header数据和数据保存的不一致，则更新数据库中对应的header数据
   *
   * @param headers 一组http header数据
   */
  @CheckResult
  TARGET addHeaders(Map<String, String> headers);

  @CheckResult
  TARGET setUrlProxy(Proxy proxy);
}
