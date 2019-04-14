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
package com.arialyy.aria.core.common.ftp;

import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.inf.ITargetHeadDelegate;
import java.net.Proxy;

/**
 * fTP任务设置的信息，如：用户名、密码、端口等信息
 */
public class FtpTaskDelegate implements ITargetHeadDelegate {

  /**
   * 账号和密码
   */
  private FtpUrlEntity urlEntity;

  private Proxy proxy;

  /**
   * 字符编码，默认为"utf-8"
   */
  private String charSet = "utf-8";

  public FtpUrlEntity getUrlEntity() {
    return urlEntity;
  }

  public void setUrlEntity(FtpUrlEntity urlEntity) {
    this.urlEntity = urlEntity;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public String getCharSet() {
    return charSet;
  }

  public void setCharSet(String charSet) {
    this.charSet = charSet;
  }
}
