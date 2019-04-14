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

import android.os.Handler;
import com.arialyy.aria.core.common.BaseListener;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.IUploadListener;
import com.arialyy.aria.util.CommonUtil;

/**
 * 下载监听类
 */
class BaseUListener extends BaseListener<UploadEntity, UTaskWrapper, UploadTask>
    implements IUploadListener {

  BaseUListener(UploadTask task, Handler outHandler) {
    super(task, outHandler);
  }

  @Override
  protected void saveData(int state, long location) {
    mTaskWrapper.setState(state);
    mEntity.setState(state);
    if (state == IEntity.STATE_CANCEL) {
      CommonUtil.delTaskRecord(mEntity.getFilePath(), 2, mTaskWrapper.isRemoveFile());
    } else if (state == IEntity.STATE_STOP) {
      mEntity.setStopTime(System.currentTimeMillis());
    } else if (state == IEntity.STATE_COMPLETE) {
      handleComplete();
    } else if (location > 0) {
      mEntity.setCurrentProgress(location);
    }
    mEntity.update();
  }
}