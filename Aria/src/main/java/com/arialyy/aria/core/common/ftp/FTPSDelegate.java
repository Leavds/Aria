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

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.ProtocolType;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.ITargetHandler;

/**
 * FTP SSL/TSL 参数委托
 */
public class FTPSDelegate<TARGET extends AbsTarget> implements ITargetHandler {
  private final String TAG = "FTPSDelegate";
  private TARGET mTarget;
  private FtpUrlEntity mUrlEntity;

  public FTPSDelegate(TARGET target) {
    mTarget = target;
    mUrlEntity = mTarget.getTaskWrapper().asFtp().getUrlEntity();
  }

  /**
   * 设置协议类型
   *
   * @param protocol {@link ProtocolType}
   */
  public FTPSDelegate setProtocol(@ProtocolType String protocol) {
    if (TextUtils.isEmpty(protocol)) {
      throw new NullPointerException("协议为空");
    }
    mUrlEntity.protocol = protocol;
    return this;
  }

  /**
   * 设置证书别名
   *
   * @param keyAlias 别名
   */
  public FTPSDelegate setAlias(String keyAlias) {
    if (TextUtils.isEmpty(keyAlias)) {
      throw new NullPointerException("别名为空");
    }
    mUrlEntity.keyAlias = keyAlias;
    return this;
  }

  /**
   * 设置证书密码
   *
   * @param storePass 私钥密码
   */
  public FTPSDelegate setStorePass(String storePass) {
    if (TextUtils.isEmpty(storePass)) {
      throw new NullPointerException("证书密码为空");
    }
    mUrlEntity.storePass = storePass;
    return this;
  }

  /**
   * 设置证书路径
   *
   * @param storePath 证书路径
   */
  public FTPSDelegate setStorePath(String storePath) {
    if (TextUtils.isEmpty(storePath)) {
      throw new NullPointerException("证书路径为空");
    }
    mUrlEntity.storePath = storePath;
    return this;
  }

  @Override public void add() {
    mTarget.add();
  }

  @Override public void start() {
    mTarget.start();
  }

  @Override public void stop() {
    mTarget.stop();
  }

  @Override public void resume() {
    mTarget.resume();
  }

  @Override public void cancel() {
    mTarget.cancel();
  }

  @Override public void save() {
    mTarget.save();
  }

  @Override public void cancel(boolean removeFile) {
    mTarget.cancel(removeFile);
  }

  @Override public void reTry() {
    mTarget.reTry();
  }

  @Override public void reStart() {
    mTarget.reStart();
  }
}
