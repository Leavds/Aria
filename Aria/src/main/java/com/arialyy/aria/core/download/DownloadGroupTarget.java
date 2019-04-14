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
import com.arialyy.aria.core.common.http.HttpHeaderDelegate;
import com.arialyy.aria.core.common.http.PostDelegate;
import com.arialyy.aria.core.inf.IHttpHeaderDelegate;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.exception.ParamException;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Created by AriaL on 2017/6/29.
 * 下载任务组
 */
public class DownloadGroupTarget extends AbsDGTarget<DownloadGroupTarget> implements
    IHttpHeaderDelegate<DownloadGroupTarget> {
  private HttpHeaderDelegate<DownloadGroupTarget> mHeaderDelegate;
  private HttpGroupDelegate mGroupDelegate;

  DownloadGroupTarget(DownloadGroupEntity groupEntity, String targetName) {
    setTargetName(targetName);
    if (groupEntity.getUrls() != null && !groupEntity.getUrls().isEmpty()) {
      init(groupEntity.getUrls());
    } else {
      throw new ParamException("组合任务只任务下载地址为空");
    }
  }

  DownloadGroupTarget(List<String> urls, String targetName) {
    setTargetName(targetName);
    init(urls);
  }

  private void init(List<String> urls) {
    mGroupDelegate = new HttpGroupDelegate(this,
        TaskWrapperManager.getInstance().getDGTaskWrapper(DGTaskWrapper.class, urls));
    mHeaderDelegate = new HttpHeaderDelegate<>(this);
  }

  /**
   * Post处理
   */
  @CheckResult
  public PostDelegate asPost() {
    return new PostDelegate<>(this);
  }

  /**
   * 更新组合任务下载地址
   *
   * @param urls 新的组合任务下载地址列表
   */
  @CheckResult
  public DownloadGroupTarget updateUrls(List<String> urls) {
    return mGroupDelegate.updateUrls(urls);
  }

  /**
   * 任务组总任务大小，任务组是一个抽象的概念，没有真实的数据实体，任务组的大小是Aria动态获取子任务大小相加而得到的，
   * 如果你知道当前任务组总大小，你也可以调用该方法给任务组设置大小
   *
   * 为了更好的用户体验，组合任务必须设置文件大小
   *
   * @param fileSize 任务组总大小
   */
  @CheckResult
  public DownloadGroupTarget setFileSize(long fileSize) {
    if (fileSize <= 0) {
      ALog.e(TAG, "文件大小不能小于 0");
      return this;
    }
    if (getEntity().getFileSize() <= 1 || getEntity().getFileSize() != fileSize) {
      getEntity().setFileSize(fileSize);
    }
    return this;
  }

  /**
   * 如果你是使用{@link DownloadReceiver#load(DownloadGroupEntity)}进行下载操作，那么你需要设置任务组的下载地址
   */
  @CheckResult
  public DownloadGroupTarget setGroupUrl(List<String> urls) {
    return mGroupDelegate.setGroupUrl(urls);
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   *
   * @deprecated {@link #setSubFileName(List)} 请使用该api
   */
  @CheckResult
  @Deprecated public DownloadGroupTarget setSubTaskFileName(List<String> subTaskFileName) {
    return setSubFileName(subTaskFileName);
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
  public DownloadGroupTarget setDirPath(String dirPath) {
    return mGroupDelegate.setDirPath(dirPath);
  }

  /**
   * 设置子任务文件名，该方法必须在{@link #setDirPath(String)}之后调用，否则不生效
   */
  @CheckResult
  public DownloadGroupTarget setSubFileName(List<String> subTaskFileName) {
    return mGroupDelegate.setSubFileName(subTaskFileName);
  }

  @Override public int getTargetType() {
    return GROUP_HTTP;
  }

  @Override protected boolean checkEntity() {
    return mGroupDelegate.checkEntity();
  }

  @Override public boolean isRunning() {
    return mGroupDelegate.isRunning();
  }

  @Override public boolean taskExists() {
    return mGroupDelegate.taskExists();
  }

  @CheckResult
  @Override public DownloadGroupTarget addHeader(@NonNull String key, @NonNull String value) {
    for (DTaskWrapper subTask : getTaskWrapper().getSubTaskWrapper()) {
      mHeaderDelegate.addHeader(subTask, key, value);
    }
    return mHeaderDelegate.addHeader(key, value);
  }

  @CheckResult
  @Override public DownloadGroupTarget addHeaders(Map<String, String> headers) {
    for (DTaskWrapper subTask : getTaskWrapper().getSubTaskWrapper()) {
      mHeaderDelegate.addHeaders(subTask, headers);
    }
    return mHeaderDelegate.addHeaders(headers);
  }

  @CheckResult
  @Override public DownloadGroupTarget setUrlProxy(Proxy proxy) {
    return mHeaderDelegate.setUrlProxy(proxy);
  }
}
