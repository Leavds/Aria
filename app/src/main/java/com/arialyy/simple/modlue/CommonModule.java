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
import android.content.Intent;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.frame.module.AbsModule;
import com.arialyy.simple.MainActivity;
import com.arialyy.simple.R;
import com.arialyy.simple.to.NormalTo;
import com.github.mikephil.charting.data.Entry;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用Modle块
 */
public class CommonModule extends AbsModule {

  private int mPosition;

  public CommonModule(Context context) {
    super(context);
  }

  public void startNextActivity(NormalTo to, Class clazz) {
    Intent intent = new Intent(getContext(), clazz);
    intent.putExtra(MainActivity.KEY_MAIN_DATA, to);
    getContext().startActivity(intent);
  }

  /**
   * 创建图表实体
   */
  public Entry createNewEntry(DownloadEntity entity) {
    long speed = entity.getSpeed() / 1024;
    boolean isKb = true;
    if (speed > 1024) {
      isKb = false;
      speed /= 1024;
    }
    Entry entry = new Entry(mPosition, speed, isKb);
    mPosition++;
    return entry;
  }

  public List<NormalTo> getDownloadData() {
    List<NormalTo> list = new ArrayList<>();
    String[] titles = getContext().getResources().getStringArray(R.array.download_items);
    int[] icons = new int[] {
        R.drawable.ic_http,
        R.drawable.ic_http_group,
        R.drawable.ic_top,
        R.drawable.ic_server,
        R.drawable.ic_windows
    };
    int i = 0;
    for (String title : titles) {
      NormalTo to = new NormalTo();
      to.icon = icons[i];
      to.title = title;
      i++;
      list.add(to);
    }
    return list;
  }

  public List<NormalTo> getMainData() {
    List<NormalTo> list = new ArrayList<>();
    String[] titles = getContext().getResources().getStringArray(R.array.main_items);
    String[] descs = getContext().getResources().getStringArray(R.array.main_items_desc);
    int[] icons = new int[] {
        R.drawable.ic_http,
        R.drawable.ic_http,
        R.drawable.ic_http_group,
        R.drawable.ic_ftp,
        R.drawable.ic_ftp_dir,
        R.drawable.ic_ftp
    };
    int i = 0;
    for (String title : titles) {
      NormalTo to = new NormalTo();
      to.icon = icons[i];
      to.title = title;
      to.desc = descs[i];
      i++;
      list.add(to);
    }
    return list;
  }
}
