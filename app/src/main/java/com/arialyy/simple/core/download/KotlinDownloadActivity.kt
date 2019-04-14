//package com.arialyy.simple.core.download
//
//import android.os.Bundle
//import android.os.Environment
//import android.support.v7.app.AppCompatActivity
//import android.util.Log
//import android.view.View
//import com.arialyy.annotations.Download
//import com.arialyy.aria.core.Aria
//import com.arialyy.aria.core.download.DownloadTask
//import com.arialyy.simple.R
//import com.arialyy.simple.base.BaseActivity
//
///**
// * Created by lyy on 2017/10/23.
// */
//class KotlinDownloadActivity : AppCompatActivity() {
//
//  private val DOWNLOAD_URL = "http://static.gaoshouyou.com/d/22/94/822260b849944492caadd2983f9bb624.apk"
//
//  private val TAG = "KotlinDownloadActivity";
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setContentView(setLayoutId())
//  }
//
//  fun setLayoutId(): Int {
//    return R.layout.activity_single
//  }
//
//  fun init(savedInstanceState: Bundle?) {
//    title = "单任务下载"
////    val target = Aria.download(this).load(DOWNLOAD_URL)
////    binding.progress = target.getPercent()
////    if (target.getTaskState() == IEntity.STATE_STOP) {
////      mStart.setText("恢复")
////      mStart.setTextColor(resources.getColor(android.R.color.holo_blue_light))
////      setBtState(true)
////    } else if (target.isDownloading()) {
////      setBtState(false)
////    }
////    binding.fileSize = target.getConvertFileSize()
//    Aria.get(this).downloadConfig.maxTaskNum = 2
//    Aria.download(this).register()
//  }
//
//  @Download.onTaskRunning
//  protected fun running(task: DownloadTask) {
//    Log.d(TAG, task.percent.toString() + "")
////    val len = task.fileSize
////    if (len == 0L) {
////      binding.progress = 0
////    } else {
////      binding.progress = task.percent
////    }
////    binding.speed = task.convertSpeed
//  }
//
//  fun onClick(view: View) {
//    when (view.id) {
//      R.id.start -> startD()
//      R.id.stop -> Aria.download(this).load(DOWNLOAD_URL).stop()
//      R.id.cancel -> Aria.download(this).load(DOWNLOAD_URL).cancel()
//    }
//  }
//
//  private fun startD() {
//    Aria.download(this)
//        .load(DOWNLOAD_URL)
//        .addHeader("groupHash", "value")
//        .setDownloadPath(Environment.getExternalStorageDirectory().path + "/hhhhhhhh.apk")
//        .start()
//  }
//}