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

import android.annotation.TargetApi;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.TextUtils;
import com.arialyy.aria.orm.annotation.Many;
import com.arialyy.aria.orm.annotation.One;
import com.arialyy.aria.orm.annotation.Wrapper;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by laoyuyu on 2018/3/22.
 * 查询数据
 */
class DelegateFind extends AbsDelegate {
  private final String PARENT_COLUMN_ALIAS = "p";
  private final String CHILD_COLUMN_ALIAS = "c";

  private DelegateFind() {
  }

  /**
   * 获取{@link One}和{@link Many}注解的字段
   *
   * @return 返回[OneField, ManyField] ，如果注解依赖错误返回null
   */
  private Field[] getOneAndManyField(Class clazz) {
    Field[] om = new Field[2];
    Field[] fields = clazz.getDeclaredFields();
    Field one = null, many = null;
    boolean hasOne = false, hasMany = false;
    for (Field field : fields) {
      if (SqlUtil.isOne(field)) {
        if (hasOne) {
          ALog.w(TAG, "查询数据失败，实体中有多个@One 注解");
          return null;
        }
        hasOne = true;
        one = field;
      }
      if (SqlUtil.isMany(field)) {
        if (hasMany) {
          ALog.w(TAG, "查询数据失败，实体中有多个@Many 注解");
          return null;
        }
        if (!field.getType().isAssignableFrom(List.class)) {
          ALog.w(TAG, "查询数据失败，@Many 注解的类型不是List");
          return null;
        }
        hasMany = true;
        many = field;
      }
    }

    if (one == null || many == null) {
      ALog.w(TAG, "查询数据失败，实体中没有@One或@Many注解");
      return null;
    }

    if (many.getType() != List.class) {
      ALog.w(TAG, "查询数据失败，@Many注解的字段必须是List");
      return null;
    }
    om[0] = one;
    om[1] = many;
    return om;
  }

  /**
   * 查找一对多的关联数据
   * 如果查找不到数据或实体没有被{@link Wrapper}注解，将返回null
   * 如果实体中没有{@link One}或{@link Many}注解，将返回null
   * 如果实体中有多个{@link One}或{@link Many}注解，将返回nul
   * {@link One} 的注解对象必须是{@link DbEntity}，{@link Many}的注解对象必须是List，并且List中的类型必须是{@link DbEntity}
   */
  <T extends AbsDbWrapper> List<T> findRelationData(SQLiteDatabase db, Class<T> clazz,
      String... expression) {
    return exeRelationSql(db, clazz, -1, -1, expression);
  }

  /**
   * 查找一对多的关联数据
   * 如果查找不到数据或实体没有被{@link Wrapper}注解，将返回null
   * 如果实体中没有{@link One}或{@link Many}注解，将返回null
   * 如果实体中有多个{@link One}或{@link Many}注解，将返回nul
   * {@link One} 的注解对象必须是{@link DbEntity}，{@link Many}的注解对象必须是List，并且List中的类型必须是{@link DbEntity}
   */
  <T extends AbsDbWrapper> List<T> findRelationData(SQLiteDatabase db, Class<T> clazz,
      int page, int num, String... expression) {
    if (page < 1 || num < 1) {
      return null;
    }
    return exeRelationSql(db, clazz, page, num, expression);
  }

