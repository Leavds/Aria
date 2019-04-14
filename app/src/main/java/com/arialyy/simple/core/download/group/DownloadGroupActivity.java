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
package com.arialyy.simple.core.download.group;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import com.arialyy.annotations.DownloadGroup;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadGroupEntity;
import com.arialyy.aria.core.download.DownloadGroupTask;
import com.arialyy.aria.core.download.DGTaskWrapper;
import com.arialyy.frame.util.show.L;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.databinding.ActivityDownloadGroupBinding;
import com.arialyy.simple.widget.SubStateLinearLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lyy on 2017/7/6.
 */
public class DownloadGroupActivity extends BaseActivity<ActivityDownloadGroupBinding> {

  SubStateLinearLayout mChildList;
  List<String> mUrls;

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    Aria.download(this).register();
    setTitle("任务组");
    mChildList = getBinding().childList;
    mUrls = getModule(GroupModule.class).getUrls();
    DGTaskWrapper entity = Aria.download(this).getGroupTask(mUrls);
    if (entity != null && entity.getEntity() != null) {
      DownloadGroupEntity groupEntity = entity.getEntity();
      mChildList.addData(groupEntity.getSubEntities());
      getBinding().setFileSize(groupEntity.getConvertFileSize());
      if (groupEntity.getFileSize() == 0) {
        getBinding().setProgress(0);
      } else {
        getBinding().setProgress(groupEntity.isComplete() ? 100
            : (int) (groupEntity.getCurrentProgress() * 100 / groupEntity.getFileSize()));
      }
    }

