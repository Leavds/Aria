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

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.util.ALog;

/**
 * Created by lyy on 2017/4/9.
 * ftp文件夹下载功能代理
 */
class FtpDirDelegate extends AbsGroupDelegate<FtpDirDownloadTarget> {
  FtpDirDelegate(FtpDirDownloadTarget target, DGTaskWrapper wrapper) {
    super(target, wrapper);
    wrapper.setRequestType(AbsTaskWrapper.D_FTP_DIR);
  }

  @Override public boolean checkEntity() {
    boolean b = checkDirPath() && checkUrl();
    if (b) {
      getEntity().save();
      if (getTaskWrapper().getSubTaskWrapper() != null) {
        //初始化子项的登录信息
        FtpUrlEntity tUrlEntity = getTaskWrapper().asFtp().getUrlEntity();
        for (DTaskWrapper wrapper : getTaskWrapper().getSubTaskWrapper()) {
          FtpUrlEntity urlEntity = wrapper.asFtp().getUrlEntity();
          urlEntity.needLogin = tUrlEntity.needLogin;
          urlEntity.account = tUrlEntity.account;
          urlEntity.user = tUrlEntity.user;
          urlEntity.password = tUrlEntity.password;
          // 处理ftps详细
          if (tUrlEntity.isFtps) {
            urlEntity.isFtps = true;
            urlEntity.protocol = tUrlEntity.protocol;
            urlEntity.storePath = tUrlEntity.storePath;
            urlEntity.storePass = tUrlEntity.storePass;
            urlEntity.keyAlias = tUrlEntity.keyAlias;
          }
        }
      }
    }
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
    return b;
  }

  /**
   * 检查普通任务的下载地址
   *
   * @return {@code true}地址合法
   */
  private boolean checkUrl() {
    final String url = getGroupHash();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "下载失败，url为null");
      return false;
    } else if (!url.startsWith("ftp")) {
      ALog.e(TAG, "下载失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "下载失败，url【" + url + "】不合法");
      return false;
    }
    return true;
  }
}
