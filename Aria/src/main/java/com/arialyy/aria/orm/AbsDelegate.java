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
package com.arialyy.aria.orm;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LruCache;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.net.URLEncoder;

/**
 * Created by laoyuyu on 2018/3/22.
 */
abstract class AbsDelegate {
  static final String TAG = "AbsDelegate";
  static final int CREATE_TABLE = 0;
  static final int TABLE_EXISTS = 1;
  static final int INSERT_DATA = 2;
  static final int MODIFY_DATA = 3;
  static final int FIND_DATA = 4;
  static final int DEL_DATA = 6;
  static final int ROW_ID = 7;
  static final int RELATION = 8;
  static final int DROP_TABLE = 9;

  static LruCache<String, DbEntity> mDataCache = new LruCache<>(1024);

  /**
   * 打印数据库日志
   *
   * @param type {@link DelegateWrapper}
   */
  static void print(int type, String sql) {
    if (ALog.DEBUG) {
      return;
    }
    String str = "";
    switch (type) {
      case CREATE_TABLE:
        str = "创建表 >>>> ";
        break;
      case TABLE_EXISTS:
        str = "表是否存在 >>>> ";
        break;
      case INSERT_DATA:
        str = "插入数据 >>>> ";
        break;
      case MODIFY_DATA:
        str = "修改数据 >>>> ";
        break;
      case FIND_DATA:
        str = "查询数据 >>>> ";
        break;
      case ROW_ID:
        str = "查询RowId >>> ";
        break;
      case RELATION:
        str = "查询关联表 >>> ";
        break;
      case DROP_TABLE:
        str = "删除表 >>> ";
        break;
    }
    ALog.d(TAG, str.concat(sql));
  }

  String getCacheKey(DbEntity dbEntity) {
    return dbEntity.getClass().getName() + "_" + dbEntity.rowID;
  }

  /**
   * URL编码字符串
   *
   * @param str 原始字符串
   * @return 编码后的字符串
   */
  String encodeStr(String str) {
    str = str.replaceAll("\\\\+", "%2B");
    return URLEncoder.encode(str);
  }

  /**
   * 检查list参数是否合法，list只能是{@code List<String>}
   *
   * @return {@code true} 合法
   */
  boolean checkList(Field list) {
    Class t = CommonUtil.getListParamType(list);
    if (t != null && t == String.class) {
      return true;
    } else {
      ALog.d(TAG, "map参数错误，支持List<String>的参数字段");
      return false;
    }
  }

  /**
   * 检查map参数是否合法，map只能是{@code Map<String, String>}
   *
   * @return {@code true} 合法
   */
  boolean checkMap(Field map) {
    Class[] ts = CommonUtil.getMapParamType(map);
    if (ts != null
        && ts[0] != null
        && ts[1] != null
        && ts[0] == String.class
        && ts[1] == String.class) {
      return true;
    } else {
      ALog.d(TAG, "map参数错误，支持Map<String,String>的参数字段");
      return false;
    }
  }

  void closeCursor(Cursor cursor) {
    synchronized (AbsDelegate.class) {
      if (cursor != null && !cursor.isClosed()) {
        try {
          cursor.close();
        } catch (android.database.SQLException e) {
          e.printStackTrace();
        }
      }
    }
  }

  void close(SQLiteDatabase db) {
    //if (db != null && db.isOpen()) db.close();
  }

  /**
   * 检查数据库是否关闭，已经关闭的话，打开数据库
   *
   * @return 返回数据库
   */
  SQLiteDatabase checkDb(SQLiteDatabase db) {
    if (db == null || !db.isOpen()) {
      db = SqlHelper.INSTANCE.getWritableDatabase();
    }
    return db;
  }
}
