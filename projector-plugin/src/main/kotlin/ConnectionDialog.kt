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
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.containers.toArray
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.swing.*

class ConnectionDialog(project: Project?) : DialogWrapper(project) {
  private val host: JComboBox<String> = ComboBox(getHosts())
  private val port: JTextField = JTextField(Utils.getPort())
  private var tokenRWPanel: TokenPanel
  private var tokenROPanel: TokenPanel
  private var urlRWPanel: UrlPanel
  private var urlROPanel: UrlPanel
  private val labelUrlRO = JLabel(UIUtils.URL_RO_TEXT)

  init {
    if (!ProjectorService.instance.host.isNullOrEmpty()) {
      host.selectedItem = ProjectorService.instance.host
    }

    if (!ProjectorService.instance.port.isNullOrEmpty()) {
      port.text = ProjectorService.instance.port
    }

    host.addActionListener {
      updateUrls()
    }

    val keyListener: KeyListener = object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        updateUrls()
      }
    }
    port.addKeyListener(keyListener)

    tokenRWPanel = TokenPanel(Utils.getPassword(), ::updateUrls)
    tokenROPanel = TokenPanel(Utils.getPassword(), ::updateUrls)
    urlRWPanel = UrlPanel(getUrl(tokenRWPanel.getToken()))
    urlROPanel = UrlPanel(getUrl(tokenROPanel.getToken()))

    title = "Start Remote Access to IDE"
    setResizable(false)
    init()
  }

  override fun createDefaultActions() {
    super.createDefaultActions()
    myOKAction.putValue(Action.NAME, "Start")
  }

  override fun createCenterPanel(): JComponent? {
    val panel = JPanel()
    LinearPanelBuilder(panel)
      .addNextComponent(JLabel("<html>Do you want to provide remote access to IDE?<br>Please check your connection parameters:"), 2, 10)
      .startNextLine().addNextComponent(JLabel("Host:")).addNextComponent(host)
      .startNextLine().addNextComponent(JLabel("Port:")).addNextComponent(port)
      .startNextLine().addNextComponent(JLabel(UIUtils.PWD_RW_TEXT)).addNextComponent(tokenRWPanel)
      .startNextLine().addNextComponent(JLabel(UIUtils.PWD_RO_TEXT)).addNextComponent(tokenROPanel)
      .startNextLine().addNextComponent(JLabel(UIUtils.URL_RW_TEXT)).addNextComponent(urlRWPanel)
      .startNextLine().addNextComponent(labelUrlRO).addNextComponent(urlROPanel)
    return panel
  }

  private fun getHosts(): Array<String> {
    val dockerVendor = byteArrayOf(0x02.toByte(), 0x42.toByte())
    val arr = emptyArray<String>()

    return NetworkInterface.getNetworkInterfaces()
      .asSequence()
      .filterNotNull()
      .filterNot { it.isLoopback }
      .filterNot {
        it.hardwareAddress != null
        &&
        it.hardwareAddress.sliceArray(0..1).contentEquals(dockerVendor)
      }
      .flatMap { it.interfaceAddresses?.asSequence()?.filterNotNull() ?: emptySequence() }
      .mapNotNull { (it.address as? Inet4Address)?.hostName }
      .toList()
      .toArray(arr)
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
    return host.selectedItem as String
  }

  fun getPort(): String {
    return port.text
  }
}
