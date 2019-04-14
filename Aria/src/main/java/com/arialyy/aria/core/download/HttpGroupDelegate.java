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
import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by lyy on 2019/4/9.
 *
 * http组合任务功能代理
 */
class HttpGroupDelegate extends AbsGroupDelegate<DownloadGroupTarget> {

  /**
   * 子任务下载地址，
   */
  private List<String> mUrls = new ArrayList<>();

  /**
   * 子任务文件名
   */
  private List<String> mSubNameTemp = new ArrayList<>();

  HttpGroupDelegate(DownloadGroupTarget target, DGTaskWrapper wrapper) {
    super(target, wrapper);
    mUrls.addAll(wrapper.getEntity().getUrls());
  }

  @CheckResult
  DownloadGroupTarget setGroupUrl(List<String> urls) {
    mUrls.clear();
    mUrls.addAll(urls);
    return getTarget();
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   */
  @CheckResult
  DownloadGroupTarget setSubFileName(List<String> subTaskFileName) {
    if (subTaskFileName == null || subTaskFileName.isEmpty()) {
      ALog.e(TAG, "修改子任务的文件名失败：列表为null");
      return getTarget();
    }
    if (subTaskFileName.size() != getTaskWrapper().getSubTaskWrapper().size()) {
      ALog.e(TAG, "修改子任务的文件名失败：子任务文件名列表数量和子任务的数量不匹配");
      return getTarget();
    }
    mSubNameTemp.clear();
    mSubNameTemp.addAll(subTaskFileName);
    return getTarget();
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult
  DownloadGroupTarget updateUrls(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      throw new NullPointerException("下载地址列表为空");
    }
    if (urls.size() != mUrls.size()) {
      throw new IllegalArgumentException("新下载地址数量和旧下载地址数量不一致");
    }
    mUrls.clear();
    mUrls.addAll(urls);
    String newHash = CommonUtil.getMd5Code(urls);
    setGroupHash(newHash);
    getEntity().setGroupHash(newHash);
    getEntity().update();
    if (getEntity().getSubEntities() != null && !getEntity().getSubEntities().isEmpty()) {
      for (DownloadEntity de : getEntity().getSubEntities()) {
        de.setGroupHash(newHash);
        de.update();
      }
    }
    return getTarget();
  }

  @Override public boolean checkEntity() {
    if (!checkDirPath()) {
      return false;
    }

    if (!checkSubName()) {
      return false;
    }

    if (!checkUrls()) {
      return false;
    }

    if (getTaskWrapper().getEntity().getFileSize() == 0) {
      ALog.e(TAG, "组合任务必须设置文件文件大小");
      return false;
    }

    if (getTaskWrapper().asHttp().getRequestEnum() == RequestEnum.POST) {
      for (DTaskWrapper subTask : getTaskWrapper().getSubTaskWrapper()) {
        subTask.asHttp().setRequestEnum(RequestEnum.POST);
      }
    }

    getEntity().save();

    if (isNeedModifyPath()) {
      reChangeDirPath(getDirPathTemp());
    }

    if (!mSubNameTemp.isEmpty()) {
      updateSingleSubFileName();
    }
    return true;
  }

  /**
   * 更新所有改动的子任务文件名
   */
  private void updateSingleSubFileName() {
    List<DTaskWrapper> entities = getTaskWrapper().getSubTaskWrapper();
    int i = 0;
    for (DTaskWrapper entity : entities) {
      if (i < mSubNameTemp.size()) {
        String newName = mSubNameTemp.get(i);
        updateSingleSubFileName(entity, newName);
      }
      i++;
    }
  }

  /**
   * 检查urls是否合法，并删除不合法的子任务
   *
   * @return {@code true} 合法
   */
  private boolean checkUrls() {
    if (mUrls.isEmpty()) {
      ALog.e(TAG, "下载失败，子任务下载列表为null");
      return false;
    }
    Set<Integer> delItem = new HashSet<>();

    int i = 0;
    for (String url : mUrls) {
      if (TextUtils.isEmpty(url)) {
        ALog.e(TAG, "子任务url为null，即将删除该子任务。");
        delItem.add(i);
        continue;
      } else if (!url.startsWith("http")) {
        //} else if (!url.startsWith("http") && !url.startsWith("ftp")) {
        ALog.e(TAG, "子任务url【" + url + "】错误，即将删除该子任务。");
        delItem.add(i);
        continue;
      }
      int index = url.indexOf("://");
      if (index == -1) {
        ALog.e(TAG, "子任务url【" + url + "】不合法，即将删除该子任务。");
        delItem.add(i);
        continue;
      }

      i++;
    }

    for (int index : delItem) {
      mUrls.remove(index);
      if (mSubNameTemp != null && !mSubNameTemp.isEmpty()) {
        mSubNameTemp.remove(index);
      }
    }

    getEntity().setGroupHash(CommonUtil.getMd5Code(mUrls));

    return true;
  }

  /**
   * 更新单个子任务文件名
   */
  private void updateSingleSubFileName(DTaskWrapper taskEntity, String newName) {
    DownloadEntity entity = taskEntity.getEntity();
    if (!newName.equals(entity.getFileName())) {
      String oldPath = getEntity().getDirPath() + "/" + entity.getFileName();
      String newPath = getEntity().getDirPath() + "/" + newName;
      if (DbEntity.checkDataExist(DownloadEntity.class, "downloadPath=? or isComplete='true'",
          newPath)) {
        ALog.w(TAG, String.format("更新文件名失败，路径【%s】已存在或文件已下载", newPath));
        return;
      }

      CommonUtil.modifyTaskRecord(oldPath, newPath);
      entity.setDownloadPath(newPath);
      entity.setFileName(newName);
      entity.update();
    }
  }

  /**
   * 如果用户设置了子任务文件名，检查子任务文件名
   *
   * @return {@code true} 合法
   */
  private boolean checkSubName() {
    if (mSubNameTemp == null || mSubNameTemp.isEmpty()) {
      return true;
    }
    if (mUrls.size() != mSubNameTemp.size()) {
      ALog.e(TAG, "子任务文件名必须和子任务数量一致");
      return false;
    }

    return true;
  }
}
