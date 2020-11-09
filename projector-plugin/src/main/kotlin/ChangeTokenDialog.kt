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

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.projector.server.ProjectorServer
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class ChangeTokenDialog(project: Project?) : DialogWrapper(project) {
  private var session: Session
  private var tokenRWPanel: TokenPanel
  private var tokenROPanel: TokenPanel
  private var urlRWPanel: UrlPanel
  private var urlROPanel: UrlPanel
  private val labelUrlRO = JLabel(UIUtils.URL_RO_TEXT)

  init {
    require(ProjectorService.instance.currentSession != null) {
      "Projector session is not started"
    }

    session = ProjectorService.instance.currentSession!!
    tokenRWPanel = TokenPanel(session.getToken(ProjectorServer.TOKEN_ENV_NAME), ::updateUrls)
    tokenROPanel = TokenPanel(session.getToken(ProjectorServer.RO_TOKEN_ENV_NAME), ::updateUrls)
    urlRWPanel = UrlPanel(session.getUrl(ProjectorServer.TOKEN_ENV_NAME))
    urlROPanel = UrlPanel(session.getUrl(ProjectorServer.RO_TOKEN_ENV_NAME))

    title = "Change Secrets"
    setResizable(false)
    init()
  }

  override fun createDefaultActions() {
    super.createDefaultActions()
    myOKAction.putValue(Action.NAME, "Save")
  }

  override fun createCenterPanel(): JComponent? {
    val panel = JPanel()
    LinearPanelBuilder(panel)
      .addNextComponent(JLabel("Do you want to change passwords?"), 2, 10)
      .startNextLine().addNextComponent(JLabel(UIUtils.SECRET_RW_TEXT)).addNextComponent(tokenRWPanel)
      .startNextLine().addNextComponent(JLabel(UIUtils.SECRET_RO_TEXT)).addNextComponent(tokenROPanel)
      .startNextLine().addNextComponent(JLabel(UIUtils.URL_RW_TEXT)).addNextComponent(urlRWPanel)
      .startNextLine().addNextComponent(labelUrlRO).addNextComponent(urlROPanel)
    return panel
  }

  private fun getUrl(token: String?): String {
    return Utils.getUrl(session.host, session.port, token)
  }

  private fun updateUrls() {
    urlRWPanel.setUrl(getUrl(tokenRWPanel.getToken()))
    urlROPanel.setUrl(getUrl(tokenROPanel.getToken()))

    val useDifferentTokens = UIUtils.useDifferentTokens(tokenRWPanel, tokenROPanel)
    labelUrlRO.isVisible = useDifferentTokens
    urlROPanel.isVisible = useDifferentTokens
  }

  fun getTokenRW(): String? {
    return tokenRWPanel.getToken()
  }

  fun getTokenRO(): String? {
    return tokenROPanel.getToken()
  }
}