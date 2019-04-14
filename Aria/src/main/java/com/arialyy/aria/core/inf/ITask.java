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
package com.arialyy.aria.core.inf;

import com.arialyy.aria.core.download.DownloadGroupTask;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.upload.UploadTask;

/**
 * Created by lyy on 2017/2/13.
 * 任务接口{@link DownloadTask}、{@link UploadTask}、{@link DownloadGroupTask}
 */
public interface ITask<TASK_WRAPPER extends AbsTaskWrapper> {

  /**
   * 普通下载任务
   */
  int DOWNLOAD = 1;
  /**
   * 上传任务
   */
  int UPLOAD = 2;
  /**
   * 组合任务
   */
  int DOWNLOAD_GROUP = 3;
  /**
   * 组合任务的子任务
   */
  int DOWNLOAD_GROUP_SUB = 4;
  /**
   * 未知
   */
  int OTHER = -1;

  /**
   * 获取任务类型
   *
   * @return {@link #DOWNLOAD}、{@link #UPLOAD}、{@link #DOWNLOAD_GROUP}
   */
  int getTaskType();

  /**
   * 获取下载状态
   */
  int getState();

  /**
   * 唯一标识符，DownloadTask 为下载地址，UploadTask 为文件路径
   */
  String getKey();

  /**
   * 任务是否正在执行
   *
   * @return true，正在执行；
   */
  boolean isRunning();

  /**
   * 获取信息实体
   */
  TASK_WRAPPER getTaskWrapper();

  void start();

  /**
   * 停止任务
   */
  void stop();

  /**
   * 停止任务
   *
   * @param type {@code 0}默认操作，{@code 1}停止任务不自动执行下一任务
   */
  void stop(int type);

  /**
   * 删除任务
   */
  void cancel();

  /**
   * 原始byte速度
   */
  long getSpeed();

  /**
   * 转换单位后的速度
   */
  String getConvertSpeed();

  /**
   * 获取百分比进度
   */
  int getPercent();

  /**
   * 原始文件byte长度
   */
  long getFileSize();

  /**
   * 转换单位后的文件长度
   */
  String getConvertFileSize();

  /**
   * 获取当前进度
   */
  long getCurrentProgress();

  /**
   * 获取单位转换后的进度
   *
   * @return 返回 3mb
   */
  String getConvertCurrentProgress();
}
