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

package com.arialyy.aria.core.common.http;

import com.arialyy.aria.core.common.RequestEnum;
import com.arialyy.aria.core.inf.ITargetHeadDelegate;
import java.net.CookieManager;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Http任务设置的信息，如：cookie、请求参数
 */
public class HttpTaskDelegate implements ITargetHeadDelegate {

  private CookieManager cookieManager;

  /**
   * 请求参数
   */
  private Map<String, String> params;

  /**
   * http 请求头
   */
  private Map<String, String> headers = new HashMap<>();

  /**
   * 字符编码，默认为"utf-8"
   */
  private String charSet = "utf-8";

  /**
   * 网络请求类型
   */
  private RequestEnum requestEnum = RequestEnum.GET;

  /**
   * 是否使用服务器通过content-disposition传递的文件名，内容格式{@code attachment; filename="filename.jpg"} {@code true}
   * 使用
   */
  private boolean useServerFileName = false;

  /**
   * 重定向链接
   */
  private String redirectUrl = "";

  /**
   * 是否是chunk模式
   */
  private boolean isChunked = false;
  /**
   * 文件上传需要的key
   */
  private String attachment;
  /**
   * 上传的文件类型
   */
  private String contentType = "multipart/form-data";
  private String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 6.1; zh-CN; rv:1.9.2.6)";

  private Proxy proxy;
  /**
   * 文件上传表单
   */
  private Map<String, String> formFields = new HashMap<>();

  public Map<String, String> getFormFields() {
    return formFields;
  }

  public void setFormFields(Map<String, String> formFields) {
    this.formFields = formFields;
  }

  public String getAttachment() {
    return attachment;
  }

  public void setAttachment(String attachment) {
    this.attachment = attachment;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public boolean isChunked() {
    return isChunked;
  }

  public void setChunked(boolean chunked) {
    isChunked = chunked;
  }

  public CookieManager getCookieManager() {
    return cookieManager;
  }

  public void setCookieManager(CookieManager cookieManager) {
    this.cookieManager = cookieManager;
  }

  public Proxy getProxy() {
    return proxy;
  }

  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getCharSet() {
    return charSet;
  }

  public void setCharSet(String charSet) {
    this.charSet = charSet;
  }

  public RequestEnum getRequestEnum() {
    return requestEnum;
  }

  public void setRequestEnum(RequestEnum requestEnum) {
    this.requestEnum = requestEnum;
  }

  public boolean isUseServerFileName() {
    return useServerFileName;
  }

  public void setUseServerFileName(boolean useServerFileName) {
    this.useServerFileName = useServerFileName;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }
}
