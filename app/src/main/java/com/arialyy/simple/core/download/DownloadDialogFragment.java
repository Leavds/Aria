package com.arialyy.simple.core.download;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import com.arialyy.annotations.Download;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.DownloadTask;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.frame.util.show.T;
import com.arialyy.simple.R;
import com.arialyy.simple.base.BaseDialog;
import com.arialyy.simple.databinding.DialogFragmentDownloadBinding;

/**
 * Created by lyy on 2017/8/8.
 */
@SuppressLint("ValidFragment") public class DownloadDialogFragment
    extends BaseDialog<DialogFragmentDownloadBinding> implements View.OnClickListener{

  private static final String DOWNLOAD_URL =
      "http://res3.d.cn/android/new/game/2/78702/fzjh_1499390260312.apk?f=web_1";

  protected DownloadDialogFragment(Object obj) {
    super(obj);
  }

  @Override protected void init(Bundle savedInstanceState) {
    super.init(savedInstanceState);
    Aria.download(getContext()).register();
    DownloadEntity entity = Aria.download(getContext()).getDownloadEntity(DOWNLOAD_URL);
    if (entity != null) {
      getBinding().setFileSize(CommonUtil.formatFileSize(entity.getFileSize()));
      getBinding().setProgress((int) (entity.getCurrentProgress() * 100 / entity.getFileSize()));
    }
    mRootView.findViewById(R.id.start).setOnClickListener(this);
    mRootView.findViewById(R.id.stop).setOnClickListener(this);
    mRootView.findViewById(R.id.cancel).setOnClickListener(this);
  }

  @Override protected int setLayoutId() {
    return R.layout.dialog_fragment_download;
  }

  @Override public void onDestroy() {
    super.onDestroy();
    Aria.download(getContext()).unRegister();
  }

  @Download.onPre(DOWNLOAD_URL) protected void onPre(DownloadTask task) {
  }

  @Download.onTaskStart(DOWNLOAD_URL) void taskStart(DownloadTask task) {
    getBinding().setFileSize(task.getConvertFileSize());
  }

  @Download.onTaskRunning(DOWNLOAD_URL) protected void running(DownloadTask task) {
    long len = task.getFileSize();
    if (len == 0) {
      getBinding().setProgress(0);
    } else {
      getBinding().setProgress(task.getPercent());
    }
    getBinding().setSpeed(task.getConvertSpeed());
  }

  @Download.onTaskResume(DOWNLOAD_URL) void taskResume(DownloadTask task) {
  }

  @Download.onTaskStop(DOWNLOAD_URL) void taskStop(DownloadTask task) {
    getBinding().setSpeed("");
  }

  @Download.onTaskCancel(DOWNLOAD_URL) void taskCancel(DownloadTask task) {
    getBinding().setProgress(0);
    Toast.makeText(getContext(), "取消下载", Toast.LENGTH_SHORT).show();
    getBinding().setSpeed("");
  }

  @Download.onTaskFail(DOWNLOAD_URL) void taskFail(DownloadTask task) {
    Toast.makeText(getContext(), "下载失败", Toast.LENGTH_SHORT).show();
  }

  @Download.onTaskComplete(DOWNLOAD_URL) void taskComplete(DownloadTask task) {
    getBinding().setProgress(100);
    Toast.makeText(getContext(), "下载完成", Toast.LENGTH_SHORT).show();
    getBinding().setSpeed("");
  }

  @Download.onNoSupportBreakPoint(DOWNLOAD_URL)
  public void onNoSupportBreakPoint(DownloadTask task) {
    T.showShort(getContext(), "该下载链接不支持断点");
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.start:
        Aria.download(getContext())
            .load(DOWNLOAD_URL)
            .setFilePath(Environment.getExternalStorageDirectory().getPath() + "/放置江湖.apk")
            .start();
        break;
      case R.id.stop:
        Aria.download(getContext()).load(DOWNLOAD_URL).stop();
        break;
      case R.id.cancel:
        Aria.download(getContext()).load(DOWNLOAD_URL).cancel();
        break;
    }
  }
}
