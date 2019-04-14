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
import com.arialyy.aria.core.inf.ITargetHandler;
import com.arialyy.aria.core.inf.INormalTarget;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;

/**
 * Created by AriaL on 2019/4/5.
 * 普通下载任务通用功能处理
 */
class DNormalDelegate<TARGET extends AbsDTarget> implements INormalTarget {
  private final String TAG = "DNormalDelegate";
  private DownloadEntity mEntity;

  private TARGET mTarget;
  private String mNewUrl;
  /**
   * 设置的文件保存路径的临时变量
   */
  private String mTempFilePath;

  /**
   * {@code true}强制下载，不考虑文件路径是否被占用
   */
  private boolean forceDownload = false;
  /**
   * 资源地址
   */
  private String mUrl;

  DNormalDelegate(TARGET target, String url, String targetName) {
    this.mTarget = target;
    initTarget(url, targetName);
  }

  private void initTarget(String url, String targetName) {
    DTaskWrapper taskWrapper =
        TaskWrapperManager.getInstance().getHttpTaskWrapper(DTaskWrapper.class, url);
    mEntity = taskWrapper.getEntity();

    mUrl = url;
    mTarget.setTargetName(targetName);
    mTarget.setTaskWrapper(taskWrapper);
    if (mEntity != null) {
      mTempFilePath = mEntity.getDownloadPath();
    }
  }

  TARGET updateUrl(String newUrl) {
    if (TextUtils.isEmpty(newUrl)) {
      ALog.e(TAG, "url更新失败，newUrl为null");
      return mTarget;
    }
    if (mUrl.equals(newUrl)) {
      ALog.e(TAG, "url更新失败，新的下载url和旧的url一致");
      return mTarget;
    }
    mNewUrl = newUrl;
    mTarget.getTaskWrapper().setRefreshInfo(true);
    return mTarget;
  }

  @Override public DownloadEntity getEntity() {
    return mTarget.getEntity();
  }

  @Override public boolean taskExists() {
    return DbEntity.checkDataExist(DownloadEntity.class, "url=?", mUrl);
  }

  @Override public boolean isRunning() {
    DownloadTask task = DownloadTaskQueue.getInstance().getTask(mEntity.getKey());
    return task != null && task.isRunning();
  }

  @Override public boolean checkEntity() {
    boolean b = checkUrl() && checkFilePath();
    if (b) {
      mEntity.save();
    }
    return b;
  }

  @Override public boolean checkFilePath() {
    String filePath = mTempFilePath;
    if (TextUtils.isEmpty(filePath)) {
      ALog.e(TAG, "下载失败，文件保存路径为null");
      return false;
    } else if (!filePath.startsWith("/")) {
      ALog.e(TAG, "下载失败，文件保存路径【" + filePath + "】错误");
      return false;
    }
    File file = new File(filePath);
    if (file.isDirectory()) {
      if (mTarget.getTargetType() == ITargetHandler.HTTP) {
        ALog.e(TAG, "下载失败，保存路径【" + filePath + "】不能为文件夹，路径需要是完整的文件路径，如：/mnt/sdcard/game.zip");
        return false;
      } else if (mTarget.getTargetType() == ITargetHandler.FTP) {
        filePath += mEntity.getFileName();
      }
    } else {
      // http文件名设置
      if (TextUtils.isEmpty(mEntity.getFileName())) {
        mEntity.setFileName(file.getName());
      }
    }

    //设置文件保存路径，如果新文件路径和旧文件路径不同，则修改路径
    if (!filePath.equals(mEntity.getDownloadPath())) {
      // 检查路径冲突
      if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=?", filePath)) {
        if (!forceDownload) {
          ALog.e(TAG, "下载失败，保存路径【" + filePath + "】已经被其它任务占用，请设置其它保存路径");
          return false;
        } else {
          ALog.w(TAG, "保存路径【" + filePath + "】已经被其它任务占用，当前任务将覆盖该路径的文件");
          CommonUtil.delTaskRecord(filePath, 1);
          mTarget.setTaskWrapper(
              TaskWrapperManager.getInstance()
                  .getHttpTaskWrapper(DTaskWrapper.class, mUrl));
        }
      }
      File oldFile = new File(mEntity.getDownloadPath());
      File newFile = new File(filePath);
      mEntity.setDownloadPath(filePath);
      mEntity.setFileName(newFile.getName());
      if (oldFile.exists()) {
        // 处理普通任务的重命名
        CommonUtil.modifyTaskRecord(oldFile.getPath(), newFile.getPath());
        ALog.i(TAG, String.format("将任务重命名为：%s", newFile.getName()));
      } else if (CommonUtil.blockTaskExists(oldFile.getPath())) {
        // 处理分块任务的重命名
        CommonUtil.modifyTaskRecord(oldFile.getPath(), newFile.getPath());
        ALog.i(TAG, String.format("将分块任务重命名为：%s", newFile.getName()));
      }
    }
    return true;
  }

  @Override public boolean checkUrl() {
    final String url = mEntity.getUrl();
    if (TextUtils.isEmpty(url)) {
      ALog.e(TAG, "下载失败，url为null");
      return false;
    } else if (!url.startsWith("http") && !url.startsWith("ftp")) {
      ALog.e(TAG, "下载失败，url【" + url + "】错误");
      return false;
    }
    int index = url.indexOf("://");
    if (index == -1) {
      ALog.e(TAG, "下载失败，url【" + url + "】不合法");
      return false;
    }
    if (!TextUtils.isEmpty(mNewUrl)) {
      mEntity.setUrl(mNewUrl);
    }
    return true;
  }

  void setForceDownload(boolean forceDownload) {
    this.forceDownload = forceDownload;
  }

  void setUrl(String url) {
    this.mUrl = url;
  }

  String getUrl() {
    return mUrl;
  }

  void setTempFilePath(String mTempFilePath) {
    this.mTempFilePath = mTempFilePath;
  }
}
