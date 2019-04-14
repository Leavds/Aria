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

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.command.ICmd;
import com.arialyy.aria.core.command.normal.CancelAllCmd;
import com.arialyy.aria.core.command.normal.NormalCmdFactory;
import com.arialyy.aria.core.common.ProxyHelper;
import com.arialyy.aria.core.inf.AbsReceiver;
import com.arialyy.aria.core.inf.ReceiverType;
import com.arialyy.aria.core.scheduler.UploadSchedulers;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.util.List;
import java.util.Set;

/**
 * Created by lyy on 2017/2/6.
 * 上传功能接收器
 */
public class UploadReceiver extends AbsReceiver {
  private static final String TAG = "UploadReceiver";

  /**
   * 设置最大上传速度，单位：kb
   *
   * @param maxSpeed 为0表示不限速
   * @deprecated 请使用 {@code Aria.get(Context).getUploadConfig().setMaxSpeed(int)}
   */
  @Deprecated
  public UploadReceiver setMaxSpeed(int maxSpeed) {
    AriaManager.getInstance(AriaManager.APP).getUploadConfig().setMaxSpeed(maxSpeed);
    return this;
  }

  /**
   * 加载HTTP单文件上传任务
   *
   * @param filePath 文件路径
   */
  @CheckResult
  public UploadTarget load(@NonNull String filePath) {
    CheckUtil.checkUploadPath(filePath);
    return new UploadTarget(filePath, targetName);
  }

  /**
   * 加载FTP单文件上传任务
   *
   * @param filePath 文件路径
   */
  @CheckResult
  public FtpUploadTarget loadFtp(@NonNull String filePath) {
    CheckUtil.checkUploadPath(filePath);
    return new FtpUploadTarget(filePath, targetName);
  }

  /**
   * 通过上传路径获取上传实体
   * 如果任务不存在，方便null
   */
  public UploadEntity getUploadEntity(String filePath) {
    if (TextUtils.isEmpty(filePath)) {
      return null;
    }
    return DbEntity.findFirst(UploadEntity.class, "filePath=?", filePath);
  }

  /**
   * 上传任务是否存在
   *
   * @return {@code true}存在，{@code false} 不存在
   */
  public boolean taskExists(String filePath) {
    return DbEntity.checkDataExist(UTaskWrapper.class, "key=?", filePath);
  }

  /**
   * 获取所有普通上传任务
   * 获取未完成的普通任务列表{@link #getAllNotCompleteTask()}
   * 获取已经完成的普通任务列表{@link #getAllCompleteTask()}
   */
  public List<UploadEntity> getTaskList() {
    return DbEntity.findAllData(UploadEntity.class);
  }

  /**
   * 分页获取所有普通上传任务
   * 获取未完成的普通任务列表{@link #getAllNotCompleteTask()}
   * 获取已经完成的普通任务列表{@link #getAllCompleteTask()}
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<UploadEntity> getTaskList(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(UploadEntity.class, page, num,
        "isGroupChild=? and downloadPath!=''", "false");
  }

  /**
   * 获取所有未完成的普通上传任务
   */
  public List<UploadEntity> getAllNotCompleteTask() {
    return DbEntity.findDatas(UploadEntity.class,
        "isGroupChild=? and isComplete=?", "false", "false");
  }

  /**
   * 分页获取所有未完成的普通上传任务
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<UploadEntity> getAllNotCompleteTask(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(UploadEntity.class, page, num,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "false");
  }

  /**
   * 获取所有已经完成的普通任务
   */
  public List<UploadEntity> getAllCompleteTask() {
    return DbEntity.findDatas(UploadEntity.class, "isGroupChild=? and isComplete=?", "false",
        "true");
  }

  /**
   * 分页获取所有已经完成的普通任务
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<UploadEntity> getAllCompleteTask(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(UploadEntity.class,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "true");
  }

  /**
   * 停止所有正在下载的任务，并清空等待队列。
   */
  public void stopAllTask() {
    AriaManager.getInstance(AriaManager.APP)
        .setCmd(NormalCmdFactory.getInstance()
            .createCmd(new UTaskWrapper(null), NormalCmdFactory.TASK_STOP_ALL,
                ICmd.TASK_TYPE_UPLOAD))
        .exe();
  }

  /**
   * 删除所有任务
   *
   * @param removeFile {@code true} 删除已经上传完成的任务，不仅删除上传记录，还会删除已经上传完成的文件，{@code false}
   * 如果文件已经上传完成，只删除上传记录
   */
  public void removeAllTask(boolean removeFile) {
    final AriaManager am = AriaManager.getInstance(AriaManager.APP);
    CancelAllCmd cancelCmd =
        (CancelAllCmd) CommonUtil.createNormalCmd(new UTaskWrapper(null),
            NormalCmdFactory.TASK_CANCEL_ALL, ICmd.TASK_TYPE_UPLOAD);
    cancelCmd.removeFile = removeFile;
    am.setCmd(cancelCmd).exe();
    Set<String> keys = am.getReceiver().keySet();
    for (String key : keys) {
      am.getReceiver().remove(key);
    }
  }

  /**
   * 将当前类注册到Aria
   */
  public void register() {
    if (TextUtils.isEmpty(targetName)) {
      ALog.e(TAG, "upload register target null");
      return;
    }
    Object obj = OBJ_MAP.get(getKey());
    if (obj == null) {
      ALog.e(TAG, String.format("【%s】观察者为空", targetName));
      return;
    }
    Set<Integer> set = ProxyHelper.getInstance().checkProxyType(obj.getClass());
    if (set != null && !set.isEmpty()) {
      for (Integer type : set) {
        if (type == ProxyHelper.PROXY_TYPE_UPLOAD) {
          UploadSchedulers.getInstance().register(obj);
        }
      }
    } else {
      ALog.i(TAG, "没有Aria的注解方法");
    }
  }

  /**
   * 取消注册，如果是Activity或fragment，Aria会界面销毁时自动调用该方法。
   * 如果是Dialog或popupwindow，需要你在撤销界面时调用该方法
   */
  @Override public void unRegister() {
    if (needRmListener) {
      unRegisterListener();
    }
    AriaManager.getInstance(AriaManager.APP).removeReceiver(OBJ_MAP.get(getKey()));
  }

  @Override public String getType() {
    return ReceiverType.UPLOAD;
  }

  @Override protected void unRegisterListener() {
    if (TextUtils.isEmpty(targetName)) {
      ALog.e(TAG, "upload unRegisterListener target null");
      return;
    }
    Object obj = OBJ_MAP.get(getKey());
    if (obj == null) {
      ALog.e(TAG, String.format("【%s】观察者为空", targetName));
      return;
    }

    Set<Integer> set = ProxyHelper.getInstance().mProxyCache.get(obj.getClass().getName());
    if (set != null) {
      for (Integer integer : set) {
        if (integer == ProxyHelper.PROXY_TYPE_UPLOAD) {
          UploadSchedulers.getInstance().unRegister(obj);
        }
      }
    }
  }
}