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
 * 组合任务接收器功能接口
 */
public interface IGroupTarget {

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
   * 检查实体是否合法
   *
   * @return {@code true}合法
   */
  boolean checkEntity();

  /**
   * 检查文件夹路径
   * 1、文件夹路径不能为空
   * 2、文件夹路径不能是文件
   *
   * @return {@code true} 合法
   */
  boolean checkDirPath();
}
