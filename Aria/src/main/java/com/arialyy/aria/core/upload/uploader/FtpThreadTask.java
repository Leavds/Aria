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
package com.arialyy.aria.core.upload.uploader;

import aria.apache.commons.net.ftp.FTPClient;
import aria.apache.commons.net.ftp.FTPReply;
import aria.apache.commons.net.ftp.OnFtpInputStreamListener;
import com.arialyy.aria.core.common.StateConstance;
import com.arialyy.aria.core.common.SubThreadConfig;
import com.arialyy.aria.core.common.ftp.AbsFtpThreadTask;
import com.arialyy.aria.core.config.BaseTaskConfig;
import com.arialyy.aria.core.config.DownloadConfig;
import com.arialyy.aria.core.config.UploadConfig;
import com.arialyy.aria.core.inf.IEventListener;
import com.arialyy.aria.core.upload.UploadEntity;
import com.arialyy.aria.core.upload.UTaskWrapper;
import com.arialyy.aria.exception.AriaIOException;
import com.arialyy.aria.exception.TaskException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.BufferedRandomAccessFile;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Created by Aria.Lao on 2017/7/28. FTP 单线程上传任务，需要FTP 服务器给用户打开append和write的权限
 */
class FtpThreadTask extends AbsFtpThreadTask<UploadEntity, UTaskWrapper> {
  private final String TAG = "FtpThreadTask";
  private String dir, remotePath;

  FtpThreadTask(StateConstance constance, IEventListener listener,
      SubThreadConfig<UTaskWrapper> info) {
    super(constance, listener, info);
  }

  @Override public int getMaxSpeed() {
    return getTaskConfig().getMaxSpeed();
  }

  @Override protected UploadConfig getTaskConfig() {
    return mTaskWrapper.getConfig();
  }

  @Override public FtpThreadTask call() throws Exception {
    super.call();
    //当前子线程的下载位置
    mChildCurrentLocation = mConfig.START_LOCATION;
    FTPClient client = null;
    BufferedRandomAccessFile file = null;
    try {
      ALog.d(TAG,
          String.format("任务【%s】线程__%s__开始上传【开始位置 : %s，结束位置：%s】", mConfig.TEMP_FILE.getName(),
              mConfig.THREAD_ID, mConfig.START_LOCATION, mConfig.END_LOCATION));
      client = createClient();
      if (client == null) {
        return this;
      }
      initPath();
      client.makeDirectory(dir);
      client.changeWorkingDirectory(dir);
      client.setRestartOffset(mConfig.START_LOCATION);
      int reply = client.getReplyCode();
      if (!FTPReply.isPositivePreliminary(reply) && reply != FTPReply.FILE_ACTION_OK) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply,
                    client.getReplyString(), mEntity.getFilePath())));
        client.disconnect();
        return this;
      }

      file = new BufferedRandomAccessFile(mConfig.TEMP_FILE, "rwd", getTaskConfig().getBuffSize());
      if (mConfig.START_LOCATION != 0) {
        //file.skipBytes((int) mConfig.START_LOCATION);
        file.seek(mConfig.START_LOCATION);
      }
      boolean complete = upload(client, file);
      if (!complete || isBreak()) {
        return this;
      }
      ALog.i(TAG,
          String.format("任务【%s】线程__%s__上传完毕", mConfig.TEMP_FILE.getName(), mConfig.THREAD_ID));
      writeConfig(true, mConfig.END_LOCATION);
      STATE.COMPLETE_THREAD_NUM++;
      if (STATE.isComplete()) {
        STATE.TASK_RECORD.deleteData();
        STATE.isRunning = false;
        mListener.onComplete();
      }
      if (STATE.isFail()) {
        STATE.isRunning = false;
        mListener.onFail(false, new TaskException(TAG,
            String.format("上传失败，filePath: %s, uploadUrl: %s", mEntity.getFilePath(), mConfig.URL)));
      }
    } catch (IOException e) {
      fail(mChildCurrentLocation, new AriaIOException(TAG,
          String.format("上传失败，filePath: %s, uploadUrl: %s", mEntity.getFilePath(), mConfig.URL)));
    } catch (Exception e) {
      fail(mChildCurrentLocation, new AriaIOException(TAG, null, e));
    } finally {
      try {
        if (file != null) {
          file.close();
        }
        if (client != null && client.isConnected()) {
          client.disconnect();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return this;
  }

  private void initPath() throws UnsupportedEncodingException {
    dir = new String(mTaskWrapper.asFtp().getUrlEntity().remotePath.getBytes(charSet),
        SERVER_CHARSET);
    remotePath = new String(String.format("%s/%s", mTaskWrapper.asFtp().getUrlEntity().remotePath,
        mEntity.getFileName()).getBytes(charSet), SERVER_CHARSET);
  }

  /**
   * 上传
   *
   * @return {@code true}上传成功、{@code false} 上传失败
   */
  private boolean upload(final FTPClient client, final BufferedRandomAccessFile bis)
      throws IOException {

    try {
      ALog.d(TAG, String.format("remotePath: %s", remotePath));
      client.storeFile(remotePath, new FtpFISAdapter(bis), new OnFtpInputStreamListener() {
        boolean isStoped = false;

        @Override public void onFtpInputStream(FTPClient client, long totalBytesTransferred,
            int bytesTransferred, long streamSize) {
          try {
            if (isBreak() && !isStoped) {
              isStoped = true;
              client.abor();
            }
            if (mSpeedBandUtil != null) {
              mSpeedBandUtil.limitNextBytes(bytesTransferred);
            }
            progress(bytesTransferred);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    } catch (IOException e) {
      String msg = String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", client.getReplyCode(),
          client.getReplyString(), mEntity.getFilePath());
      if (client.isConnected()) {
        client.disconnect();
      }
      if (e.getMessage().contains("AriaIOException caught while copying")) {
        e.printStackTrace();
      } else {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG, msg, e));
      }
      return false;
    }

    int reply = client.getReplyCode();
    if (!FTPReply.isPositiveCompletion(reply)) {
      if (reply != FTPReply.TRANSFER_ABORTED) {
        fail(mChildCurrentLocation,
            new AriaIOException(TAG,
                String.format("文件上传错误，错误码为：%s, msg：%s, filePath: %s", reply,
                    client.getReplyString(), mEntity.getFilePath())));
      }
      if (client.isConnected()) {
        client.disconnect();
      }
      return false;
    }
    return true;
  }
}
