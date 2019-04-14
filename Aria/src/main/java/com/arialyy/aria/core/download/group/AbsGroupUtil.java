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
package com.arialyy.aria.core.download.group;

import android.os.Handler;
import android.os.Looper;
import com.arialyy.aria.core.common.IUtil;
import com.arialyy.aria.core.config.Configuration;
import com.arialyy.aria.core.download.DTaskWrapper;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.util.ALog;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by AriaL on 2017/6/30.
 * 任务组核心逻辑
 */
public abstract class AbsGroupUtil implements IUtil, Runnable {
  private final String TAG = "AbsGroupUtil";
  /**
   * FTP文件夹
   */
  int FTP_DIR = 0xa1;
  /**
   * HTTP 任务组
   */
  int HTTP_GROUP = 0xa2;

  private long mCurrentLocation = 0;
  protected IDownloadGroupListener mListener;
  private ScheduledThreadPoolExecutor mTimer;
  private long mUpdateInterval;
  private boolean isStop = false, isCancel = false;
  private Handler mScheduler;
  private SimpleSubQueue mSubQueue = SimpleSubQueue.newInstance();
  private Map<String, SubDownloadLoader> mExeLoader = new WeakHashMap<>();
  private Map<String, DTaskWrapper> mCache = new WeakHashMap<>();
  DGTaskWrapper mGTWrapper;
  GroupRunState mState;

  AbsGroupUtil(IDownloadGroupListener listener, DGTaskWrapper groupWrapper) {
    mListener = listener;
    mGTWrapper = groupWrapper;
    mUpdateInterval = Configuration.getInstance().downloadCfg.getUpdateInterval();
    mState = new GroupRunState(groupWrapper.getKey(), mListener,
        groupWrapper.getSubTaskWrapper().size(), mSubQueue);
    mScheduler = new Handler(Looper.getMainLooper(), SimpleSchedulers.newInstance(mState));
    initState();
  }

  /**
   * 初始化组合任务状态
   */
  private void initState() {
    for (DTaskWrapper wrapper : mGTWrapper.getSubTaskWrapper()) {
      if (wrapper.getEntity().getState() == IEntity.STATE_COMPLETE) {
        mState.updateCompleteNum();
        mCurrentLocation += wrapper.getEntity().getFileSize();
      } else {
        mCache.put(wrapper.getKey(), wrapper);
        mCurrentLocation += wrapper.getEntity().getCurrentProgress();
      }
    }
    mState.updateProgress(mCurrentLocation);
  }

  @Override public String getKey() {
    return mGTWrapper.getKey();
  }

  /**
   * 获取任务类型
   *
   * @return {@link #FTP_DIR}、{@link #HTTP_GROUP}
   */
  abstract int getTaskType();

  /**
   * 启动子任务下载
   *
   * @param url 子任务下载地址
   */
  public void startSubTask(String url) {
    if (!checkSubTask(url, "开始")) return;
    if (!mState.isRunning) {
      startTimer();
    }
    SubDownloadLoader d = getDownloader(url);
    if (d != null && !d.isRunning()) {
      mSubQueue.startTask(d);
    }
  }

  /**
   * 停止子任务下载
   *
   * @param url 子任务下载地址
   */
  public void stopSubTask(String url) {
    if (!checkSubTask(url, "停止")) return;
    SubDownloadLoader d = getDownloader(url);
    if (d != null && d.isRunning()) {
      mSubQueue.stopTask(d);
    }
  }

  /**
   * 检查子任务
   *
   * @param url 子任务url
   * @param type 任务类型
   * @return {@code true} 任务可以下载
   */
  private boolean checkSubTask(String url, String type) {
    DTaskWrapper wrapper = mCache.get(url);
    if (wrapper != null) {
      if (wrapper.getState() == IEntity.STATE_COMPLETE) {
        ALog.w(TAG, "任务【" + url + "】已完成，" + type + "失败");
        return false;
      }
    } else {
      ALog.w(TAG, "任务组中没有该任务【" + url + "】，" + type + "失败");
      return false;
    }
    return true;
  }

