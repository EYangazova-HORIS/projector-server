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

class SessionDialog(project: Project?) : DialogWrapper(project) {
  private val isAlreadyStarted = (ProjectorService.instance.currentSession != null)
  private val message = JLabel(getMessage())
  private val hostPanel: HostPanel
  private val portPanel: PortPanel
  private var tokenRWPanel: TokenPanel
  private var tokenROPanel: TokenPanel
  private var urlRWPanel: UrlPanel
  private var urlROPanel: UrlPanel
  private val labelUrlRO = JLabel(UIUtils.URL_RO_TEXT)

  init {
    hostPanel = HostPanel(ProjectorService.instance.host, ::updateUrls)
    portPanel = PortPanel(ProjectorService.instance.port, ::updateUrls)
    tokenRWPanel = TokenPanel(UIUtils.RW_ACCESS, getInitialToken(ProjectorServer.TOKEN_ENV_NAME), ::updateUrls)
    tokenROPanel = TokenPanel(UIUtils.RO_ACCESS, getInitialToken(ProjectorServer.RO_TOKEN_ENV_NAME), ::updateUrls)
    urlRWPanel = UrlPanel(getUrl(tokenRWPanel.getToken()))
    urlROPanel = UrlPanel(getUrl(tokenROPanel.getToken()))

    hostPanel.hostBox.isEnabled = !isAlreadyStarted
    portPanel.portField.isEnabled = !isAlreadyStarted

    title = if (isAlreadyStarted) "Edit Current Session Parameters" else "Start Remote Access to IDE"
    myOKAction.putValue(Action.NAME, if (isAlreadyStarted) "Save" else "Start")
    setResizable(false)
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val addressPanel = JPanel()
    LinearPanelBuilder(addressPanel)
      .addNextComponent(hostPanel, width = 0.5, rightGap = 35)
      .addNextComponent(portPanel, width = 0.5)

    val panel = JPanel()
    LinearPanelBuilder(panel)
      .addNextComponent(message, gridCount = 2, bottomGap = 5)
      .startNextLine().addNextComponent(addressPanel, 2)
      .startNextLine().addNextComponent(tokenRWPanel, 2)
      .startNextLine().addNextComponent(tokenROPanel, 2)
      .startNextLine().addNextComponent(JLabel(UIUtils.LINKS_TEXT), gridCount = 2, topGap = 5, bottomGap = 5)
      .startNextLine().addNextComponent(JLabel(UIUtils.URL_RW_TEXT)).addNextComponent(urlRWPanel)
      .startNextLine().addNextComponent(labelUrlRO).addNextComponent(urlROPanel)
    return panel
  }

  private fun getMessage(): String {
    return if (isAlreadyStarted) "<html>The current session has already started.<br>Do you want to change passwords?"
    else "<html>Do you want to provide remote access to IDE?<br>Please check your connection parameters:"
  }

  private fun getInitialToken(tokenPropertyName: String): String? {
    return if (isAlreadyStarted) ProjectorService.instance.currentSession!!.getToken(tokenPropertyName) else Utils.getPassword()
  }

  private fun getUrl(token: String?): String {
    return Utils.getUrl(getHost(), getPort(), token)
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

  fun getHost(): String {
    return hostPanel.hostBox.selectedItem as String
  }

  fun getPort(): String {
    return portPanel.portField.text
  }
}