    mChildList.setOnItemClickListener(new SubStateLinearLayout.OnItemClickListener() {
      @Override public void onItemClick(int position, View view) {
        showPopupWindow(position);
      }
    });
  }

  private void showPopupWindow(int position) {
    ChildHandleDialog dialog =
        new ChildHandleDialog(this, mUrls, mChildList.getSubData().get(position));
    dialog.show(getSupportFragmentManager(), "sub_dialog");
  }

  @Override protected int setLayoutId() {
    return R.layout.activity_download_group;
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        List<String> temp = new ArrayList<>();
        temp.add("https://d.pcs.baidu.com/file/130335545f3f4d9cc38afe709c19af5a?fid=1411168371-250528-1010657263806840&dstime=1531134714&rt=sh&sign=FDtAERVY-DCb740ccc5511e5e8fedcff06b081203-h8KgJ6gl4oY9UR6NqvwJsT4nVSM%3D&expires=8h&chkv=1&chkbd=0&chkpc=et&dp-logid=4401996296756616039&dp-callid=0&r=279987343");
        Aria.download(this)
            .loadGroup(mUrls)
            .setDirPath(
                //Environment.getExternalStorageDirectory().getPath() + "/Download/group_test_5")
                Environment.getExternalStorageDirectory().getPath() + "/Download/group_test_2")
            .setGroupAlias("任务组测试")
            //.setSubFileName(getModule(GroupModule.class).getSubName2())
            .setSubFileName(getModule(GroupModule.class).getSubName())
            .setFileSize(114981416)
            //.updateUrls(temp)
            .start();
        break;
      case R.id.stop:
        Aria.download(this).loadGroup(mUrls).stop();
        break;
      case R.id.cancel:
        Aria.download(this).loadGroup(mUrls).cancel(true);
        //Aria.download(this).removeAllTask(true);
        //mUrls = getModule(GroupModule.class).getUrls1();
        //Aria.download(this)
        //    .load(mUrls)
        //    .setDownloadDirPath(
        //        Environment.getExternalStorageDirectory().getPath() + "/Download/group_test_3")
        //    .setGroupAlias("任务组测试")
        //    .resetState()
        //    .setSubFileName(getModule(GroupModule.class).getSubName())
        //    //.setFileSize(32895492)
        //    .start();
        break;
    }
  }

  @DownloadGroup.onWait void taskWait(DownloadGroupTask task) {
    L.d(TAG, task.getTaskName() + "wait");
  }

  @DownloadGroup.onPre() protected void onPre(DownloadGroupTask task) {
    L.d(TAG, "group pre");
  }

  @DownloadGroup.onTaskPre() protected void onTaskPre(DownloadGroupTask task) {
    if (mChildList.getSubData().size() <= 0) {
      mChildList.addData(task.getEntity().getSubEntities());
    }
    L.d(TAG, "group task pre");
    getBinding().setFileSize(task.getConvertFileSize());
    if (mChildList.getSubData().size() <= 0) {
      mChildList.addData(task.getEntity().getSubEntities());
    }
  }

  @DownloadGroup.onTaskStart() void taskStart(DownloadGroupTask task) {
    getBinding().setFileSize(task.getConvertFileSize());
    L.d(TAG, "group task start");
  }

  @DownloadGroup.onTaskRunning() protected void running(DownloadGroupTask task) {
    Log.d(TAG, "group running, p = "
        + task.getPercent()
        + ", speed = "
        + task.getConvertSpeed()
        + "current_p = "
        + task.getCurrentProgress());
    getBinding().setProgress(task.getPercent());
    getBinding().setSpeed(task.getConvertSpeed());
    //Log.d(TAG, "sub_len = " + task.getEntity().getSubEntities().size());
    mChildList.updateChildProgress(task.getEntity().getSubEntities());
  }

  @DownloadGroup.onTaskResume() void taskResume(DownloadGroupTask task) {
    L.d(TAG, "group task resume");
  }

  @DownloadGroup.onTaskStop() void taskStop(DownloadGroupTask task) {
    L.d(TAG, "group task stop");
    getBinding().setSpeed("");
  }

  @DownloadGroup.onTaskCancel() void taskCancel(DownloadGroupTask task) {
    L.d(TAG, "group task cancel");
    getBinding().setSpeed("");
    getBinding().setProgress(0);
  }

  @DownloadGroup.onTaskFail() void taskFail(DownloadGroupTask task) {
    L.d(TAG, "group task fail");
  }

  @DownloadGroup.onTaskComplete() void taskComplete(DownloadGroupTask task) {
    getBinding().setProgress(100);
    getBinding().setSpeed("");
    mChildList.updateChildProgress(task.getEntity().getSubEntities());
    T.showShort(this, "任务组下载完成");
    L.d(TAG, "任务组下载完成");
  }

  @DownloadGroup.onSubTaskRunning void onSubTaskRunning(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
    //Log.e(TAG, "gHash = "
    //    + groupTask.getEntity().getSubEntities().get(0).hashCode()
    //    + "; subHash = "
    //    + groupTask.getHttpTaskWrapper().getSubTaskEntities().get(0).getEntity().hashCode() +
    //    "; subHash = " + subEntity.hashCode());
    //int percent = subEntity.getPercent();
    ////如果你打开了速度单位转换配置，将可以通过以下方法获取带单位的下载速度，如：1 mb/s
    //String convertSpeed = subEntity.getConvertSpeed();
    ////当前下载完成的进度，长度bytes
    //long completedSize = subEntity.getCurrentProgress();
    //Log.d(TAG, "subTask名字："
    //    + subEntity.getFileName()
    //    + ", "
    //    + " speed:"
    //    + convertSpeed
    //    + ",percent: "
    //    + percent
    //    + "%,  completedSize:"
    //    + completedSize);
  }

  @DownloadGroup.onSubTaskPre void onSubTaskPre(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
  }

  @DownloadGroup.onSubTaskStop void onSubTaskStop(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
  }

  @DownloadGroup.onSubTaskStart void onSubTaskStart(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
  }

  //@DownloadGroup.onSubTaskCancel void onSubTaskCancel(DownloadGroupTask groupTask,
  //    DownloadEntity subEntity) {
  //  Log.d(TAG, "new Size: " + groupTask.getConvertFileSize());
  //  mSub.setText("子任务：" + mChildName + "，状态：取消下载");
  //}

  @DownloadGroup.onSubTaskComplete void onSubTaskComplete(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
  }

  @DownloadGroup.onSubTaskFail void onSubTaskFail(DownloadGroupTask groupTask,
      DownloadEntity subEntity) {
  }
}