  /**
   * 通过地址获取下载器
   *
   * @param url 子任务下载地址
   */
  private SubDownloadLoader getDownloader(String url) {
    SubDownloadLoader d = mExeLoader.get(url);
    if (d == null) {
      return createSubLoader(mCache.get(url));
    }
    return d;
  }

  @Override public long getFileSize() {
    return mGTWrapper.getEntity().getFileSize();
  }

  @Override public long getCurrentLocation() {
    return mCurrentLocation;
  }

  @Override public boolean isRunning() {
    return mState.isRunning;
  }

  @Override public void cancel() {
    isCancel = true;
    closeTimer();
    onCancel();
    Set<String> keys = mExeLoader.keySet();
    mSubQueue.clear();

    for (String key : keys) {
      SubDownloadLoader loader = mExeLoader.get(key);
      if (loader != null && loader.isRunning()) {
        loader.cancel();
      }
    }
    mListener.onCancel();
  }

  public void onCancel() {

  }

  @Override public void stop() {
    isStop = true;
    closeTimer();
    onStop();

    Set<String> keys = mExeLoader.keySet();
    mSubQueue.clear();

    for (String key : keys) {
      SubDownloadLoader loader = mExeLoader.get(key);
      if (loader != null && loader.isRunning()) {
        mSubQueue.stopTask(loader);
      }
    }
    mListener.onStop(mCurrentLocation);
  }

  protected void onStop() {

  }

  @Override public void start() {
    new Thread(this).start();
  }

  @Override public void run() {
    if (isStop || isCancel) {
      closeTimer();
      return;
    }
    onStart();
    startRunningFlow();
  }

  protected void onStart() {

  }

  @Override public void resume() {
    start();
  }

  @Override public void setMaxSpeed(int speed) {
    Set<String> keys = mSubQueue.getExec().keySet();
    for (String key : keys) {
      SubDownloadLoader dt = mSubQueue.getExec().get(key);
      if (dt != null) {
        dt.setMaxSpeed(speed);
      }
    }
  }

  synchronized void closeTimer() {
    if (mTimer != null && !mTimer.isShutdown()) {
      mTimer.shutdown();
    }
  }

  /**
   * 开始进度流程
   */
  private void startRunningFlow() {
    closeTimer();
    mListener.onPostPre(mGTWrapper.getEntity().getFileSize());
    if (mCurrentLocation > 0) {
      mListener.onResume(mCurrentLocation);
    } else {
      mListener.onStart(mCurrentLocation);
    }
    startTimer();
  }

  private synchronized void startTimer() {
    mState.isRunning = true;
    mTimer = new ScheduledThreadPoolExecutor(1);
    mTimer.scheduleWithFixedDelay(new Runnable() {
      @Override public void run() {
        if (!mState.isRunning) {
          closeTimer();
        } else if (mCurrentLocation >= 0) {
          long t = 0;
          for (DTaskWrapper te : mGTWrapper.getSubTaskWrapper()) {
            if (te.getState() == IEntity.STATE_COMPLETE) {
              t += te.getEntity().getFileSize();
            } else {
              t += te.getEntity().getCurrentProgress();
            }
          }
          mCurrentLocation = t;
          mState.updateProgress(mCurrentLocation);
          mListener.onProgress(t);
        }
      }
    }, 0, mUpdateInterval, TimeUnit.MILLISECONDS);
  }

  /**
   * 创建并启动子任务下载器
   */
  SubDownloadLoader createSubLoader(DTaskWrapper taskWrapper) {
    SubDownloadLoader loader = new SubDownloadLoader(mScheduler, taskWrapper);
    mExeLoader.put(loader.getKey(), loader);
    mSubQueue.startTask(loader);
    return loader;
  }
}
