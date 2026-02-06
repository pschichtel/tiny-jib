/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.cloud.tools.jib.gradle;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

public class JibPlugin implements Plugin<Project> {

  public static final String JIB_EXTENSION_NAME = "jib";
  public static final String BUILD_IMAGE_TASK_NAME = "jib";
  public static final String BUILD_TAR_TASK_NAME = "jibBuildTar";
  public static final String BUILD_DOCKER_TASK_NAME = "jibDockerBuild";

  @Override
  public void apply(Project project) {
    JibExtension jibExtension =
        project.getExtensions().create(JIB_EXTENSION_NAME, JibExtension.class, project);

    TaskContainer tasks = project.getTasks();
    TaskProvider<BuildImageTask> buildImageTask =
        tasks.register(
            BUILD_IMAGE_TASK_NAME,
            BuildImageTask.class,
            task -> {
              task.setGroup("Jib");
              task.setDescription("Builds a container image to a registry.");
              task.setJibExtension(jibExtension);
            });

    TaskProvider<BuildDockerTask> buildDockerTask =
        tasks.register(
            BUILD_DOCKER_TASK_NAME,
            BuildDockerTask.class,
            task -> {
              task.setGroup("Jib");
              task.setDescription("Builds a container image to a Docker daemon.");
              task.setJibExtension(jibExtension);
            });

    TaskProvider<BuildTarTask> buildTarTask =
        tasks.register(
            BUILD_TAR_TASK_NAME,
            BuildTarTask.class,
            task -> {
              task.setGroup("Jib");
              task.setDescription("Builds a container image to a tarball.");
              task.setJibExtension(jibExtension);
            });

    project.afterEvaluate(
        projectAfterEvaluation -> {
          TaskProvider<Task> warTask = TaskCommon.getWarTaskProvider(projectAfterEvaluation);
          TaskProvider<Task> bootWarTask =
              TaskCommon.getBootWarTaskProvider(projectAfterEvaluation);
          List<Object> jibDependencies = new ArrayList<>();
          if (warTask != null || bootWarTask != null) {
            // Have all tasks depend on the 'war' and/or 'bootWar' task.
            if (warTask != null) {
              jibDependencies.add(warTask);
            }
            if (bootWarTask != null) {
              jibDependencies.add(bootWarTask);
            }
          } else if ("packaged".equals(jibExtension.getContainerizingMode())) {
            // Have all tasks depend on the 'jar' task.
            TaskProvider<Task> jarTask = projectAfterEvaluation.getTasks().named("jar");
            jibDependencies.add(jarTask);

            if (projectAfterEvaluation.getPlugins().hasPlugin("org.springframework.boot")) {
              Task bootJarTask = projectAfterEvaluation.getTasks().getByName("bootJar");

              if (bootJarTask.getEnabled()) {
                String bootJarPath = bootJarTask.getOutputs().getFiles().getAsPath();
                String jarPath = jarTask.get().getOutputs().getFiles().getAsPath();
                if (bootJarPath.equals(jarPath)) {
                  if (!jarTask.get().getEnabled()) {
                    ((Jar) jarTask.get()).getArchiveClassifier().set("original");
                  } else {
                    throw new GradleException(
                        "Both 'bootJar' and 'jar' tasks are enabled, but they write their jar file "
                            + "into the same location at "
                            + jarPath
                            + ". Did you forget to set 'archiveClassifier' on either task?");
                  }
                }
              }
              jarTask.get().setEnabled(true);
            }
          }

          SourceSet mainSourceSet =
              projectAfterEvaluation
                  .getExtensions()
                  .getByType(SourceSetContainer.class)
                  .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
          jibDependencies.add(mainSourceSet.getRuntimeClasspath());
          jibDependencies.add(
              projectAfterEvaluation
                  .getConfigurations()
                  .getByName(jibExtension.getConfigurationName().get()));

          Set<TaskProvider<?>> jibTaskProviders =
              ImmutableSet.of(buildImageTask, buildDockerTask, buildTarTask);
          jibTaskProviders.forEach(
              provider ->
                  provider.configure(task -> jibDependencies.forEach(dep -> task.dependsOn(dep))));
        });
  }
}
