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
package com.arialyy.aria.core.inf;

/**
 * Created by lyy on 2019/4/5.
 * 普通任务接收器功能接口
 */
public interface INormalTarget {

  /**
   * 获取实体
   */
  AbsEntity getEntity();

  /**
   * 任务是否存在
   *
   * @return {@code true}任务存在，{@code false} 任务不存在
   */
  boolean taskExists();

  /**
   * 任务是否在执行
   *
   * @return {@code true} 任务正在执行，{@code false} 任务没有执行
   */
  boolean isRunning();

  /**
   * 检查下载实体，判断实体是否合法 合法标准为：
   * 1、下载路径不为null，并且下载路径是正常的http或ftp路径
   * 2、保存路径不为null，并且保存路径是android文件系统路径
   * 3、保存路径不能重复
   *
   * @return {@code true}合法
   */
  boolean checkEntity();

  /**
   * 检查并设置普通任务的文件保存路径
   *
   * @return {@code true}保存路径合法
   */
  boolean checkFilePath();

  /**
   * 检查普通任务的下载地址
   *
   * @return {@code true}地址合法
   */
  boolean checkUrl();
}
