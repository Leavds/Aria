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
package com.arialyy.simple.modlue;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.show.L;
import java.io.File;

/**
 * Created by laoyuyu on 2018/4/13.
 */

public class AnyRunnModule {
  String TAG = "AnyRunnModule";
  private Context mContext;
  private String mUrl;

  public AnyRunnModule(Context context) {
    Aria.download(this).register();
    mContext = context;
  }

  @Download.onWait void onWait(DownloadTask task) {
    Log.d(TAG, "wait ==> " + task.getDownloadEntity().getFileName());
  }

  @Download.onPre protected void onPre(DownloadTask task) {
    Log.d(TAG, "onPre");
  }

  @Download.onTaskStart void taskStart(DownloadTask task) {
    Log.d(TAG, "onStart");
  }

  @Download.onTaskRunning protected void running(DownloadTask task) {
    Log.d(TAG, "running；Percent = " + task.getPercent());
  }

  @Download.onTaskResume void taskResume(DownloadTask task) {
    Log.d(TAG, "resume");
  }

  @Download.onTaskStop void taskStop(DownloadTask task) {
    Log.d(TAG, "stop");
  }

  @Download.onTaskCancel void taskCancel(DownloadTask task) {
    Log.d(TAG, "cancel");
  }

  @Download.onTaskFail void taskFail(DownloadTask task) {
    Log.d(TAG, "fail");
  }

  @Download.onTaskComplete void taskComplete(DownloadTask task) {
    L.d(TAG, "path ==> " + task.getDownloadEntity().getDownloadPath());
    L.d(TAG, "md5Code ==> " + CommonUtil.getFileMD5(new File(task.getDownloadPath())));
  }

  public void start(String url) {
    mUrl = url;
    String path = Environment.getExternalStorageDirectory().getPath() + "/mmm2.mp4";
    Aria.download(this)
        .load(url)
        .setFilePath(path)
        .resetState()
        .start();
  }

  public void startFtp(String url) {
    mUrl = url;
    Aria.download(this)
        .loadFtp(url)
        .login("lao", "123456")
        .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/Download/")
        .asFtps()
        .setStorePath("/mnt/sdcard/Download/server.crt")
        .setAlias("www.laoyuyu.me")
        .setStorePass("123456")
        .start();
  }

  public void stop(String url) {
    Aria.download(this).load(url).stop();
  }

  public void cancel(String url) {
    Aria.download(this).load(url).cancel();
  }

  public void unRegister() {
    Aria.download(this).unRegister();
  }
}
