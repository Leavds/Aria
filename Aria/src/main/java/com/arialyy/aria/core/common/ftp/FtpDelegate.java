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
package com.arialyy.aria.core.common.ftp;

import android.text.TextUtils;
import com.arialyy.aria.core.FtpUrlEntity;
import com.arialyy.aria.core.inf.AbsTarget;
import com.arialyy.aria.core.inf.IFtpTarget;
import com.arialyy.aria.util.ALog;
import java.net.Proxy;

/**
 * Created by laoyuyu on 2018/3/9.
 * ftp 委托
 */
public class FtpDelegate<TARGET extends AbsTarget> implements IFtpTarget<TARGET> {
  private static final String TAG = "FtpDelegate";
  private TARGET mTarget;

  public FtpDelegate(TARGET target) {
    mTarget = target;
  }

  @Override public TARGET charSet(String charSet) {
    if (TextUtils.isEmpty(charSet)) {
      throw new NullPointerException("字符编码为空");
    }
    mTarget.getTaskWrapper().asFtp().setCharSet(charSet);
    return mTarget;
  }

  @Override public TARGET login(String userName, String password) {
    return login(userName, password, null);
  }

  @Override public TARGET login(String userName, String password, String account) {
    if (TextUtils.isEmpty(userName)) {
      ALog.e(TAG, "用户名不能为null");
      return mTarget;
    } else if (TextUtils.isEmpty(password)) {
      ALog.e(TAG, "密码不能为null");
      return mTarget;
    }
    // urlEntity 不能在构造函数中获取，因为ftp上传时url是后于构造函数的
    FtpUrlEntity urlEntity = mTarget.getTaskWrapper().asFtp().getUrlEntity();
    urlEntity.needLogin = true;
    urlEntity.user = userName;
    urlEntity.password = password;
    urlEntity.account = account;
    return mTarget;
  }

  @Override public TARGET setProxy(Proxy proxy) {
    mTarget.getTaskWrapper().asFtp().setProxy(proxy);
    return mTarget;
  }
}
