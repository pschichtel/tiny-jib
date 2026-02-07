/*
 * Copyright 2019 Google LLC.
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

import java.nio.file.Path;
import java.nio.file.Paths;
import javax.inject.Inject;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

/** Object that configures where Jib should create its build output files. */
public class OutputPathsParameters {

  private final Project project;

  private Path digest;
  private Path tar;
  private Path imageId;
  private Path imageJson;

  @Inject
  public OutputPathsParameters(Project project) {
    this.project = project;
    digest = project.getLayout().getBuildDirectory().get().getAsFile().toPath().resolve("jib-image.digest");
    imageId = project.getLayout().getBuildDirectory().get().getAsFile().toPath().resolve("jib-image.id");
    imageJson = project.getLayout().getBuildDirectory().get().getAsFile().toPath().resolve("jib-image.json");
    tar = project.getLayout().getBuildDirectory().get().getAsFile().toPath().resolve("jib-image.tar");
  }

  @Input
  public String getDigest() {
    return getRelativeToProjectRoot(digest).toString();
  }

  @Internal
  Path getDigestPath() {
    return getRelativeToProjectRoot(digest);
  }

  public void setDigest(String digest) {
    this.digest = Paths.get(digest);
  }

  @Input
  public String getImageId() {
    return getRelativeToProjectRoot(imageId).toString();
  }

  @Internal
  Path getImageIdPath() {
    return getRelativeToProjectRoot(imageId);
  }

  public void setImageId(String id) {
    this.imageId = Paths.get(id);
  }

  @Input
  public String getImageJson() {
    return getRelativeToProjectRoot(imageJson).toString();
  }

  @Internal
  Path getImageJsonPath() {
    return getRelativeToProjectRoot(imageJson);
  }

  public void setImageJson(String imageJson) {
    this.imageJson = Paths.get(imageJson);
  }

  @Input
  public String getTar() {
    return getRelativeToProjectRoot(tar).toString();
  }

  @Internal
  Path getTarPath() {
    return getRelativeToProjectRoot(tar);
  }

  public void setTar(String tar) {
    this.tar = Paths.get(tar);
  }

  private Path getRelativeToProjectRoot(Path configuration) {
      return configuration.isAbsolute() ? configuration : project.getProjectDir().toPath().resolve(configuration);
  }
}
