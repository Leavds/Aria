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
package com.arialyy.compiler;

import com.arialyy.annotations.Download;
import com.arialyy.annotations.DownloadGroup;
import com.arialyy.annotations.Upload;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

/**
 * Created by lyy on 2017/9/6. 任务事件代理文件
 *
 * <pre>
 *   <code>
 * package com.arialyy.simple.core.download;
 *
 * import com.arialyy.aria.core.download.DownloadTask;
 * import com.arialyy.aria.core.scheduler.AbsSchedulerListener;
 *
 * public final class SingleTaskActivity$$DownloadListenerProxy extends
 * AbsSchedulerListener<DownloadTask> {
 * private SingleTaskActivity obj;
 *
 *    public void onPre(final DownloadTask task) {
 *      obj.onPre((DownloadTask)task);
 *    }
 *
 *    public void onTaskStart(final DownloadTask task) {
 *      obj.onStart((DownloadTask)task);
 *    }
 *
 *    public void setListener(final Object obj) {
 *      this.obj = (SingleTaskActivity)obj;
 *    }
 * }
 *   </code>
 * </pre>
 */
final class EventProxyFiler {

  private Filer mFiler;
  private ParamObtainUtil mPbUtil;

  EventProxyFiler(Filer filer, ParamObtainUtil pbUtil) {
    mFiler = filer;
    mPbUtil = pbUtil;
  }

  /**
   * 创建任务事件代理文件
   */
  void createEventProxyFile() throws IOException {
    Set<String> keys = mPbUtil.getMethodParams().keySet();
    for (String key : keys) {
      ProxyClassParam entity = mPbUtil.getMethodParams().get(key);
      JavaFile jf = JavaFile.builder(entity.packageName, createProxyClass(entity)).build();
      createFile(jf);
    }
  }

  /**
   * 创建代理方法
   *
   * @param taskEnum 任务类型枚举{@link TaskEnum}
   * @param annotation {@link Download}、{@link Upload}
   * @param methodInfo 被代理类注解的方法信息
   */
  private MethodSpec createProxyMethod(TaskEnum taskEnum, Class<? extends Annotation> annotation,
      MethodInfo methodInfo) {
    ClassName task = ClassName.get(taskEnum.getPkg(), taskEnum.getClassName());

    ParameterSpec taskParam =
        ParameterSpec.builder(task, "task").addModifiers(Modifier.FINAL).build();

    String callCode;

    if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB) {
      if (methodInfo.params.get(methodInfo.params.size() - 1)
          .asType()
          .toString()
          .equals(Exception.class.getName())
          && annotation == DownloadGroup.onSubTaskFail.class) {
        callCode = "task, subEntity, e";
      } else {
        callCode = "task, subEntity";
      }
    } else {
      if (methodInfo.params.get(methodInfo.params.size() - 1)
          .asType()
          .toString()
          .equals(Exception.class.getName())
          && (annotation == Download.onTaskFail.class
          || annotation == Upload.onTaskFail.class
          || annotation == DownloadGroup.onTaskFail.class)) {
        callCode = "task, e";
      } else {
        callCode = "task";
      }
    }
    StringBuilder sb = new StringBuilder();
    sb.append("obj.")
        .append(methodInfo.methodName)
        .append("((")
        .append(taskEnum.getClassName())
        .append(")")
        .append(callCode)
        .append(");\n");

    MethodSpec.Builder builder = MethodSpec.methodBuilder(annotation.getSimpleName())
        .addModifiers(Modifier.PUBLIC)
        .returns(void.class)
        .addParameter(taskParam)
        .addAnnotation(Override.class)
        .addCode(sb.toString());

    //任务组接口
    if (taskEnum == TaskEnum.DOWNLOAD_GROUP_SUB) {
      ClassName subTask =
          ClassName.get(TaskEnum.DOWNLOAD_ENTITY.pkg, TaskEnum.DOWNLOAD_ENTITY.className);
      ParameterSpec subTaskParam =
          ParameterSpec.builder(subTask, "subEntity").addModifiers(Modifier.FINAL).build();

      builder.addParameter(subTaskParam);
    }

