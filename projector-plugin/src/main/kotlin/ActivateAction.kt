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
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ActivateAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    ProjectorService.instance.activate()
  }

  override fun update(e: AnActionEvent) {
    val state = ProjectorService.instance.enabled == EnabledState.NO_VM_OPTIONS_AND_DISABLED
    e.presentation.isEnabled = state
    e.presentation.isVisible = state
  }
}
