package com.arialyy.simple.core.test;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import com.arialyy.annotations.DownloadGroup;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadGroupTask;
import com.arialyy.frame.util.show.L;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseActivity;
import com.arialyy.simple.databinding.ActivityTestBinding;
import com.arialyy.simple.core.download.group.GroupModule;
import java.util.List;

/**
 * Created by Administrator on 2018/4/12.
 */

public class TestFTPDirActivity extends BaseActivity<ActivityTestBinding> {
  List<String> mUrls;
  private static final String dir = "ftps://192.168.29.140:990/upload/测试";

  @Override protected int setLayoutId() {
    return R.layout.activity_test;
  }

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    mBar.setVisibility(View.GONE);
    Aria.download(this).register();
    mUrls = getModule(GroupModule.class).getUrls();
  }

  @DownloadGroup.onWait void taskWait(DownloadGroupTask task) {
    L.d(TAG, task.getTaskName() + "wait");
  }

  @DownloadGroup.onPre() protected void onPre(DownloadGroupTask task) {
    L.d(TAG, "group pre");
  }

  @DownloadGroup.onTaskPre() protected void onTaskPre(DownloadGroupTask task) {
    L.d(TAG, "group task pre");
  }

  @DownloadGroup.onTaskStart() void taskStart(DownloadGroupTask task) {
    L.d(TAG, "group task start");
  }

  @DownloadGroup.onTaskRunning() protected void running(DownloadGroupTask task) {
    L.d(TAG, "group task running");
  }

  @DownloadGroup.onTaskResume() void taskResume(DownloadGroupTask task) {
    L.d(TAG, "group task resume");
  }

  @DownloadGroup.onTaskStop() void taskStop(DownloadGroupTask task) {
    L.d(TAG, "group task stop");
  }

  @DownloadGroup.onTaskCancel() void taskCancel(DownloadGroupTask task) {
    L.d(TAG, "group task cancel");
  }

  @DownloadGroup.onTaskFail() void taskFail(DownloadGroupTask task) {
    L.d(TAG, "group task fail");
  }

  @DownloadGroup.onTaskComplete() void taskComplete(DownloadGroupTask task) {
    L.d(TAG, "group task complete");
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        //Aria.download(this)
        //    .loadGroup(mUrls)
        //    .setDirPath(Environment.getExternalStorageDirectory().getPath() + "/download/test/")
        //    .resetState()
        //    .start();
        Aria.download(this)
            .loadFtpDir(dir)
            .setDirPath(
                Environment.getExternalStorageDirectory().getPath() + "/Download/ftp_dir")
            .setGroupAlias("ftp文件夹下载")
            //.setSubTaskFileName(getModule(GroupModule.class).getSubName())
            .login("lao", "123456")
            .asFtps()
            .setStorePath("/mnt/sdcard/Download/server.crt")
            .setAlias("www.laoyuyu.me")
            .setStorePass("123456")
            .start();
        break;
      case R.id.stop:
        Aria.download(this).loadFtpDir(dir).stop();
        break;
      case R.id.cancel:
        Aria.download(this).loadFtpDir(dir).cancel();
        break;
    }
  }
}
