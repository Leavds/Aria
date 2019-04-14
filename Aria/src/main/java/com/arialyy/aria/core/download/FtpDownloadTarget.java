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
package com.arialyy.aria.core.download;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.arialyy.aria.core.common.ftp.FTPSDelegate;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.net.Proxy;

/**
 * Created by lyy on 2016/12/5.
 * https://github.com/AriaLyy/Aria
 */
public class FtpDownloadTarget extends AbsDTarget<FtpDownloadTarget>
    implements IFtpTarget<FtpDownloadTarget> {
  private FtpDelegate<FtpDownloadTarget> mFtpDelegate;
  private DNormalDelegate<FtpDownloadTarget> mNormalDelegate;

  FtpDownloadTarget(DownloadEntity entity, String targetName) {
    this(entity.getUrl(), targetName);
  }

  FtpDownloadTarget(String url, String targetName) {
    mNormalDelegate = new DNormalDelegate<>(this, url, targetName);
    init();
  }

  private void init() {
    int lastIndex = mNormalDelegate.getUrl().lastIndexOf("/");
    getEntity().setFileName(mNormalDelegate.getUrl().substring(lastIndex + 1));
    getTaskWrapper().asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(mNormalDelegate.getUrl()));
    getTaskWrapper().setRequestType(AbsTaskWrapper.D_FTP);

    mFtpDelegate = new FtpDelegate<>(this);
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSDelegate#setStorePath(String)} 、{@link FTPSDelegate#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSDelegate<FtpDownloadTarget> asFtps() {
    getTaskWrapper().asFtp().getUrlEntity().isFtps = true;
    return new FTPSDelegate<>(this);
  }

  @Override protected boolean checkEntity() {
    if (getTaskWrapper().asFtp().getUrlEntity().isFtps) {
      if (TextUtils.isEmpty(getTaskWrapper().asFtp().getUrlEntity().storePath)) {
        ALog.e(TAG, "证书路径为空");
        return false;
      }
      if (TextUtils.isEmpty(getTaskWrapper().asFtp().getUrlEntity().keyAlias)) {
        ALog.e(TAG, "证书别名为空");
        return false;
      }
    }
    return mNormalDelegate.checkEntity();
  }

  @Override public boolean isRunning() {
    return mNormalDelegate.isRunning();
  }

  @Override public boolean taskExists() {
    return mNormalDelegate.taskExists();
  }

  /**
   * 设置文件保存文件夹路径
   *
   * @param filePath 文件保存路径
   * @deprecated {@link #setFilePath(String)} 请使用这个api
   */
  @Deprecated
  @CheckResult
  public FtpDownloadTarget setDownloadPath(@NonNull String filePath) {
    return setFilePath(filePath);
  }

  /**
   * 设置文件保存文件夹路径
   * 关于文件名：
   * 1、如果保存路径是该文件的保存路径，如：/mnt/sdcard/file.zip，则使用路径中的文件名file.zip
   * 2、如果保存路径是文件夹路径，如：/mnt/sdcard/，则使用FTP服务器该文件的文件名
   */
  @CheckResult
  public FtpDownloadTarget setFilePath(@NonNull String filePath) {
    mNormalDelegate.setTempFilePath(filePath);
    return this;
  }

  /**
   * 设置文件存储路径，如果需要修改新的文件名，修改路径便可。
   * 如：原文件路径 /mnt/sdcard/test.zip
   * 如果需要将test.zip改为game.zip，只需要重新设置文件路径为：/mnt/sdcard/game.zip
   *
   * @param filePath 路径必须为文件路径，不能为文件夹路径
   * @param forceDownload {@code true}强制下载，不考虑文件路径是否被占用
   */
  @CheckResult
  public FtpDownloadTarget setFilePath(@NonNull String filePath, boolean forceDownload) {
    mNormalDelegate.setTempFilePath(filePath);
    mNormalDelegate.setForceDownload(forceDownload);
    return this;
  }

  @Override public int getTargetType() {
    return FTP;
  }

  @CheckResult
  @Override public FtpDownloadTarget charSet(String charSet) {
    return mFtpDelegate.charSet(charSet);
  }

  @CheckResult
  @Override public FtpDownloadTarget login(String userName, String password) {
    return mFtpDelegate.login(userName, password);
  }

  @CheckResult
  @Override public FtpDownloadTarget login(String userName, String password, String account) {
    return mFtpDelegate.login(userName, password, account);
  }

  @CheckResult
  @Override public FtpDownloadTarget setProxy(Proxy proxy) {
    return mFtpDelegate.setProxy(proxy);
  }

  @Override public FtpDownloadTarget updateUrl(String newUrl) {
    return mNormalDelegate.updateUrl(newUrl);
  }
}
