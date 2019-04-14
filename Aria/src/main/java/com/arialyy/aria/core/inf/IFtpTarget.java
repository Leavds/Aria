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
import java.net.Proxy;

/**
 * Created by laoyuyu on 2018/3/9.
 */
public interface IFtpTarget<TARGET extends ITargetHandler> {
  /**
   * 设置字符编码
   */
  @CheckResult
  TARGET charSet(String charSet);

  /**
   * ftp 用户登录信。
   *
   * @param userName ftp用户名
   * @param password ftp用户密码
   */
  @CheckResult
  TARGET login(String userName, String password);

  /**
   * ftp 用户登录信息
   *
   * @param userName ftp用户名
   * @param password ftp用户密码
   * @param account ftp账号
   */
  @CheckResult
  TARGET login(String userName, String password, String account);

  /**
   * 设置代理
   *
   * @param proxy {@link Proxy}
   */
  @CheckResult
  TARGET setProxy(Proxy proxy);
}
