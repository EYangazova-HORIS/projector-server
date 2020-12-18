/*
 * GNU General Public License version 2
 *
 * Copyright (C) 2019-2020 JetBrains s.r.o.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
import org.jetbrains.intellij.tasks.PatchPluginXmlTask

plugins {
  kotlin("jvm")
  id("org.jetbrains.intellij")
}

dependencies {
  implementation(project(":projector-agent"))
}

intellij {
  version = "2019.3"
  updateSinceUntilBuild = false
}

(tasks["runIde"] as JavaExec).apply {
  jvmArgs = jvmArgs.orEmpty() + listOf("-Djdk.attach.allowAttachSelf=true", "-Dswing.bufferPerWindow=false")
}

tasks.withType<PatchPluginXmlTask> {
  changeNotes(
    """
    Add change notes here.<br>
    <em>most HTML tags may be used</em>
    """
  )
}

val projectorClientVersion: String by project

tasks.withType<ProcessResources> {
  val pluginVersionTag: String = "git describe --match agent-v[0-9]* --abbrev=0 --tags".runCommand(workingDir = rootDir)
  val serverVersionTag: String = "git describe --match v[0-9]* --abbrev=0 --tags".runCommand(workingDir = rootDir)

  val pluginVersion = Pair("pluginVersion", pluginVersionTag.substringAfter("agent-v"))  //"0.43.1"
  val serverVersion = Pair("serverVersion", serverVersionTag.substringAfter("v"))        //"0.49.13"
  val clientVersion = Pair("clientVersion", projectorClientVersion)
  val properties = mapOf(pluginVersion, serverVersion, clientVersion)
  inputs.properties(properties)

  filesMatching("version.properties") {
    expand(properties)
  }
}

fun String.runCommand(
  workingDir: File = File("."),
  timeoutAmount: Long = 60,
  timeoutUnit: TimeUnit = TimeUnit.SECONDS,
): String = ProcessBuilder(
  //split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex())
  split("\\s".toRegex())
)
  .directory(workingDir)
  .redirectOutput(ProcessBuilder.Redirect.PIPE)
  .redirectError(ProcessBuilder.Redirect.PIPE)
  .start()
  .apply { waitFor(timeoutAmount, timeoutUnit) }
  .run {
    val error = errorStream.bufferedReader().readText().trim()
    if (error.isNotEmpty()) {
      println("runCommand error $error")
      throw Exception(error)
    }
    inputStream.bufferedReader().readText().trim()
  }
