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
import com.arialyy.aria.core.common.ftp.FTPSDelegate;
import com.arialyy.aria.core.common.ftp.FtpDelegate;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import java.net.Proxy;

/**
 * Created by Aria.Lao on 2017/7/26.
 * ftp文件夹下载
 */
public class FtpDirDownloadTarget extends AbsDGTarget<FtpDirDownloadTarget>
    implements IFtpTarget<FtpDirDownloadTarget> {
  private FtpDelegate<FtpDirDownloadTarget> mFtpDelegate;
  private FtpDirDelegate mDirDelegate;

  FtpDirDownloadTarget(String url, String targetName) {
    setTargetName(targetName);
    init(url);
  }

  private void init(String key) {
    mDirDelegate = new FtpDirDelegate(this,
        TaskWrapperManager.getInstance().getFtpTaskWrapper(DGTaskWrapper.class, key));
    mFtpDelegate = new FtpDelegate<>(this);
  }

  @Override public int getTargetType() {
    return GROUP_FTP_DIR;
  }

  @Override protected boolean checkEntity() {
    return mDirDelegate.checkEntity();
  }

  @Override public boolean isRunning() {
    return mDirDelegate.isRunning();
  }

  @Override public boolean taskExists() {
    return mDirDelegate.taskExists();
  }

  /**
   * 设置任务组的文件夹路径，在Aria中，任务组的所有子任务都会下载到以任务组组名的文件夹中。
   * 如：groupDirPath = "/mnt/sdcard/download/group_test"
   * <pre>
   *   {@code
   *      + mnt
   *        + sdcard
   *          + download
   *            + group_test
   *              - task1.apk
   *              - task2.apk
   *              - task3.apk
   *              ....
   *
   *   }
   * </pre>
   *
   * @param dirPath 任务组保存文件夹路径
   */
  @CheckResult
  public FtpDirDownloadTarget setDirPath(String dirPath) {
    return mDirDelegate.setDirPath(dirPath);
  }

  /**
   * 是否是FTPS协议
   * 如果是FTPS协议，需要使用{@link FTPSDelegate#setStorePath(String)} 、{@link FTPSDelegate#setAlias(String)}
   * 设置证书信息
   */
  @CheckResult
  public FTPSDelegate<FtpDirDownloadTarget> asFtps() {
    getTaskWrapper().asFtp().getUrlEntity().isFtps = true;
    return new FTPSDelegate<>(this);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget charSet(String charSet) {
    return mFtpDelegate.charSet(charSet);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget login(String userName, String password) {
    return mFtpDelegate.login(userName, password);
  }

  @CheckResult
  @Override public FtpDirDownloadTarget login(String userName, String password, String account) {
    return mFtpDelegate.login(userName, password, account);
  }

  @Override public FtpDirDownloadTarget setProxy(Proxy proxy) {
    return mFtpDelegate.setProxy(proxy);
  }
}
