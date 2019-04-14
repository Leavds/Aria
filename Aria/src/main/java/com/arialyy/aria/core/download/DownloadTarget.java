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
package com.arialyy.aria.core.download;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import com.arialyy.aria.core.common.http.GetDelegate;
import com.arialyy.aria.core.common.http.HttpHeaderDelegate;
import com.arialyy.aria.core.common.http.PostDelegate;
import com.arialyy.aria.core.inf.IHttpHeaderDelegate;
import java.net.Proxy;
import java.util.Map;

/**
 * Created by lyy on 2016/12/5.
 * https://github.com/AriaLyy/Aria
 */
public class DownloadTarget extends AbsDTarget<DownloadTarget>
    implements IHttpHeaderDelegate<DownloadTarget> {
  private HttpHeaderDelegate<DownloadTarget> mHeaderDelegate;
  private DNormalDelegate<DownloadTarget> mNormalDelegate;

  DownloadTarget(DownloadEntity entity, String targetName) {
    this(entity.getUrl(), targetName);
  }

  DownloadTarget(String url, String targetName) {
    mNormalDelegate = new DNormalDelegate<>(this, url, targetName);
    mHeaderDelegate = new HttpHeaderDelegate<>(this);
  }

  /**
   * Post处理
   */
  @CheckResult
  public PostDelegate asPost() {
    return new PostDelegate<>(this);
  }

  /**
   * get参数传递
   */
  @CheckResult
  public GetDelegate asGet() {
    return new GetDelegate<>(this);
  }

  /**
   * 是否使用服务器通过content-disposition传递的文件名，内容格式{@code attachment;filename=***}
   * 如果获取不到服务器文件名，则使用用户设置的文件名
   *
   * @param use {@code true} 使用
   */
  @CheckResult
  public DownloadTarget useServerFileName(boolean use) {
    getTaskWrapper().asHttp().setUseServerFileName(use);
    return this;
  }

  /**
   * 设置文件存储路径，如果需要修改新的文件名，修改路径便可。
   * 如：原文件路径 /mnt/sdcard/test.zip
   * 如果需要将test.zip改为game.zip，只需要重新设置文件路径为：/mnt/sdcard/game.zip
   *
   * @param filePath 路径必须为文件路径，不能为文件夹路径
   */
  @CheckResult
  public DownloadTarget setFilePath(@NonNull String filePath) {
    mNormalDelegate.setTempFilePath(filePath);
    return this;
  }

  /**
   * 设置文件存储路径，如果需要修改新的文件名，修改路径便可。
   * 如：原文件路径 /mnt/sdcard/test.zip
   * 如果需要将test.zip改为game.zip，只需要重新设置文件路径为：/mnt/sdcard/game.zip
   *
   * @param filePath 路径必须为文件路径，不能为文件夹路径
   * @param forceDownload {@code true}强制下载，不考虑文件路径是否被占用
   */
  @CheckResult
  public DownloadTarget setFilePath(@NonNull String filePath, boolean forceDownload) {
    mNormalDelegate.setTempFilePath(filePath);
    mNormalDelegate.setForceDownload(forceDownload);
    return this;
  }

  /**
   * 从header中获取文件描述信息
   */
  public String getContentDisposition() {
    return getEntity().getDisposition();
  }

  @Override public DownloadTarget updateUrl(String newUrl) {
    return mNormalDelegate.updateUrl(newUrl);
  }

  @Override public int getTargetType() {
    return HTTP;
  }

  /**
   * 设置URL的代理
   *
   * @param proxy {@link Proxy}
   */
  @CheckResult
  @Override public DownloadTarget setUrlProxy(Proxy proxy) {
    return mHeaderDelegate.setUrlProxy(proxy);
  }

  @CheckResult
  @Override public DownloadTarget addHeader(@NonNull String key, @NonNull String value) {
    return mHeaderDelegate.addHeader(key, value);
  }

  @CheckResult
  @Override public DownloadTarget addHeaders(Map<String, String> headers) {
    return mHeaderDelegate.addHeaders(headers);
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
}
