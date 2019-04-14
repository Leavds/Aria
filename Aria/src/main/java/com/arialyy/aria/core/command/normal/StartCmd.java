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

package com.arialyy.aria.core.command.normal;

import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.common.QueueMod;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.inf.AbsTask;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.manager.TaskWrapperManager;
import com.arialyy.aria.core.queue.DownloadGroupTaskQueue;
import com.arialyy.aria.core.queue.DownloadTaskQueue;
import com.arialyy.aria.core.queue.UploadTaskQueue;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.NetUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyy on 2016/8/22. 开始命令 队列模型{@link QueueMod#NOW}、{@link QueueMod#WAIT}
 */
class StartCmd<T extends AbsTaskWrapper> extends AbsNormalCmd<T> {

  StartCmd(T entity, int taskType) {
    super(entity, taskType);
  }

  @Override public void executeCmd() {
    if (!canExeCmd) return;
    if (!NetUtils.isConnected(AriaManager.APP)) {
      ALog.e(TAG, "启动任务失败，网络未连接");
      return;
    }
    String mod;
    int maxTaskNum = mQueue.getMaxTaskNum();
    AriaManager manager = AriaManager.getInstance(AriaManager.APP);
    if (isDownloadCmd) {
      mod = manager.getDownloadConfig().getQueueMod();
    } else {
      mod = manager.getUploadConfig().getQueueMod();
    }

    AbsTask task = getTask();
    if (task == null) {
      task = createTask();
      // 任务不存在时，根据配置不同，对任务执行操作
      if (mod.equals(QueueMod.NOW.getTag())) {
        startTask();
      } else if (mod.equals(QueueMod.WAIT.getTag())) {
        int state = task.getState();
        if (mQueue.getCurrentExePoolNum() < maxTaskNum) {
          if (state == IEntity.STATE_STOP
              || task.getState() == IEntity.STATE_FAIL
              || task.getState() == IEntity.STATE_OTHER
              || task.getState() == IEntity.STATE_PRE
              || task.getState() == IEntity.STATE_POST_PRE
              || task.getState() == IEntity.STATE_COMPLETE) {
            resumeTask();
          } else {
            startTask();
          }
        } else {
          sendWaitState(task);
        }
      }
    } else {
      //任务没执行并且执行队列中没有该任务，才认为任务没有运行中
      if (!task.isRunning() && !mQueue.taskIsRunning(task.getKey()) && !mQueue.taskExists(
          task.getKey())) {
        resumeTask();
      } else {
        ALog.w(TAG, String.format("任务【%s】已经在运行", task.getTaskName()));
      }
    }
    if (mQueue.getCurrentCachePoolNum() == 0) {
      findAllWaitTask();
    }
  }

  /**
   * 当缓冲队列为null时，查找数据库中所有等待中的任务
   */
  private void findAllWaitTask() {
    new Thread(new WaitTaskThread()).start();
  }

  private class WaitTaskThread implements Runnable {

    @Override public void run() {
      if (isDownloadCmd) {
        handleTask(findWaitData(1));
        handleTask(findWaitData(2));
      } else {
        handleTask(findWaitData(3));
      }
    }

    private List<AbsTaskWrapper> findWaitData(int type) {
      List<AbsTaskWrapper> waitList = new ArrayList<>();
      TaskWrapperManager tManager = TaskWrapperManager.getInstance();
      if (type == 1) { // 普通下载任务
        List<DownloadEntity> dEntities = DbEntity.findDatas(DownloadEntity.class,
            "isGroupChild=? and state=?", "false", "3");
        if (dEntities != null && !dEntities.isEmpty()) {
          for (DownloadEntity e : dEntities) {
            if (e.getTaskType() == AbsTaskWrapper.D_FTP) {
              waitList.add(tManager.getFtpTaskWrapper(DTaskWrapper.class, e.getKey()));
            } else if (e.getTaskType() == AbsTaskWrapper.D_HTTP) {
              waitList.add(tManager.getHttpTaskWrapper(DTaskWrapper.class, e.getKey()));
            }
          }
        }
      } else if (type == 2) { // 组合任务
        List<DownloadGroupEntity> dEntities =
            DbEntity.findDatas(DownloadGroupEntity.class, "state=?", "3");
        if (dEntities != null && !dEntities.isEmpty()) {
          for (DownloadGroupEntity e : dEntities) {
            if (e.getTaskType() == AbsTaskWrapper.DG_HTTP) {
              waitList.add(tManager.getDGTaskWrapper(DGTaskWrapper.class, e.getUrls()));
            } else if (e.getTaskType() == AbsTaskWrapper.D_FTP_DIR) {
              waitList.add(tManager.getFtpTaskWrapper(DGTaskWrapper.class, e.getKey()));
            }
          }
        }
      } else if (type == 3) { //普通上传任务
        List<UploadEntity> dEntities = DbEntity.findDatas(UploadEntity.class, "state=?", "3");

        if (dEntities != null && !dEntities.isEmpty()) {
          for (UploadEntity e : dEntities) {
            if (e.getTaskType() == AbsTaskWrapper.D_FTP) {
              waitList.add(tManager.getFtpTaskWrapper(UTaskWrapper.class, e.getKey()));
            } else if (e.getTaskType() == AbsTaskWrapper.D_HTTP) {
              waitList.add(tManager.getHttpTaskWrapper(UTaskWrapper.class, e.getKey()));
            }
          }
        }
      }
      return waitList;
    }

    private void handleTask(List<AbsTaskWrapper> waitList) {
      for (AbsTaskWrapper te : waitList) {
        if (te.getEntity() == null) continue;
        AbsTask task = getTask(te.getEntity());
        if (task != null) continue;
        if (te instanceof DTaskWrapper) {
          if (te.getRequestType() == AbsTaskWrapper.D_FTP
              || te.getRequestType() == AbsTaskWrapper.U_FTP) {
            te.asFtp().setUrlEntity(CommonUtil.getFtpUrlInfo(te.getEntity().getKey()));
          }
          mQueue = DownloadTaskQueue.getInstance();
        } else if (te instanceof UTaskWrapper) {
          mQueue = UploadTaskQueue.getInstance();
        } else if (te instanceof DGTaskWrapper) {
          mQueue = DownloadGroupTaskQueue.getInstance();
        }
        createTask(te);
      }
    }
  }
}