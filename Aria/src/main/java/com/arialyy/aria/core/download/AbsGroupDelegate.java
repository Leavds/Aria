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
import android.text.TextUtils;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.IGroupTarget;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyy on 2019/4/9.
 * 下载组合任务功能
 */
abstract class AbsGroupDelegate<TARGET extends AbsDGTarget> implements IGroupTarget {
  protected String TAG;
  private TARGET mTarget;
  private DGTaskWrapper mWrapper;
  /**
   * 组任务名
   */
  private String mGroupHash;
  /**
   * 文件夹临时路径
   */
  private String mDirPathTemp;
  /**
   * 是否需要修改路径
   */
  private boolean needModifyPath = false;

  AbsGroupDelegate(TARGET target, DGTaskWrapper wrapper) {
    TAG = CommonUtil.getClassName(getClass());
    mTarget = target;
    mWrapper = wrapper;
    setGroupHash(wrapper.getKey());
    mTarget.setTaskWrapper(wrapper);
    if (getEntity() != null) {
      mDirPathTemp = getEntity().getDirPath();
    }
  }

  @Override public boolean isRunning() {
    DownloadGroupTask task = DownloadGroupTaskQueue.getInstance().getTask(getEntity().getKey());
    return task != null && task.isRunning();
  }

  @CheckResult
  TARGET setDirPath(String dirPath) {
    mDirPathTemp = dirPath;
    return mTarget;
  }

  /**
   * 改变任务组文件夹路径，修改文件夹路径会将子任务所有路径更换
   *
   * @param newDirPath 新的文件夹路径
   */
  void reChangeDirPath(String newDirPath) {
    List<DTaskWrapper> subTasks = mWrapper.getSubTaskWrapper();
    if (subTasks != null && !subTasks.isEmpty()) {
      List<DbEntity> des = new ArrayList<>();
      for (DTaskWrapper dte : subTasks) {
        DownloadEntity de = dte.getEntity();
        String oldPath = de.getDownloadPath();
        String newPath = newDirPath + "/" + de.getFileName();
        File file = new File(oldPath);
        if (file.exists()) {
          file.renameTo(new File(newPath));
        }
        de.setDownloadPath(newPath);
        des.add(de);
      }
      AbsEntity.saveAll(des);
    }
  }

  /**
   * 检查并设置文件夹路径
   *
   * @return {@code true} 合法
   */
  @Override public boolean checkDirPath() {
    if (TextUtils.isEmpty(mDirPathTemp)) {
      ALog.e(TAG, "文件夹路径不能为null");
      return false;
    } else if (!mDirPathTemp.startsWith("/")) {
      ALog.e(TAG, "文件夹路径【" + mDirPathTemp + "】错误");
      return false;
    }
    File file = new File(mDirPathTemp);
    if (file.isFile()) {
      ALog.e(TAG, "路径【" + mDirPathTemp + "】是文件，请设置文件夹路径");
      return false;
    }

    if (TextUtils.isEmpty(getEntity().getDirPath()) || !getEntity().getDirPath()
        .equals(mDirPathTemp)) {
      if (!file.exists()) {
        file.mkdirs();
      }
      needModifyPath = true;
      getEntity().setDirPath(mDirPathTemp);
      ALog.i(TAG, String.format("文件夹路径改变，将更新文件夹路径为：%s", mDirPathTemp));
    }
    return true;
  }

  @Override public DownloadGroupEntity getEntity() {
    return mWrapper.getEntity();
  }

  @Override public boolean taskExists() {
    return DbEntity.checkDataExist(DownloadGroupEntity.class, "groupHash=?", mWrapper.getKey());
  }

  DGTaskWrapper getTaskWrapper() {
    return mWrapper;
  }

  boolean isNeedModifyPath() {
    return needModifyPath;
  }

  String getDirPathTemp() {
    return mDirPathTemp;
  }

  TARGET getTarget() {
    return mTarget;
  }

  public String getGroupHash() {
    return mGroupHash;
  }

  public void setGroupHash(String groupHash) {
    this.mGroupHash = groupHash;
  }
}