  /**
   * 执行关联查询，如果不需要分页，page和num传-1
   *
   * @param page 当前页
   * @param num 一页的数量
   */
  private <T extends AbsDbWrapper> List<T> exeRelationSql(SQLiteDatabase db, Class<T> clazz,
      int page, int num, String... expression) {
    db = checkDb(db);
    if (SqlUtil.isWrapper(clazz)) {
      Field[] om = getOneAndManyField(clazz);
      if (om == null) {
        return null;
      }
      StringBuilder sb = new StringBuilder();
      Field one = om[0], many = om[1];
      try {
        Many m = many.getAnnotation(Many.class);
        Class parentClazz = Class.forName(one.getType().getName());
        Class childClazz = Class.forName(CommonUtil.getListParamType(many).getName());
        final String pTableName = parentClazz.getSimpleName();
        final String cTableName = childClazz.getSimpleName();
        List<Field> pColumn = SqlUtil.getAllNotIgnoreField(parentClazz);
        List<Field> cColumn = SqlUtil.getAllNotIgnoreField(childClazz);
        List<String> pColumnAlias = new ArrayList<>();
        List<String> cColumnAlias = new ArrayList<>();
        StringBuilder pSb = new StringBuilder();
        StringBuilder cSb = new StringBuilder();

        if (pColumn != null) {
          pSb.append(pTableName.concat(".rowid AS ").concat(PARENT_COLUMN_ALIAS).concat("rowid,"));
          for (Field f : pColumn) {
            String temp = PARENT_COLUMN_ALIAS.concat(f.getName());
            pColumnAlias.add(temp);
            pSb.append(pTableName.concat(".").concat(f.getName()))
                .append(" AS ")
                .append(temp)
                .append(",");
          }
        }

        if (cColumn != null) {
          pSb.append(cTableName.concat(".rowid AS ").concat(CHILD_COLUMN_ALIAS).concat("rowid,"));
          for (Field f : cColumn) {
            String temp = CHILD_COLUMN_ALIAS.concat(f.getName());
            cColumnAlias.add(temp);
            cSb.append(cTableName.concat(".").concat(f.getName()))
                .append(" AS ")
                .append(temp)
                .append(",");
          }
        }

        String pColumnAlia = pSb.toString();
        String cColumnAlia = cSb.toString();
        if (!TextUtils.isEmpty(pColumnAlia)) {
          pColumnAlia = pColumnAlia.substring(0, pColumnAlia.length() - 1);
        }

        if (!TextUtils.isEmpty(cColumnAlia)) {
          cColumnAlia = cColumnAlia.substring(0, cColumnAlia.length() - 1);
        }

        sb.append("SELECT ");

        if (!TextUtils.isEmpty(pColumnAlia)) {
          sb.append(pColumnAlia).append(",");
        }
        if (!TextUtils.isEmpty(cColumnAlia)) {
          sb.append(cColumnAlia);
        }
        if (TextUtils.isEmpty(pColumnAlia) && TextUtils.isEmpty(cColumnAlia)) {
          sb.append(" * ");
        }

        sb.append(" FROM ")
            .append(pTableName)
            .append(" INNER JOIN ")
            .append(cTableName)
            .append(" ON ")
            .append(pTableName.concat(".").concat(m.parentColumn()))
            .append(" = ")
            .append(cTableName.concat(".").concat(m.entityColumn()));
        String sql;
        if (expression != null && expression.length > 0) {
          CheckUtil.checkSqlExpression(expression);
          sb.append(" WHERE ").append(expression[0]).append(" ");
          sql = sb.toString();
          sql = sql.replace("?", "%s");
          Object[] params = new String[expression.length - 1];
          for (int i = 0, len = params.length; i < len; i++) {
            params[i] = String.format("'%s'", encodeStr(expression[i + 1]));
          }
          sql = String.format(sql, params);
        } else {
          sql = sb.toString();
        }
        if (page != -1 && num != -1) {
          sql = sql.concat(String.format(" LIMIT %s,%s", (page - 1) * num, num));
        }

        print(RELATION, sql);
        Cursor cursor = db.rawQuery(sql, null);
        List<T> data =
            (List<T>) newInstanceEntity(clazz, parentClazz, childClazz, cursor, pColumn, cColumn,
                pColumnAlias, cColumnAlias);
        closeCursor(cursor);
        close(db);
        return data;
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    } else {
      ALog.e(TAG, "查询数据失败，实体类没有使用@Wrapper 注解");
      return null;
    }
    return null;
  }

  /**
   * 创建关联查询的数据
   *
   * @param pColumn 父表的所有字段
   * @param cColumn 字表的所有字段
   * @param pColumnAlias 关联查询父表别名
   * @param cColumnAlias 关联查询子表别名
   */
  private synchronized <T extends AbsDbWrapper, P extends DbEntity, C extends DbEntity> List<T> newInstanceEntity(
      Class<T> clazz, Class<P> parent,
      Class<C> child,
      Cursor cursor,
      List<Field> pColumn, List<Field> cColumn,
      List<String> pColumnAlias, List<String> cColumnAlias) {
    try {
      String parentPrimary = ""; //父表主键别名
      for (Field f : pColumn) {
        if (SqlUtil.isPrimary(f)) {
          parentPrimary = PARENT_COLUMN_ALIAS.concat(f.getName());
          break;
        }
      }

      List<T> wrappers = new ArrayList<>();
      Map<Object, P> tempParent = new LinkedHashMap<>();  // 所有父表元素，key为父表主键的值
      Map<Object, List<C>> tempChild = new LinkedHashMap<>(); // 所有的字表元素，key为父表主键的值

      Object old = null;
      while (cursor.moveToNext()) {
        //创建父实体
        Object ppValue = setPPValue(parentPrimary, cursor);
        if (old == null || ppValue != old) {  //当主键不同时，表示是不同的父表数据
          old = ppValue;
          if (tempParent.get(old) == null) {
            P pEntity = parent.newInstance();
            String pPrimaryName = "";
            for (int i = 0, len = pColumnAlias.size(); i < len; i++) {
              Field pField = pColumn.get(i);
              pField.setAccessible(true);
              Class<?> type = pField.getType();
              int column = cursor.getColumnIndex(pColumnAlias.get(i));
              if (column == -1) continue;
              setFieldValue(type, pField, column, cursor, pEntity);

              if (SqlUtil.isPrimary(pField) && (type == int.class || type == Integer.class)) {
                pPrimaryName = pField.getName();
              }
            }

            //当设置了主键，而且主键的类型为integer时，查询RowID等于主键
            pEntity.rowID = cursor.getInt(
                cursor.getColumnIndex(
                    TextUtils.isEmpty(pPrimaryName) ? PARENT_COLUMN_ALIAS.concat("rowid")
                        : pPrimaryName));

            tempParent.put(ppValue, pEntity);
          }
        }

        // 创建子实体
        C cEntity = child.newInstance();
        String cPrimaryName = "";
        for (int i = 0, len = cColumnAlias.size(); i < len; i++) {
          Field cField = cColumn.get(i);
          cField.setAccessible(true);
          Class<?> type = cField.getType();

          int column = cursor.getColumnIndex(cColumnAlias.get(i));
          if (column == -1) continue;
          setFieldValue(type, cField, column, cursor, cEntity);

          if (SqlUtil.isPrimary(cField) && (type == int.class || type == Integer.class)) {
            cPrimaryName = cField.getName();
          }
        }
        //当设置了主键，而且主键的类型为integer时，查询RowID等于主键
        cEntity.rowID = cursor.getInt(
            cursor.getColumnIndex(
                TextUtils.isEmpty(cPrimaryName) ? CHILD_COLUMN_ALIAS.concat("rowid")
                    : cPrimaryName));
        if (tempChild.get(old) == null) {
          tempChild.put(old, new ArrayList<C>());
        }
        tempChild.get(old).add(cEntity);
      }

      List<Field> wFields = SqlUtil.getAllNotIgnoreField(clazz);
      if (wFields != null && !wFields.isEmpty()) {
        Set<Object> pKeys = tempParent.keySet();
        for (Object pk : pKeys) {
          T wrapper = clazz.newInstance();
          P p = tempParent.get(pk);
          boolean isPSet = false, isCSet = false;
          for (Field f : wFields) {
            if (!isPSet && f.getAnnotation(One.class) != null) {
              f.set(wrapper, p);
              isPSet = true;
            }
            if (!isCSet && f.getAnnotation(Many.class) != null) {
              f.set(wrapper, tempChild.get(pk));
              isCSet = true;
            }
          }
          wrapper.handleConvert();  //处理下转换
          wrappers.add(wrapper);
        }
      }
      return wrappers;
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * 获取父表主键数据
   *
   * @param parentPrimary 父表主键别名
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB) private Object setPPValue(String parentPrimary,
      Cursor cursor) {
    Object ppValue = null;
    int ppColumn = cursor.getColumnIndex(parentPrimary);  //父表主键所在的列
    int type = cursor.getType(ppColumn);
    switch (type) {
      case Cursor.FIELD_TYPE_INTEGER:
        ppValue = cursor.getLong(ppColumn);
        break;
      case Cursor.FIELD_TYPE_FLOAT:
        ppValue = cursor.getFloat(ppColumn);
        break;
      case Cursor.FIELD_TYPE_STRING:
        ppValue = cursor.getString(ppColumn);
        break;
    }
    return ppValue;
  }

  /**
   * 条件查寻数据
   */
  <T extends DbEntity> List<T> findData(SQLiteDatabase db, Class<T> clazz, String... expression) {
    db = checkDb(db);
    CheckUtil.checkSqlExpression(expression);
    String sql = String.format("SELECT rowid, * FROM %s WHERE %s", CommonUtil.getClassName(clazz),
        expression[0]);
    String[] params = new String[expression.length - 1];
    System.arraycopy(expression, 1, params, 0, params.length);

    return exeNormalDataSql(db, clazz, sql, params);
  }

  /**
   * 获取分页数据
   */
  <T extends DbEntity> List<T> findData(SQLiteDatabase db, Class<T> clazz, int page, int num,
      String... expression) {
    if (page < 1 || num < 1) {
      return null;
    }
    db = checkDb(db);
    CheckUtil.checkSqlExpression(expression);
    String sql = String.format("SELECT rowid, * FROM %s WHERE %s LIMIT %s,%s",
        CommonUtil.getClassName(clazz),
        expression[0], (page - 1) * num, num);

    String[] params = new String[expression.length - 1];
    System.arraycopy(expression, 1, params, 0, params.length);

    return exeNormalDataSql(db, clazz, sql, params);
  }

  /**
   * 模糊查寻数据
   */
  <T extends DbEntity> List<T> findDataByFuzzy(SQLiteDatabase db, Class<T> clazz,
      String conditions) {
    db = checkDb(db);
    if (TextUtils.isEmpty(conditions)) {
      throw new IllegalArgumentException("sql语句表达式不能为null或\"\"");
    }
    if (!conditions.toUpperCase().contains("LIKE")) {
      throw new IllegalArgumentException("sql语句表达式未包含LIEK");
    }
    String sql = String.format("SELECT rowid, * FROM %s, WHERE %s", CommonUtil.getClassName(clazz),
        conditions);
    return exeNormalDataSql(db, clazz, sql, null);
  }

  /**
   * 分页、模糊搜索数据
   */
  <T extends DbEntity> List<T> findDataByFuzzy(SQLiteDatabase db, Class<T> clazz,
      int page, int num, String conditions) {
    if (page < 1 || num < 1) {
      return null;
    }
    db = checkDb(db);
    if (TextUtils.isEmpty(conditions)) {
      throw new IllegalArgumentException("sql语句表达式不能为null或\"\"");
    }
    if (!conditions.toUpperCase().contains("LIKE")) {
      throw new IllegalArgumentException("sql语句表达式未包含LIEK");
    }
    String sql = String.format("SELECT rowid, * FROM %s WHERE %s LIMIT %s,%s",
        CommonUtil.getClassName(clazz), conditions, (page - 1) * num, num);
    return exeNormalDataSql(db, clazz, sql, null);
  }

  /**
   * 查找表的所有数据
   */
  <T extends DbEntity> List<T> findAllData(SQLiteDatabase db, Class<T> clazz) {
    db = checkDb(db);
    String sql = String.format("SELECT rowid, * FROM %s", CommonUtil.getClassName(clazz));
    return exeNormalDataSql(db, clazz, sql, null);
  }

  /**
   * 执行查询普通数据的sql语句，并创建对象
   *
   * @param sql sql 查询语句
   * @param selectionArgs 查询参数，如何sql语句中查询条件含有'？'则该参数不能为空
   */
  private <T extends DbEntity> List<T> exeNormalDataSql(SQLiteDatabase db, Class<T> clazz,
      String sql, String[] selectionArgs) {
    print(FIND_DATA, sql);
    String[] temp = new String[selectionArgs.length];
    int i = 0;
    for (String arg : selectionArgs){
      temp[i] = encodeStr(arg);
      i ++;
    }
    Cursor cursor = db.rawQuery(sql, temp);
    List<T> data = cursor.getCount() > 0 ? newInstanceEntity(clazz, cursor) : null;
    closeCursor(cursor);
    close(db);
    return data;
  }

  /**
   * 根据数据游标创建一个具体的对象
   */
  private synchronized <T extends DbEntity> List<T> newInstanceEntity(Class<T> clazz,
      Cursor cursor) {
    List<Field> fields = CommonUtil.getAllFields(clazz);
    List<T> entitys = new ArrayList<>();
    if (fields != null && fields.size() > 0) {
      try {
        while (cursor.moveToNext()) {
          T entity = clazz.newInstance();
          String primaryName = "";
          for (Field field : fields) {
            field.setAccessible(true);
            if (SqlUtil.isIgnore(field)) {
              continue;
            }

            Class<?> type = field.getType();
            if (SqlUtil.isPrimary(field) && (type == int.class || type == Integer.class)) {
              primaryName = field.getName();
            }

            int column = cursor.getColumnIndex(field.getName());
            if (column == -1) continue;
            setFieldValue(type, field, column, cursor, entity);
          }
          //当设置了主键，而且主键的类型为integer时，查询RowID等于主键
          entity.rowID = cursor.getInt(
              cursor.getColumnIndex(TextUtils.isEmpty(primaryName) ? "rowid" : primaryName));
          //mDataCache.put(getCacheKey(entity), entity);
          entitys.add(entity);
        }
        closeCursor(cursor);
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return entitys;
  }

  /**
   * 设置字段的值
   *
   * @throws IllegalAccessException
   */
  private void setFieldValue(Class type, Field field, int column, Cursor cursor, Object entity)
      throws IllegalAccessException {
    if (cursor == null || cursor.isClosed()) {
      ALog.e(TAG, "cursor没有初始化");
      return;
    }
    if (type == String.class) {
      String temp = cursor.getString(column);
      if (!TextUtils.isEmpty(temp)) {
        field.set(entity, URLDecoder.decode(temp));
      }
    } else if (type == int.class || type == Integer.class) {
      field.setInt(entity, cursor.getInt(column));
    } else if (type == float.class || type == Float.class) {
      field.setFloat(entity, cursor.getFloat(column));
    } else if (type == double.class || type == Double.class) {
      field.setDouble(entity, cursor.getDouble(column));
    } else if (type == long.class || type == Long.class) {
      field.setLong(entity, cursor.getLong(column));
    } else if (type == boolean.class || type == Boolean.class) {
      String temp = cursor.getString(column);
      if (TextUtils.isEmpty(temp)) {
        field.setBoolean(entity, false);
      } else {
        field.setBoolean(entity, !temp.equalsIgnoreCase("false"));
      }
    } else if (type == java.util.Date.class || type == java.sql.Date.class) {
      field.set(entity, new Date(URLDecoder.decode(cursor.getString(column))));
    } else if (type == byte[].class) {
      field.set(entity, cursor.getBlob(column));
    } else if (type == Map.class) {
      String temp = cursor.getString(column);
      if (!TextUtils.isEmpty(temp)) {
        field.set(entity, SqlUtil.str2Map(URLDecoder.decode(temp)));
      }
    } else if (type == List.class) {
      String value = cursor.getString(column);
      if (!TextUtils.isEmpty(value)) {
        field.set(entity, SqlUtil.str2List(URLDecoder.decode(value), field));
      }
    }
  }

  /**
   * 获取所在行Id
   */
  int[] getRowId(SQLiteDatabase db, Class clazz) {
    db = checkDb(db);
    Cursor cursor = db.rawQuery("SELECT rowid, * FROM " + CommonUtil.getClassName(clazz), null);
    int[] ids = new int[cursor.getCount()];
    int i = 0;
    while (cursor.moveToNext()) {
      ids[i] = cursor.getInt(cursor.getColumnIndex("rowid"));
      i++;
    }
    cursor.close();
    close(db);
    return ids;
  }

  /**
   * 获取行Id
   */
  int getRowId(SQLiteDatabase db, Class clazz, Object[] wheres, Object[] values) {
    db = checkDb(db);
    if (wheres.length <= 0 || values.length <= 0) {
      ALog.e(TAG, "请输入删除条件");
      return -1;
    } else if (wheres.length != values.length) {
      ALog.e(TAG, "groupHash 和 vaule 长度不相等");
      return -1;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT rowid FROM ").append(CommonUtil.getClassName(clazz)).append(" WHERE ");
    int i = 0;
    for (Object where : wheres) {
      sb.append(where).append("=").append("'").append(values[i]).append("'");
      sb.append(i >= wheres.length - 1 ? "" : ",");
      i++;
    }
    print(ROW_ID, sb.toString());
    Cursor c = db.rawQuery(sb.toString(), null);
    int id = c.getColumnIndex("rowid");
    c.close();
    close(db);
    return id;
  }

  /**
   * 通过rowId判断数据是否存在
   */
  <T extends DbEntity> boolean itemExist(SQLiteDatabase db, Class<T> clazz, long rowId) {
    return itemExist(db, CommonUtil.getClassName(clazz), rowId);
  }

  /**
   * 通过rowId判断数据是否存在
   */
  boolean itemExist(SQLiteDatabase db, String tableName, long rowId) {
    db = checkDb(db);
    String sql = "SELECT rowid FROM " + tableName + " WHERE rowid=" + rowId;
    print(ROW_ID, sql);
    Cursor cursor = db.rawQuery(sql, null);
    boolean isExist = cursor.getCount() > 0;
    cursor.close();
    return isExist;
  }
}
