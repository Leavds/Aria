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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTarget;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.scheduler.ISchedulers;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.show.L;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.databinding.ActivitySingleBinding;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleTaskActivity extends BaseActivity<ActivitySingleBinding> {

  private String DOWNLOAD_URL =
      //"http://kotlinlang.org/docs/kotlin-docs.pdf";
      //"https://atom-installer.github.com/v1.13.0/AtomSetup.exe?s=1484074138&ext=.exe";
      //"http://static.gaoshouyou.com/d/22/94/822260b849944492caadd2983f9bb624.apks";
      //"http://120.55.95.61:8811/ghcg/zg/武义总规纲要成果.zip";
      //"https://yizi-kejian.oss-cn-beijing.aliyuncs.com/qimeng/package1/qmtable11.zip";
      "http://rs.0.gaoshouyou.com/d/04/1e/400423a7551e1f3f0eb1812afa1f9b44.apk";
  //"http://58.210.9.131/tpk/sipgt//TDLYZTGH.tpk"; //chunked 下载
  //"https://static.donguo.me/video/ip/course/pfys_1.mp4";
  //"https://www.baidu.com/link?url=_LFCuTPtnzFxVJByJ504QymRywIA1Z_T5xUxe9ZLuxcGM0C_RcdpWyB1eGjbJC-e5wv5wAKM4WmLMAS5KeF6EZJHB8Va3YqZUiaErqK_pxm&wd=&eqid=e8583fe70002d126000000065a99f864";
  //"https://d.pcs.baidu.com/file/a02c89a2d479d4fd2756f3313d42491d?fid=4232431903-250528-1114369760340736&dstime=1525491372&rt=sh&sign=FDtAERVY-DCb740ccc5511e5e8fedcff06b081203-3C13vkOkuk4TqXvVYW05zj1K0ao%3D&expires=8h&chkv=1&chkbd=0&chkpc=et&dp-logid=8651730921842106225&dp-callid=0&r=165533013";
  //"http://apk500.bce.baidu-mgame.com/game/67000/67734/20170622040827_oem_5502845.apk?r=1";
  //"https://dl.genymotion.com/releases/genymotion-2.12.1/genymotion-2.12.1-vbox.exe";
  //"http://9.9.9.50:5000/download1";
  //"http://9.9.9.50:5000/download/CentOS-7-x86_64-Minimal-1804.iso";
  //"http://v2.qingdian1.com/m_20180730_991/2/2B9FB34A4BCD8CE61481D1C8418EFE36_1080P.m3u8";
  //"https://firmwareapi.azurewebsites.net/firmware-overview?name=A19_Filament_W_IMG0038_00102411-encrypted.ota";
  Button mStart;
  Button mStop;
  Button mCancel;
  RadioGroup mRg;

  BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals(ISchedulers.ARIA_TASK_INFO_ACTION)) {
        ALog.d(TAG, "state = " + intent.getIntExtra(ISchedulers.TASK_STATE, -1));
        ALog.d(TAG, "type = " + intent.getIntExtra(ISchedulers.TASK_TYPE, -1));
        ALog.d(TAG, "speed = " + intent.getLongExtra(ISchedulers.TASK_SPEED, -1));
        ALog.d(TAG, "percent = " + intent.getIntExtra(ISchedulers.TASK_PERCENT, -1));
        ALog.d(TAG, "entity = " + intent.getParcelableExtra(ISchedulers.TASK_ENTITY).toString());
      }
    }
  };

  @Override
  protected void onResume() {
    super.onResume();
    //registerReceiver(receiver, new IntentFilter(ISchedulers.ARIA_TASK_INFO_ACTION));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    //unregisterReceiver(receiver);
    Aria.download(this).unRegister();
  }

  @Override
  protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    mStart = findViewById(R.id.start);
    mStop = findViewById(R.id.stop);
    mCancel = findViewById(R.id.cancel);
    mRg = findViewById(R.id.speeds);
    setTitle("单任务下载");
    Aria.download(this).register();
    DownloadTarget target = Aria.download(this).load(DOWNLOAD_URL);
    getBinding().setProgress(target.getPercent());
    if (target.getTaskState() == IEntity.STATE_STOP) {
      mStart.setText("恢复");
      mStart.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
      setBtState(true);
    } else if (target.isRunning()) {
      setBtState(false);
    }
    getBinding().setFileSize(target.getConvertFileSize());
  }

  /**
   * 设置start 和 stop 按钮状态
   */
  private void setBtState(boolean state) {
    //mStart.setEnabled(state);
    //mStop.setEnabled(!state);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_single_task_activity, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    int speed = -1;
    String msg = "";
    switch (item.getItemId()) {
      case R.id.help:
        msg = "一些小知识点：\n"
            + "1、你可以在注解中增加链接，用于指定被注解的方法只能被特定的下载任务回调，以防止progress乱跳\n"
            + "2、当遇到网络慢的情况时，你可以先使用onPre()更新UI界面，待连接成功时，再在onTaskPre()获取完整的task数据，然后给UI界面设置正确的数据\n"
            + "3、你可以在界面初始化时通过Aria.download(this).load(URL).getPercent()等方法快速获取相关任务的一些数据";
        showMsgDialog("tip", msg);
        break;
      case R.id.speed_0:
        speed = 0;
        break;
      case R.id.speed_128:
        speed = 128;
        break;
      case R.id.speed_256:
        speed = 256;
        break;
      case R.id.speed_512:
        speed = 512;
        break;
      case R.id.speed_1m:
        speed = 1024;
        break;
    }
    if (speed > -1) {
      msg = item.getTitle().toString();
      Aria.download(this).setMaxSpeed(speed);
      T.showShort(this, msg);
    }
    return true;
  }

  @Download.onWait
  void onWait(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      Log.d(TAG, "wait ==> " + task.getDownloadEntity().getFileName());
    }
  }

  @Download.onPre
  protected void onPre(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      setBtState(false);
    }
  }

  @Download.onTaskStart
  void taskStart(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      getBinding().setFileSize(task.getConvertFileSize());
    }
  }

  @Download.onTaskRunning
  protected void running(DownloadTask task) {
    ALog.d(TAG, String.format("%s_running_%s", getClass().getName(), hashCode()));
    //if (task.getKey().equals(DOWNLOAD_URL)) {
    //Log.d(TAG, task.getKey());
    long len = task.getFileSize();
    if (len == 0) {
      getBinding().setProgress(0);
    } else {
      getBinding().setProgress(task.getPercent());
    }
    getBinding().setSpeed(task.getConvertSpeed());
    //}
  }

  @Download.onTaskResume
  void taskResume(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      mStart.setText("暂停");
      setBtState(false);
    }
  }

  @Download.onTaskStop
  void taskStop(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      mStart.setText("恢复");
      setBtState(true);
      getBinding().setSpeed("");
    }
  }

  @Download.onTaskCancel
  void taskCancel(DownloadTask task) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      getBinding().setProgress(0);
      Toast.makeText(SingleTaskActivity.this, "取消下载", Toast.LENGTH_SHORT).show();
      mStart.setText("开始");
      setBtState(true);
      getBinding().setSpeed("");
      Log.d(TAG, "cancel");
    }
  }

  /**
   * @param task
   * @param e
   */
  @Download.onTaskFail
  void taskFail(DownloadTask task, Exception e) {
    if (task.getKey().equals(DOWNLOAD_URL)) {
      Toast.makeText(SingleTaskActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
      setBtState(true);
      Aria.download(this)
          .load(DOWNLOAD_URL)
          .updateUrl("http://120.55.95.61:8811/ghcg/zg/武义总规纲要成果.zip")
          .start();
      //DOWNLOAD_URL = "http://120.55.95.61:8811/ghcg/zg/武义总规纲要成果.zip";
      //ALog.d(TAG, ALog.getExceptionString(e));
    }
  }

  @Download.onTaskComplete
  void taskComplete(DownloadTask task) {
    //if (task.getKey().equals(DOWNLOAD_URL)) {
    getBinding().setProgress(100);
    Toast.makeText(SingleTaskActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
    mStart.setText("重新开始？");
    //mCancel.setEnabled(false);
    setBtState(true);
    getBinding().setSpeed("");
    L.d(TAG, "path ==> " + task.getDownloadEntity().getDownloadPath());
    L.d(TAG, "md5Code ==> " + CommonUtil.getFileMD5(new File(task.getDownloadPath())));
    L.d(TAG, "data ==> " + Aria.download(this).getDownloadEntity(DOWNLOAD_URL));
    //Intent install = new Intent(Intent.ACTION_VIEW);
    //install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //File apkFile = new File(task.getDownloadPath());
    //install.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
    //startActivity(install);
    //}
  }

  @Download.onNoSupportBreakPoint
  public void onNoSupportBreakPoint(DownloadTask task) {
    Log.d(TAG, "该下载链接不支持断点");
    if (task.getKey().equals(DOWNLOAD_URL)) {
      T.showShort(SingleTaskActivity.this, "该下载链接不支持断点");
    }
  }

  @Override
  protected int setLayoutId() {
    return R.layout.activity_single;
  }



  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        startD();
        //List<DownloadEntity> list = Aria.download(this).getAllNotCompleteTask(5, 2);
        //ALog.d("Tag", list.size() + "");
        break;
      case R.id.stop:
        Aria.download(this).load(DOWNLOAD_URL).stop();
        //startActivity(new Intent(this, SingleTaskActivity.class));
        //Aria.download(this).unRegister();
        //Aria.download(this).load(DOWNLOAD_URL).removeRecord();
        //Log.d(TAG, Aria.download(this).taskExists(DOWNLOAD_URL) + "");
        break;
      case R.id.cancel:
        Aria.download(this).load(DOWNLOAD_URL).cancel(true);
        //Aria.download(this).load(DOWNLOAD_URL).removeRecord();
        break;
    }
  }

  int i = 1;

  private void startD() {
    String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/ggsg14.apk";
    //Map<String, String> params = new HashMap<>();
    //params.put("key", "value");
    //params.put("filename", "CentOS-7-x86_64-Minimal-1804.iso");
    Aria.download(SingleTaskActivity.this)
        .load(DOWNLOAD_URL)
        //.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        //.addHeader("Accept-Encoding", "gzip, deflate")
        //.addHeader("DNT", "1")
        //.addHeader("Cookie", "BAIDUID=648E5FF020CC69E8DD6F492D1068AAA9:FG=1; BIDUPSID=648E5FF020CC69E8DD6F492D1068AAA9; PSTM=1519099573; BD_UPN=12314753; locale=zh; BDSVRTM=0")
        .useServerFileName(true)
        .setFilePath(path, true)
        //.asGet()
        //.asPost()
        //.setParams(params)
        //.setExtendField("{\n"
        //    + "\"id\":\"你的样子\"\n< > "
        //    + "}")
        //.resetState()
        .start();
    //.add();
  }

  @Override
  protected void onStop() {
    super.onStop();
    //Aria.download(this).unRegister();
  }
}