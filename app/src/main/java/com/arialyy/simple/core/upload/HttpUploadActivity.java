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

package com.arialyy.simple.core.upload;

import android.os.Bundle;
import android.view.View;
import com.arialyy.annotations.Upload;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.upload.UploadTask;
import com.arialyy.frame.util.FileUtil;
import com.arialyy.frame.util.show.L;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.databinding.ActivityUploadBinding;
import com.arialyy.simple.widget.HorizontalProgressBarWithNumber;
import java.io.File;

/**
 * Created by lyy on 2017/2/9.
 */
public class HttpUploadActivity extends BaseActivity<ActivityUploadBinding> {
  private static final String TAG = "HttpUploadActivity";
  HorizontalProgressBarWithNumber mPb;

  private static final String FILE_PATH = "/mnt/sdcard/test.apk";

  @Override protected int setLayoutId() {
    return R.layout.activity_upload;
  }

  @Override protected void init(Bundle savedInstanceState) {
    setTile("D_HTTP 上传");
    super.init(savedInstanceState);
    Aria.upload(this).register();
    getBinding().upload.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        upload();
      }
    });
    getBinding().stop.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        stop();
      }
    });
    getBinding().remove.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        remove();
      }
    });
    mPb = getBinding().pb;
  }

  void upload() {
    Aria.upload(HttpUploadActivity.this).load(FILE_PATH)
        .setUploadUrl(
            "http://lib-test.xzxyun.com:8042/Api/upload?data={\"type\":\"1\",\"fileType\":\".apk\"}")
        //.setTempUrl("http://192.168.1.6:8080/upload/sign_file/").setAttachment("file")
        //.addHeader("iplanetdirectorypro", "11a09102fb934ad0bc206f9c611d7933")
        .asPost()
        .start();
  }

  void stop() {
    Aria.upload(this).load(FILE_PATH).cancel();
  }

  void remove() {
    Aria.upload(this).load(FILE_PATH).cancel();
  }

  @Upload.onPre public void onPre(UploadTask task) {
  }

  @Upload.onTaskStart public void taskStart(UploadTask task) {
    L.d(TAG, "upload start，md5：" + FileUtil.getFileMD5(new File(task.getEntity().getFilePath())));
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Upload.onTaskStop public void taskStop(UploadTask task) {
    L.d(TAG, "upload stop");
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Upload.onTaskCancel public void taskCancel(UploadTask task) {
    L.d(TAG, "upload cancel");
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @Upload.onTaskRunning public void taskRunning(UploadTask task) {
    getBinding().setSpeed(task.getConvertSpeed());
    getBinding().setProgress(task.getPercent());
    L.d(TAG, "P => " + task.getPercent());
  }

  @Upload.onTaskComplete public void taskComplete(UploadTask task) {
    L.d(TAG, "上传完成");
    L.d(TAG, "上传成功服务端返回数据（如果有的话）：" + task.getEntity().getResponseStr());
    getBinding().setSpeed("");
    getBinding().setProgress(100);
  }
}
