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
package com.arialyy.aria.core.common;

import android.content.Context;
import android.util.SparseArray;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.BaseDListener;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.inf.AbsNormalEntity;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.manager.ThreadTaskManager;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.DbDataHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/7/1. 任务处理器
 */
public abstract class AbsFileer<ENTITY extends AbsNormalEntity, TASK_WRAPPER extends AbsTaskWrapper<ENTITY>>
    implements Runnable {
  private static final String STATE = "_state_";
  private static final String RECORD = "_record_";
  /**
   * 分块文件路径
   */
  public static final String SUB_PATH = "%s.%s.part";
  /**
   * 小于1m的文件不启用多线程
   */
  protected static final long SUB_LEN = 1024 * 1024;
  protected static final int DOWNLOAD = 0x1;
  protected static final int UPLOAD = 0x2;

  private final String TAG = "AbsFileer";
  protected IEventListener mListener;
  protected TASK_WRAPPER mTaskWrapper;
  protected ENTITY mEntity;
  protected Context mContext;
  protected File mTempFile; //文件
  protected StateConstance mConstance;
  //总线程数
  protected int mTotalThreadNum;
  //已完成的线程数
  private int mCompleteThreadNum;
  private SparseArray<AbsThreadTask> mTask = new SparseArray<>();

  private ScheduledThreadPoolExecutor mTimer;
  @Deprecated private File mConfigFile;
  /**
   * 进度刷新间隔
   */
  private long mUpdateInterval = 1000;
  protected TaskRecord mRecord;

  protected AbsFileer(IEventListener listener, TASK_WRAPPER taskEntity) {
    mListener = listener;
    mTaskWrapper = taskEntity;
    mEntity = mTaskWrapper.getEntity();
    mContext = AriaManager.APP;
    mConstance = new StateConstance();
  }

  protected abstract int getType();

  public void setNewTask(boolean newTask) {
    mTaskWrapper.setNewTask(newTask);
  }

  public String getKey() {
    return mTaskWrapper.getKey();
  }

  public ENTITY getEntity() {
    return mEntity;
  }

  /**
   * 设置最大下载/上传速度
   *
   * @param maxSpeed 单位为：kb
   */
  public void setMaxSpeed(int maxSpeed) {
    for (int i = 0; i < mTotalThreadNum; i++) {
      AbsThreadTask task = mTask.get(i);
      if (task != null) {
        task.setMaxSpeed(maxSpeed);
      }
    }
  }

  /**
   * 重置任务状态
   */
  private void resetState() {
    closeTimer();
    mCompleteThreadNum = 0;
    mTotalThreadNum = 0;
    if (mTask != null && mTask.size() != 0) {
      for (int i = 0; i < mTask.size(); i++) {
        AbsThreadTask task = mTask.get(i);
        if (task != null && !task.isRunning()) {
          task.breakTask();
        }
      }
      mTask.clear();
    }
  }

  /**
   * 开始流程
   */
  private void startFlow() {
    if (isBreak()) {
      return;
    }
    resetState();
    mConstance.resetState();
    checkRecord();
    mConstance.isRunning = true;
    mConstance.TASK_RECORD = mRecord;
    if (!mTaskWrapper.isSupportBP()) {
      mTotalThreadNum = 1;
    } else {
      mTotalThreadNum =
          mTaskWrapper.isNewTask() ? setNewTaskThreadNum() : mTotalThreadNum;
    }
    mConstance.START_THREAD_NUM = mTotalThreadNum;

    // 处理分块和动态文件参数
    if (mTaskWrapper.isNewTask() && mTaskWrapper instanceof DTaskWrapper) {
      AriaManager manager = AriaManager.getInstance(AriaManager.APP);
      mRecord.isBlock = mTotalThreadNum > 1 && manager.getDownloadConfig().isUseBlock();
      // 线程数不等1并且没有使用块下载，则认为没有使用动态文件
      mRecord.isOpenDynamicFile = mTotalThreadNum == 1 || mRecord.isBlock;
    }

    /*
     * mTaskWrapper.getEntity().getFileSize() != mTempFile.length()为兼容以前老版本代码
     * 动态长度条件：
     * 1、总线程数为1，并且是新任务
     * 2、总线程数为1，不是新任务，但是长度不是文件全长度
     */
    if (mTotalThreadNum == 1 && (mTaskWrapper.getEntity().getFileSize() != mTempFile.length())) {
      mRecord.isOpenDynamicFile = true;
    }

    onPostPre();

    if (!mTaskWrapper.isSupportBP()) {
      handleNoSupportBP();
    } else {
      handleBreakpoint();
    }
    startTimer();
  }

  @Override public void run() {
    if (mConstance.isRunning) {
      return;
    }
    startFlow();
  }

  /**
   * 预处理完成
   */
  protected void onPostPre() {

  }

  /**
   * 设置新任务的最大线程数
   */
  protected abstract int setNewTaskThreadNum();

  /**
   * 启动进度获取定时器
   */
  private synchronized void startTimer() {
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (mConstance.isComplete()
            || mConstance.isStop()
            || mConstance.isCancel()
            || mConstance.isFail()
            || !mConstance.isRunning) {
          closeTimer();
        } else if (mConstance.CURRENT_LOCATION >= 0) {
          mListener.onProgress(mConstance.CURRENT_LOCATION);
        }
      }
    }, 0, mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  public synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  /**
   * 设置定时器更新间隔
   *
   * @param interval 单位毫秒，不能小于0
   */
  protected void setUpdateInterval(long interval) {
    if (interval < 0) {
      ALog.w(TAG, "更新间隔不能小于0，默认为1000毫秒");
      return;
    }
    mUpdateInterval = interval;
  }

  public long getFileSize() {
    return mEntity.getFileSize();
  }

  /**
   * 获取当前任务位置
   */
  public long getCurrentLocation() {
    return mConstance.CURRENT_LOCATION;
  }

  public synchronized boolean isRunning() {
    return mConstance.isRunning;
  }

  public synchronized void cancel() {
    if (mConstance.isCancel) {
      return;
    }
    closeTimer();
    mConstance.isRunning = false;
    mConstance.isCancel = true;
    new Thread(new Runnable() {
      @Override public void run() {

        for (int i = 0; i < mTask.size(); i++) {
          AbsThreadTask task = mTask.get(i);
          if (task != null) {
            task.cancel();
          }
        }
        ThreadTaskManager.getInstance().stopTaskThread(mTaskWrapper.getKey());
      }
    }).start();
  }

  public synchronized void stop() {
    if (mConstance.isStop) {
      return;
    }
    closeTimer();
    mConstance.isRunning = false;
    mConstance.isStop = true;
    if (mConstance.isComplete()) return;
    new Thread(new Runnable() {
      @Override public void run() {

        for (int i = 0; i < mTask.size(); i++) {
          AbsThreadTask task = mTask.get(i);
          if (task != null && !task.isThreadComplete()) {
            task.stop();
          }
        }
        ThreadTaskManager.getInstance().stopTaskThread(mTaskWrapper.getKey());
      }
    }).start();
  }

  /**
   * 直接调用的时候会自动启动线程执行
   */
  public synchronized void start() {
    if (mConstance.isRunning) {
      return;
    }
    new Thread(this).start();
  }

  public void resume() {
    start();
  }

  /**
   * 检查记录 对于分块任务： 子分块不存在或被删除，子线程将重新下载 对于普通任务： 预下载文件不存在，则任务任务呗删除
   */
  private void checkRecord() {
    mConfigFile = new File(CommonUtil.getFileConfigPath(false, mEntity.getFileName()));
    if (mConfigFile.exists()) {
      convertDb();
    } else {
      mRecord = DbDataHelper.getTaskRecord(getFilePath());
      if (mRecord == null) {
        initRecord(true);
      } else {
        if (mRecord.threadRecords == null || mRecord.threadRecords.isEmpty()) {
          initRecord(true);
        } else if (mRecord.isBlock) {
          handleBlockRecord();
        } else {
          handleNormalRecord();
        }
      }
    }
  }

  /**
   * 处理普通任务的记录
   */
  private void handleNormalRecord() {
    File file = new File(mRecord.filePath);
    if (!file.exists()) {
      ALog.w(TAG, String.format("文件【%s】不存在，任务将重新开始", file.getPath()));
      mRecord.deleteData();
      initRecord(true);
      return;
    }

    if (mRecord.isOpenDynamicFile) {
      ThreadRecord tr = mRecord.threadRecords.get(0);
      if (tr == null) {
        mTaskWrapper.setNewTask(true);
        mTotalThreadNum = 1;
        return;
      }
      if (file.length() > mEntity.getFileSize()) {
        ALog.i(TAG, String.format("文件【%s】错误，任务重新开始", file.getPath()));
        file.delete();
        tr.startLocation = 0;
        tr.isComplete = false;
        tr.endLocation = mEntity.getFileSize();
      } else if (file.length() == mEntity.getFileSize()) {
        tr.isComplete = true;
        mCompleteThreadNum++;
      } else {
        if (file.length() != tr.startLocation) {
          ALog.i(TAG, String.format("修正【%s】的进度记录为：%s", file.getPath(), file.length()));
          tr.startLocation = file.length();
          tr.isComplete = false;
        }
      }
    } else {
      for (ThreadRecord tr : mRecord.threadRecords) {
        if (tr.isComplete) {
          mCompleteThreadNum++;
        }
      }
    }
    mTotalThreadNum = mRecord.threadRecords.size();
    mTaskWrapper.setNewTask(false);
  }

  /**
   * 处理分块任务的记录
   */
  private void handleBlockRecord() {
    final int threadNum = mRecord.threadRecords.size();
    final long blockLen = mEntity.getFileSize() / threadNum;
    int i = 0;
    for (ThreadRecord tr : mRecord.threadRecords) {
      File temp = new File(String.format(SUB_PATH, mRecord.filePath, tr.threadId));
      if (!temp.exists()) {
        ALog.i(TAG, String.format("分块文件【%s】不存在，该分块将重新开始", temp.getPath()));
        tr.isComplete = false;
        tr.startLocation = -1;
      } else {
        if (tr.isComplete) {
          mCompleteThreadNum++;
        } else {
          long realLocation = i * blockLen + temp.length();
          ALog.i(TAG, String.format(
              "startLocation = %s; endLocation = %s; block = %s; tempLen = %s; i = %s",
              tr.startLocation, tr.endLocation, blockLen, temp.length(), i));
          if (tr.endLocation == realLocation) {
            ALog.i(TAG, String.format("分块【%s】已完成，更新记录", temp.getPath()));
            tr.startLocation = realLocation;
            tr.isComplete = true;
            mCompleteThreadNum++;
          } else {
            tr.isComplete = false;
            if (realLocation == tr.startLocation) {
              i++;
              continue;
            }
            if (realLocation > tr.startLocation) {
              ALog.i(TAG, String.format("修正分块【%s】的进度记录为：%s", temp.getPath(), realLocation));
              tr.startLocation = realLocation;
            } else {
              ALog.i(TAG, String.format("分块【%s】错误，将重新开始该分块", temp.getPath()));
              temp.delete();
              tr.startLocation = i * blockLen;
            }
          }
        }
      }
      i++;
    }
    mTotalThreadNum = mRecord.threadRecords.size();
    mTaskWrapper.setNewTask(false);
  }

  /**
   * convertDb 是兼容性代码 从3.4.1开始，线程配置信息将存储在数据库中。 将配置文件的内容复制到数据库中，并将配置文件删除
   */
  private void convertDb() {
    List<RecordWrapper> records =
        DbEntity.findRelationData(RecordWrapper.class, "TaskRecord.filePath=?",
            getFilePath());
    if (records == null || records.size() == 0) {
      Properties pro = CommonUtil.loadConfig(mConfigFile);
      if (pro.isEmpty()) {
        mTaskWrapper.setNewTask(true);
        return;
      }
      initRecord(false);
      Set<Object> keys = pro.keySet();
      // 老版本记录是5s存一次，但是5s中内，如果线程执行完成，record记录是没有的，只有state记录...
      // 第一步应该是record 和 state去重取正确的线程数
      Set<Integer> set = new HashSet<>();
      for (Object key : keys) {
        String str = String.valueOf(key);
        int i = Integer.parseInt(str.substring(str.length() - 1, str.length()));
        set.add(i);
      }
      int threadNum = set.size();
      if (threadNum == 0) {
        mTaskWrapper.setNewTask(true);
        return;
      }
      mRecord.threadNum = threadNum;
      mTotalThreadNum = threadNum;

      for (int i = 0; i < threadNum; i++) {
        ThreadRecord tRecord = new ThreadRecord();
        tRecord.key = mRecord.filePath;
        Object state = pro.getProperty(mTempFile.getName() + STATE + i);
        Object record = pro.getProperty(mTempFile.getName() + RECORD + i);
        if (state != null && Integer.parseInt(String.valueOf(state)) == 1) {
          mCompleteThreadNum++;
          tRecord.isComplete = true;
          continue;
        }
        if (record != null) {
          long temp = Long.parseLong(String.valueOf(record));
          tRecord.startLocation = temp > 0 ? temp : 0;
        } else {
          tRecord.startLocation = 0;
        }
        mRecord.threadRecords.add(tRecord);
      }
      mConfigFile.delete();
    }
  }

  /**
   * 初始化记录
   */
  private void initRecord(boolean isNewTask) {
    mRecord = new TaskRecord();
    mRecord.fileName = mEntity.getFileName();
    mRecord.filePath = getFilePath();
    mRecord.threadRecords = new ArrayList<>();
    mRecord.isGroupRecord = mTaskWrapper.getEntity().isGroupChild();
    if (mRecord.isGroupRecord) {
      if (mTaskWrapper.getEntity() instanceof DownloadEntity) {
        mRecord.dGroupHash = ((DownloadEntity) mTaskWrapper.getEntity()).getGroupHash();
      }
    }
    mTaskWrapper.setNewTask(isNewTask);
  }

  private String getFilePath() {
    if (getType() == DOWNLOAD) {
      return ((DownloadEntity) mTaskWrapper.getEntity()).getDownloadPath();
    } else {
      return ((UploadEntity) mTaskWrapper.getEntity()).getFilePath();
    }
  }

  /**
   * 保存任务记录
   */
  private void saveRecord() {
    mRecord.threadNum = mRecord.threadRecords.size();
    mRecord.save();
    for (ThreadRecord tr : mRecord.threadRecords) {
      tr.save();
    }
  }

  public TaskRecord getRecord() {
    return mRecord;
  }

  /**
   * 恢复记录地址
   *
   * @return {@code true}任务已完成
   */
  private boolean resumeRecordLocation(int i, long startL, long endL) {
    mConstance.CURRENT_LOCATION += endL - startL;
    ALog.d(TAG, String.format("任务【%s】线程__%s__已完成", mTaskWrapper.getEntity().getFileName(), i));
    mConstance.COMPLETE_THREAD_NUM = mCompleteThreadNum;
    mConstance.STOP_NUM++;
    mConstance.CANCEL_NUM++;
    if (mConstance.isComplete()) {
      mRecord.deleteData();
      mListener.onComplete();
      mConstance.isRunning = false;
      return true;
    }
    return false;
  }

  /**
   * 启动断点任务时，创建单线程任务
   *
   * @param i 线程id
   * @param startL 该任务起始位置
   * @param endL 该任务结束位置
   * @param fileLength 该任务需要处理的文件长度
   */
  private AbsThreadTask createSingThreadTask(int i, long startL, long endL, long fileLength,
      ThreadRecord record) {
    SubThreadConfig<TASK_WRAPPER> config = new SubThreadConfig<>();
    config.TOTAL_FILE_SIZE = fileLength;
    config.URL = mEntity.isRedirect() ? mEntity.getRedirectUrl() : mEntity.getUrl();
    config.TEMP_FILE =
        mRecord.isBlock ? new File(String.format(SUB_PATH, mTempFile.getPath(), i)) : mTempFile;
    config.THREAD_ID = i;
    config.START_LOCATION = startL;
    config.END_LOCATION = endL;
    config.SUPPORT_BP = mTaskWrapper.isSupportBP();
    config.TASK_WRAPPER = mTaskWrapper;
    config.THREAD_RECORD = record;
    return selectThreadTask(config);
  }

  private void handleBreakpoint() {
    long fileLength = mEntity.getFileSize();
    long blockSize = fileLength / mTotalThreadNum;
    Set<Integer> threads = new HashSet<>();

    mRecord.fileLength = fileLength;
    if (mTaskWrapper.isNewTask() && !handleNewTask()) {
      return;
    }
    for (int i = 0; i < mTotalThreadNum; i++) {
      long startL = i * blockSize, endL = (i + 1) * blockSize;
      ThreadRecord tr;
      boolean isNewTr = false;  // 是否是新的线程记录
      if (mTaskWrapper.isNewTask()) {
        tr = new ThreadRecord();
        tr.key = mRecord.filePath;
        tr.threadId = i;
        isNewTr = true;
      } else {
        tr = mRecord.threadRecords.get(i);
      }

      if (tr.blockLen == 0) {
        tr.blockLen = CommonUtil.getBlockLen(fileLength, i, mTotalThreadNum);
      }

      if (tr.isComplete) {//该线程已经完成
        if (resumeRecordLocation(i, startL, endL)) return;
        continue;
      }

      //如果有记录，则恢复任务
      if (tr.startLocation > 0) {
        long r = tr.startLocation;
        //记录的位置需要在线程区间中
        if (startL < r && r <= (i == (mTotalThreadNum - 1) ? fileLength : endL)) {
          mConstance.CURRENT_LOCATION += r - startL;
          startL = r;
        }
        ALog.d(TAG, String.format("任务【%s】线程__%s__恢复任务", mEntity.getFileName(), i));
      } else {
        tr.startLocation = startL;
      }
      //最后一个线程的结束位置即为文件的总长度
      if (i == (mTotalThreadNum - 1)) {
        endL = fileLength;
      }
      if (tr.endLocation <= 0) {
        tr.endLocation = endL;
      }
      if (isNewTr) {
        mRecord.threadRecords.add(tr);
      }
      AbsThreadTask task = createSingThreadTask(i, startL, endL, fileLength, tr);
      if (task == null) return;
      mTask.put(i, task);
      threads.add(i);
    }
    if (mConstance.CURRENT_LOCATION != 0
        && mConstance.CURRENT_LOCATION != mEntity.getCurrentProgress()) {
      ALog.d(TAG, String.format("进度修正，当前进度：%s", mConstance.CURRENT_LOCATION));
      mEntity.setCurrentProgress(mConstance.CURRENT_LOCATION);
    }
    saveRecord();
    startThreadTask(threads);
  }

  /**
   * 启动单线程任务
   */
  private void startThreadTask(Set<Integer> recordL) {
    if (isBreak()) {
      return;
    }
    if (mConstance.CURRENT_LOCATION > 0) {
      mListener.onResume(mConstance.CURRENT_LOCATION);
    } else {
      mListener.onStart(mConstance.CURRENT_LOCATION);
    }
    for (int l : recordL) {
      if (l == -1) continue;
      ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), mTask.get(l));
    }
  }

  /**
   * 重试任务
   */
  public void retryTask() {
    ALog.w(TAG, String.format("任务【%s】开始重试", mEntity.getFileName()));
    startFlow();
  }

  /**
   * 处理新任务
   *
   * @return {@code true}创建新任务失败
   */
  protected abstract boolean handleNewTask();

  /**
   * 处理不支持断点的任务
   */
  private void handleNoSupportBP() {
    if (mListener instanceof BaseDListener) {
      ((BaseDListener) mListener).supportBreakpoint(false);
    }
    SubThreadConfig<TASK_WRAPPER> config = new SubThreadConfig<>();
    config.TOTAL_FILE_SIZE = mEntity.getFileSize();
    config.URL = mEntity.isRedirect() ? mEntity.getRedirectUrl() : mEntity.getUrl();
    config.TEMP_FILE = mTempFile;
    config.THREAD_ID = 0;
    config.START_LOCATION = 0;
    config.END_LOCATION = config.TOTAL_FILE_SIZE;
    config.SUPPORT_BP = mTaskWrapper.isSupportBP();
    config.TASK_WRAPPER = mTaskWrapper;
    ThreadRecord record = DbEntity.findFirst(ThreadRecord.class, "key=?", mRecord.filePath);
    if (record != null) {
      record.deleteData();
    }
    record = new ThreadRecord();
    record.startLocation = 0;
    record.endLocation = config.TOTAL_FILE_SIZE;
    record.key = mTempFile.getPath();
    mRecord.threadRecords.add(record);
    config.THREAD_RECORD = record;
    AbsThreadTask task = selectThreadTask(config);
    if (task == null) return;
    mTask.put(0, task);
    saveRecord();
    ThreadTaskManager.getInstance().startThread(mTaskWrapper.getKey(), task);
    mListener.onStart(0);
  }

  /**
   * 选择单任务线程的类型
   */
  protected abstract AbsThreadTask selectThreadTask(SubThreadConfig<TASK_WRAPPER> config);

  /**
   * 任务是否已经中断
   *
   * @return {@code true}中断
   */
  public boolean isBreak() {
    if (mConstance.isCancel || mConstance.isStop) {
      closeTimer();
      ALog.d(TAG, String.format("任务【%s】已停止或取消了", mEntity.getFileName()));
      return true;
    }
    return false;
  }
}
