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
import android.text.TextUtils;
import com.arialyy.aria.orm.annotation.Default;
import com.arialyy.aria.orm.annotation.Foreign;
import com.arialyy.aria.orm.annotation.Primary;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by laoyuyu on 2018/3/22.
 * 通用委托，创建表，检查字段
 */
class DelegateCommon extends AbsDelegate {
  private DelegateCommon() {
  }

  /**
   * 删除指定的表
   */
  void dropTable(SQLiteDatabase db, String tableName) {
    db = checkDb(db);
    String deleteSQL = String.format("DROP TABLE IF EXISTS %s", tableName);
    print(DROP_TABLE, deleteSQL);
    //db.beginTransaction();
    db.execSQL(deleteSQL);
    //db.setTransactionSuccessful();
    //db.endTransaction();
  }

  /**
   * 清空表数据
   */
  <T extends DbEntity> void clean(SQLiteDatabase db, Class<T> clazz) {
    db = checkDb(db);
    String tableName = CommonUtil.getClassName(clazz);
    if (tableExists(db, clazz)) {
      String sql = "DELETE FROM " + tableName;
      db.execSQL(sql);
    }
  }

  /**
   * 查找表是否存在
   *
   * @param clazz 数据库实体
   * @return true，该数据库实体对应的表存在；false，不存在
   */
  boolean tableExists(SQLiteDatabase db, Class clazz) {
    return tableExists(db, CommonUtil.getClassName(clazz));
  }


  /**
   * 查找表是否存在
   *
   * @param tableName 表名
   * @return true，该数据库实体对应的表存在；false，不存在
   */
  boolean tableExists(SQLiteDatabase db, String tableName) {
    db = checkDb(db);
    Cursor cursor = null;
    try {
      String sql =
          String.format("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='%s'",
              tableName);
      print(TABLE_EXISTS, sql);
      cursor = db.rawQuery(sql, null);
      if (cursor != null && cursor.moveToNext()) {
        int count = cursor.getInt(0);
        if (count > 0) {
          return true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      closeCursor(cursor);
      close(db);
    }
    return false;
  }

  /**
   * 检查某个字段的值是否存在
   *
   * @param expression 字段和值"url=xxx"
   * @return {@code true}该字段的对应的value已存在
   */
  boolean checkDataExist(SQLiteDatabase db, Class clazz, String... expression) {
    db = checkDb(db);
    CheckUtil.checkSqlExpression(expression);
    String sql = String.format("SELECT rowid, * FROM %s WHERE %s ", CommonUtil.getClassName(clazz),
        expression[0]);
    sql = sql.replace("?", "%s");
    Object[] params = new String[expression.length - 1];
    for (int i = 0, len = params.length; i < len; i++) {
      params[i] = String.format("'%s'", encodeStr(expression[i + 1]));
    }
    sql = String.format(sql, params);
    print(FIND_DATA, sql);
    Cursor cursor = db.rawQuery(sql, null);
    final boolean isExist = cursor.getCount() > 0;
    closeCursor(cursor);
    close(db);
    return isExist;
  }

  /**
   * 创建表
   *
   * @param clazz 数据库实体
   */
  void createTable(SQLiteDatabase db, Class clazz) {
    db = checkDb(db);
    List<Field> fields = CommonUtil.getAllFields(clazz);
    if (fields != null && fields.size() > 0) {
      //外键Map，在Sqlite3中foreign修饰的字段必须放在最后
      final List<Field> foreignArray = new ArrayList<>();
      StringBuilder sb = new StringBuilder();
      sb.append("CREATE TABLE IF NOT EXISTS ")
          .append(CommonUtil.getClassName(clazz))
          .append(" (");
      for (Field field : fields) {
        field.setAccessible(true);
        if (SqlUtil.isIgnore(field)) {
          continue;
        }
        Class<?> type = field.getType();
        sb.append(field.getName());
        if (type == String.class || type.isEnum()) {
          sb.append(" VARCHAR");
        } else if (type == int.class || type == Integer.class) {
          sb.append(" INTEGER");
        } else if (type == float.class || type == Float.class) {
          sb.append(" FLOAT");
        } else if (type == double.class || type == Double.class) {
          sb.append(" DOUBLE");
        } else if (type == long.class || type == Long.class) {
          sb.append(" BIGINT");
        } else if (type == boolean.class || type == Boolean.class) {
          sb.append(" BOOLEAN");
        } else if (type == java.util.Date.class || type == java.sql.Date.class) {
          sb.append(" DATA");
        } else if (type == byte.class || type == Byte.class) {
          sb.append(" BLOB");
        } else if (type == Map.class || type == List.class) {
          sb.append(" TEXT");
        } else {
          continue;
        }
        if (SqlUtil.isPrimary(field)) {
          Primary pk = field.getAnnotation(Primary.class);
          sb.append(" PRIMARY KEY");
          if (pk.autoincrement() && (type == int.class || type == Integer.class)) {
            sb.append(" AUTOINCREMENT");
          }
        }

        if (SqlUtil.isForeign(field)) {
          foreignArray.add(field);
        }

        if (SqlUtil.isNoNull(field)) {
          sb.append(" NOT NULL");
        }

        if (SqlUtil.isDefault(field)) {
          Default d = field.getAnnotation(Default.class);
          if (!TextUtils.isEmpty(d.value())) {
            sb.append(" DEFAULT ").append("'").append(d.value()).append("'");
          }
        }

        if (SqlUtil.isUnique(field)) {
          sb.append(" UNIQUE");
        }

        sb.append(",");
      }

      for (Field field : foreignArray) {
        Foreign foreign = field.getAnnotation(Foreign.class);
        sb.append("FOREIGN KEY (")
            .append(field.getName())
            .append(") REFERENCES ")
            .append(CommonUtil.getClassName(foreign.parent()))
            .append("(")
            .append(foreign.column())
            .append(")");
        ActionPolicy update = foreign.onUpdate();
        ActionPolicy delete = foreign.onDelete();
        if (update != ActionPolicy.NO_ACTION) {
          sb.append(" ON UPDATE ").append(update.function);
        }

        if (delete != ActionPolicy.NO_ACTION) {
          sb.append(" ON DELETE ").append(update.function);
        }
        sb.append(",");
      }

      String str = sb.toString();
      str = str.substring(0, str.length() - 1) + ");";
      print(CREATE_TABLE, str);
      db.execSQL(str);
    }
    close(db);
  }
}
