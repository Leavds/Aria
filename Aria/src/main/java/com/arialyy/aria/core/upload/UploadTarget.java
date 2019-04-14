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

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.arialyy.aria.core.common.http.HttpHeaderDelegate;
import com.arialyy.aria.core.common.http.PostDelegate;
import com.arialyy.aria.core.inf.AbsTaskWrapper;
import com.arialyy.aria.core.inf.IHttpHeaderDelegate;
import java.net.Proxy;
import java.util.Map;

/**
 * Created by lyy on 2017/2/28.
 * http 单文件上传
 */
public class UploadTarget extends AbsUploadTarget<UploadTarget>
    implements IHttpHeaderDelegate<UploadTarget> {
  private HttpHeaderDelegate<UploadTarget> mHeaderDelegate;
  private UNormalDelegate<UploadTarget> mNormalDelegate;

  UploadTarget(String filePath, String targetName) {
    mNormalDelegate = new UNormalDelegate<>(this, filePath, targetName);
    initTask();
  }

  private void initTask() {
    //http暂时不支持断点上传
    getTaskWrapper().setSupportBP(false);
    getTaskWrapper().setRequestType(AbsTaskWrapper.U_HTTP);
    mHeaderDelegate = new HttpHeaderDelegate<>(this);
  }

  /**
   * 设置上传路径
   *
   * @param tempUrl 上传路径
   */
  public UploadTarget setUploadUrl(String tempUrl) {
    mNormalDelegate.setTempUrl(tempUrl);
    return this;
  }

  /**
   * Post处理
   */
  public PostDelegate asPost() {
    return new PostDelegate<>(this);
  }

  /**
   * 设置userAgent
   */
  @CheckResult
  public UploadTarget setUserAngent(@NonNull String userAgent) {
    getTaskWrapper().asHttp().setUserAgent(userAgent);
    return this;
  }

  /**
   * 设置服务器需要的附件key
   *
   * @param attachment 附件key
   */
  @CheckResult
  public UploadTarget setAttachment(@NonNull String attachment) {
    getTaskWrapper().asHttp().setAttachment(attachment);
    return this;
  }

  /**
   * 设置上传文件类型
   *
   * @param contentType tip：multipart/form-data
   */
  @CheckResult
  public UploadTarget setContentType(String contentType) {
    getTaskWrapper().asHttp().setContentType(contentType);
    return this;
  }

  @CheckResult
  @Override public UploadTarget addHeader(@NonNull String key, @NonNull String value) {
    return mHeaderDelegate.addHeader(key, value);
  }

  @CheckResult
  @Override public UploadTarget addHeaders(Map<String, String> headers) {
    return mHeaderDelegate.addHeaders(headers);
  }

  @Override public UploadTarget setUrlProxy(Proxy proxy) {
    return mHeaderDelegate.setUrlProxy(proxy);
  }

  @Override protected boolean checkEntity() {
    return mNormalDelegate.checkEntity();
  }

  @Override public boolean isRunning() {
    return mNormalDelegate.isRunning();
  }

  @Override public boolean taskExists() {
    return mNormalDelegate.taskExists();
  }

  @Override public int getTargetType() {
    return HTTP;
  }
}