    if (annotation == Download.onTaskFail.class
        || annotation == Upload.onTaskFail.class
        || annotation == DownloadGroup.onTaskFail.class
        || annotation == DownloadGroup.onSubTaskFail.class) {
      ParameterSpec exception = ParameterSpec.builder(Exception.class, "e").build();
      builder.addParameter(exception);
    }

    return builder.build();
  }

  /**
   * 创建代理类
   */
  private TypeSpec createProxyClass(ProxyClassParam entity) {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder(entity.proxyClassName).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    //添加被代理的类的字段
    ClassName obj = ClassName.get(entity.packageName, entity.className);
    FieldSpec observerField = FieldSpec.builder(obj, "obj").addModifiers(Modifier.PRIVATE).build();
    builder.addField(observerField);

    //添加注解方法
    for (TaskEnum te : entity.methods.keySet()) {
      Map<Class<? extends Annotation>, MethodInfo> methodInfoMap = entity.methods.get(te);
      //if (entity.proxyClassName.contains(TaskEnum.DOWNLOAD.proxySuffix)) {
      //  type.add(1);
      //} else if (entity.proxyClassName.contains(TaskEnum.DOWNLOAD_GROUP.proxySuffix)) {
      //  type.add(2);
      //} else if (entity.proxyClassName.contains(TaskEnum.UPLOAD.proxySuffix)) {
      //  type.add(3);
      //} else if (entity.proxyClassName.contains(TaskEnum.UPLOAD_GROUP.proxySuffix)) {
      //  type.add(4);
      //}
      if (methodInfoMap != null) {
        for (Class<? extends Annotation> annotation : methodInfoMap.keySet()) {
          MethodSpec method = createProxyMethod(te, annotation, methodInfoMap.get(annotation));
          builder.addMethod(method);
        }
      }
    }

    //注册当前类
    //for (Integer i : type) {
    //  String str = null;
    //  switch (i) {
    //    case 1:
    //    case 2:
    //      str = "$T.download(obj).register();\n";
    //      break;
    //    case 3:
    //    case 4:
    //      str = "$T.upload(obj).register();\n";
    //      break;
    //  }
    //  if (str != null) {
    //    cb.add(str, ClassName.get("com.arialyy.aria.core", "Aria"));
    //  }
    //}

    MethodSpec structure = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build();
    builder.addMethod(structure);

    //添加设置代理的类
    ParameterSpec parameterSpec =
        ParameterSpec.builder(Object.class, "obj").addModifiers(Modifier.FINAL).build();
    MethodSpec listener = MethodSpec.methodBuilder(ProxyConstance.SET_LISTENER)
        .addModifiers(Modifier.PUBLIC)
        .returns(void.class)
        .addParameter(parameterSpec)
        .addAnnotation(Override.class)
        .addCode("this.obj = (" + entity.className + ")obj;\n")
        .build();
    builder.addJavadoc("该文件为Aria自动生成的代理文件，请不要修改该文件的任何代码！\n");

    //创建父类参数
    ClassName superClass = ClassName.get("com.arialyy.aria.core.scheduler", "AbsSchedulerListener");
    //主任务泛型参数
    ClassName taskTypeVariable =
        ClassName.get(entity.mainTaskEnum.pkg, entity.mainTaskEnum.className);
    //子任务泛型参数
    ClassName subTaskTypeVariable =
        ClassName.get(entity.subTaskEnum.pkg, entity.subTaskEnum.className);

    builder.superclass(
        ParameterizedTypeName.get(superClass, taskTypeVariable, subTaskTypeVariable));
    builder.addMethod(listener);
    return builder.build();
  }

  private void createFile(JavaFile jf) throws IOException {
    if (ProxyConstance.DEBUG) {
      // 如果需要在控制台打印生成的文件，则去掉下面的注释
      jf.writeTo(System.out);
    } else {
      jf.writeTo(mFiler);
    }
  }
}
