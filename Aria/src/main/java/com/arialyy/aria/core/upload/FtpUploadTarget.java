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
package com.arialyy.aria.core.upload;

import android.support.annotation.CheckResult;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.common.ftp.FTPSDelegate;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.util.CheckUtil;
import java.net.Proxy;

/**
 * Created by Aria.Lao on 2017/7/27.
 * ftp单任务上传
 */
public class FtpUploadTarget extends AbsUploadTarget<FtpUploadTarget>
    implements IFtpTarget<FtpUploadTarget> {
  private FtpDelegate<FtpUploadTarget> mFtpDelegate;
  private UNormalDelegate<FtpUploadTarget> mNormalDelegate;

  FtpUploadTarget(String filePath, String targetName) {
    mNormalDelegate = new UNormalDelegate<>(this, filePath, targetName);
    initTask();
  }

  private void initTask() {
    getTaskWrapper().setRequestType(AbsTaskWrapper.U_FTP);
    mFtpDelegate = new FtpDelegate<>(this);
  }

  /**
   * 设置上传路径
   *
   * @param tempUrl 上传路径
   */
  public FtpUploadTarget setUploadUrl(String tempUrl) {
    mNormalDelegate.setTempUrl(tempUrl);
    return this;
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSDelegate#setStorePath(String)} 、{@link FTPSDelegate#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSDelegate<FtpUploadTarget> asFtps() {
    if (getTaskWrapper().asFtp().getUrlEntity() == null) {
      FtpUrlEntity urlEntity = new FtpUrlEntity();
      urlEntity.isFtps = true;
      getTaskWrapper().asFtp().setUrlEntity(urlEntity);
    }
    return new FTPSDelegate<>(this);
  }

  @CheckResult
  @Override public FtpUploadTarget charSet(String charSet) {
    return mFtpDelegate.charSet(charSet);
  }

  @Override public FtpUploadTarget login(String userName, String password) {
    return mFtpDelegate.login(userName, password);
  }

  @Override public FtpUploadTarget login(String userName, String password, String account) {
    CheckUtil.checkFtpUploadUrl(mNormalDelegate.getTempUrl());
    return mFtpDelegate.login(userName, password, account);
  }

  @Override public FtpUploadTarget setProxy(Proxy proxy) {
    return mFtpDelegate.setProxy(proxy);
  }

  @Override protected boolean checkEntity() {
    return mNormalDelegate.checkEntity();
  }

  @Override public boolean isRunning() {
    return mNormalDelegate.isRunning();
  }

  @Override public boolean taskExists() {
    return mNormalDelegate.taskExists();
  }

  @Override public int getTargetType() {
    return FTP;
  }
}
