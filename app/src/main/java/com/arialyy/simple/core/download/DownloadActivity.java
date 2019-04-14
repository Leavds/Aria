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

package com.arialyy.simple.core.download;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import com.arialyy.simple.MainActivity;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.base.adapter.AbsHolder;
import com.arialyy.simple.base.adapter.AbsRVAdapter;
import com.arialyy.simple.base.adapter.RvItemClickSupport;
import com.arialyy.simple.databinding.ActivityDownloadMeanBinding;
import com.arialyy.simple.core.download.fragment_download.FragmentActivity;
import com.arialyy.simple.core.download.multi_download.MultiTaskActivity;
import com.arialyy.simple.core.download.service_download.DownloadService;
import com.arialyy.simple.modlue.CommonModule;
import com.arialyy.simple.to.NormalTo;
import java.util.List;

/**
 * Created by Lyy on 2016/10/13.
 */
public class DownloadActivity extends BaseActivity<ActivityDownloadMeanBinding> {
  private NormalTo mTo;

  @Override protected int setLayoutId() {
    return R.layout.activity_download_mean;
  }

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    mTo = getIntent().getParcelableExtra(MainActivity.KEY_MAIN_DATA);
    setTitle(mTo.title);
    final List<NormalTo> data = getModule(CommonModule.class).getDownloadData();
    getBinding().list.setLayoutManager(new GridLayoutManager(this, 2));
    getBinding().list.setAdapter(new Adapter(this, data));
    RvItemClickSupport.addTo(getBinding().list).setOnItemClickListener(
        new RvItemClickSupport.OnItemClickListener() {
          @Override public void onItemClicked(RecyclerView recyclerView, int position, View v) {
            CommonModule module = getModule(CommonModule.class);
            switch (position) {
              case 0:
                module.startNextActivity(data.get(position), SingleTaskActivity.class);
                break;
              case 1:
                module.startNextActivity(data.get(position), MultiTaskActivity.class);
                break;
              case 2:
                module.startNextActivity(data.get(position), HighestPriorityActivity.class);
                break;
              case 3:
                break;
              case 4:
                break;
            }
          }
        });
  }

  //public void onClick(View view) {
  //  switch (view.getId()) {
  //    case R.id.highest_priority:
  //      startActivity(new Intent(this, HighestPriorityActivity.class));
  //      break;
  //    case R.id.service:
  //      startService(new Intent(this, DownloadService.class));
  //      break;
  //    case R.id.single_task:
  //      startActivity(new Intent(this, SingleTaskActivity.class));
  //      break;
  //    case R.id.multi_task:
  //      startActivity(new Intent(this, MultiTaskActivity.class));
  //      break;
  //    case R.id.dialog_task:
  //      DownloadDialog dialog = new DownloadDialog(this);
  //
  //      dialog.show();
  //      //DownloadDialogFragment dialog = new DownloadDialogFragment(this);
  //      //dialog.show(getSupportFragmentManager(), "dialog");
  //      break;
  //    case R.id.pop_task:
  //      DownloadPopupWindow pop = new DownloadPopupWindow(this);
  //      pop.showAtLocation(mRootView, Gravity.CENTER_VERTICAL, 0, 0);
  //      break;
  //    case R.id.fragment_task:
  //      startActivity(new Intent(this, FragmentActivity.class));
  //      break;
  //    case R.id.notification:
  //      //SimpleNotification notification = new SimpleNotification(this);
  //      //notification.start();
  //      break;
  //  }
  //}

  private static class Adapter extends AbsRVAdapter<NormalTo, Adapter.Holder> {

    Adapter(Context context, List<NormalTo> data) {
      super(context, data);
    }

    @Override protected Adapter.Holder getViewHolder(View convertView, int viewType) {
      return new Adapter.Holder(convertView);
    }

    @Override protected int setLayoutId(int type) {
      return R.layout.item_download;
    }

    @Override protected void bindData(Adapter.Holder holder, int position, NormalTo item) {
      holder.text.setText(item.title);
      holder.image.setImageResource(item.icon);
    }

    private static class Holder extends AbsHolder {
      TextView text;
      AppCompatImageView image;

      Holder(View itemView) {
        super(itemView);
        text = findViewById(R.id.title);
        image = findViewById(R.id.image);
      }
    }
  }
}