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

import android.text.TextUtils;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.INormalTarget;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * Created by Aria.Lao on 2019/4/5.
 * 普通上传任务通用功能处理
 */
class UNormalDelegate<TARGET extends AbsUploadTarget> implements INormalTarget {
  private String TAG = "UNormalDelegate";
  private UploadEntity mEntity;
  private TARGET mTarget;
  /**
   * 上传路径
   */
  private String mTempUrl;

  UNormalDelegate(TARGET target, String filePath, String targetName) {
    mTarget = target;
    initTarget(filePath, targetName);
  }

  private void initTarget(String filePath, String targetName) {
    UTaskWrapper taskWrapper =
        TaskWrapperManager.getInstance().getHttpTaskWrapper(UTaskWrapper.class, filePath);
    mEntity = taskWrapper.getEntity();
    File file = new File(filePath);
    mEntity.setFileName(file.getName());
    mEntity.setFileSize(file.length());
    mTarget.setTargetName(targetName);
    mTarget.setTaskWrapper(taskWrapper);
    mTempUrl = mEntity.getUrl();
  }

  TARGET updateUrl(String newUrl) {
    mTempUrl = newUrl;
    return mTarget;
  }

  @Override public AbsEntity getEntity() {
    return mEntity;
  }

  @Override public boolean taskExists() {
    return DbEntity.checkDataExist(UploadEntity.class, "key=?", mEntity.getFilePath());
  }

  @Override public boolean isRunning() {
    UploadTask task = UploadTaskQueue.getInstance().getTask(mEntity.getKey());
    return task != null && task.isRunning();
  }

  @Override public boolean checkEntity() {
    boolean b = checkUrl() && checkFilePath();
    if (b) {
      mEntity.save();
    }
    if (mTarget.getTaskWrapper().asFtp().getUrlEntity() != null && mTarget.getTaskWrapper()
        .asFtp()
        .getUrlEntity().isFtps) {
      //if (TextUtils.isEmpty(mTaskWrapper.getUrlEntity().storePath)) {
      //  ALog.e(TAG, "证书路径为空");
      //  return false;
      //}
      if (TextUtils.isEmpty(mTarget.getTaskWrapper().asFtp().getUrlEntity().keyAlias)) {
        ALog.e(TAG, "证书别名为空");
        return false;
      }
    }
    return b;
  }

  @Override public boolean checkFilePath() {
    String filePath = mEntity.getFilePath();
    if (TextUtils.isEmpty(filePath)) {
      ALog.e(TAG, "上传失败，文件路径为null");
      return false;
    } else if (!filePath.startsWith("/")) {
      ALog.e(TAG, "上传失败，文件路径【" + filePath + "】不合法");
      return false;
    }

    File file = new File(mEntity.getFilePath());
    if (!file.exists()) {
      ALog.e(TAG, "上传失败，文件【" + filePath + "】不存在");
      return false;
    }
    if (file.isDirectory()) {
      ALog.e(TAG, "上传失败，文件【" + filePath + "】不能是文件夹");
      return false;
    }
    return true;
  }

  @Override public boolean checkUrl() {

    final String url = mTempUrl;
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "上传失败，url为null");
      return false;
    } else if (!url.startsWith("http") && !url.startsWith("ftp")) {
      ALog.e(TAG, "上传失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "上传失败，url【" + url + "】不合法");
      return false;
    }
    mEntity.setUrl(url);
    return true;
  }

  void setTempUrl(String tempUrl) {
    this.mTempUrl = tempUrl;
    mTarget.getTaskWrapper().asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(tempUrl));
  }

  public String getTempUrl() {
    return mTempUrl;
  }
}
