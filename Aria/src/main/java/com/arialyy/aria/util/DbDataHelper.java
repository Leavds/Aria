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
package com.arialyy.aria.util;

import com.arialyy.aria.core.common.RecordWrapper;
import com.arialyy.aria.core.common.TaskRecord;
import com.arialyy.aria.core.download.DGEntityWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.orm.DbEntity;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库帮助类
 */
public class DbDataHelper {

  /**
   * 获取任务记录
   *
   * @param filePath 文件地址
   * @return 没有记录返回null，有记录则返回任务记录
   */
  public static TaskRecord getTaskRecord(String filePath) {
    List<RecordWrapper> records =
        DbEntity.findRelationData(RecordWrapper.class, "TaskRecord.filePath=?", filePath);
    if (records == null || records.size() == 0) {
      return null;
    }
    return records.get(0).taskRecord;
  }

  /**
   * 获取组合任务实体 如果数据库不存在该实体，则新创建一个新的任务组实体
   *
   * @param groupHash 组合任务Hash
   * @param urls 子任务url列表
   */
  public static DownloadGroupEntity getHttpDGEntity(String groupHash, List<String> urls) {
    List<DGEntityWrapper> wrapper =
        DbEntity.findRelationData(DGEntityWrapper.class, "DownloadGroupEntity.groupHash=?",
            groupHash);

    DownloadGroupEntity groupEntity;
    if (wrapper != null && !wrapper.isEmpty()) {
      groupEntity = wrapper.get(0).groupEntity;
      if (groupEntity == null) {
        groupEntity = new DownloadGroupEntity();
        groupEntity.setSubEntities(createHttpSubTask(groupHash, urls));
      }
    } else {
      groupEntity = new DownloadGroupEntity();
      groupEntity.setSubEntities(createHttpSubTask(groupHash, urls));
    }
    groupEntity.setGroupHash(groupHash);
    groupEntity.setUrls(urls);
    return groupEntity;
  }

  /**
   * 创建HTTP子任务实体
   */
  private static List<DownloadEntity> createHttpSubTask(String groupHash, List<String> urls) {
    List<DownloadEntity> list = new ArrayList<>();
    for (int i = 0, len = urls.size(); i < len; i++) {
      String url = urls.get(i);
      DownloadEntity entity = new DownloadEntity();
      entity.setUrl(url);
      entity.setDownloadPath(groupHash + "_" + i);
      int lastIndex = url.lastIndexOf(File.separator);
      entity.setFileName(url.substring(lastIndex + 1));
      entity.setGroupHash(groupHash);
      entity.setGroupChild(true);
      list.add(entity);
    }
    return list;
  }

  /**
   * 通过Ftp下载地址获取组合任务实体
   *
   * @param ftpUrl ftp下载地址
   */
  public static DownloadGroupEntity getFtpDGEntity(String ftpUrl) {
    List<DGEntityWrapper> wrapper =
        DbEntity.findRelationData(DGEntityWrapper.class, "DownloadGroupEntity.groupHash=?",
            ftpUrl);
    DownloadGroupEntity groupEntity;
    if (wrapper != null && !wrapper.isEmpty()) {
      groupEntity = wrapper.get(0).groupEntity;
      if (groupEntity == null) {
        groupEntity = new DownloadGroupEntity();
      }
    } else {
      groupEntity = new DownloadGroupEntity();
    }
    groupEntity.setGroupHash(ftpUrl);
    return groupEntity;
  }
}
