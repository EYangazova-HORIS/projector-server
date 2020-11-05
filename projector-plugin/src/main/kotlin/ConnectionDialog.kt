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
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.containers.toArray
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.swing.*
import kotlin.random.Random

private class CenterPanelBuilder(private var panelWrapper: JPanel) {
  private val constraints = GridBagConstraints()

  init {
    panelWrapper.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
    panelWrapper.layout = GridBagLayout()
    constraints.fill = GridBagConstraints.HORIZONTAL
    constraints.gridx = 0
    constraints.gridy = 0
  }

  fun addNext(c: Component, width: Int = 1, padY: Int = 0): CenterPanelBuilder {
    constraints.gridwidth = width
    constraints.ipady = padY
    panelWrapper.add(c, constraints)
    constraints.gridx += width
    return this
  }

  fun startNextLine(): CenterPanelBuilder {
    constraints.gridx = 0
    constraints.gridy += 1
    return this
  }
}

class ConnectionDialog(project: Project?) : DialogWrapper(project) {
  private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
  private var panelWrapper: JPanel = JPanel()
  private val host: JComboBox<String> = ComboBox(getHosts())
  private val port: JTextField = JTextField(Utils.getPort())
  private val notUsePwdRW: JCheckBox = JCheckBox("Not use")
  private val notUsePwdRO: JCheckBox = JCheckBox("Not use")
  private val tokenRW: JTextField = JTextField(getSecret())
  private val tokenRO: JTextField = JTextField(getSecret())
  private val copyRW = JButton(AllIcons.Actions.Copy)
  private val copyRO = JButton(AllIcons.Actions.Copy)
  private val urlRW: JLabel = JLabel()
  private val urlRO: JLabel = JLabel()
  private val labelRW: JLabel = JLabel("Full Access URL:")
  private val labelRO: JLabel = JLabel("View Only URL:")

  init {
    title = "Start Remote Access to IDE"
    setResizable(false)
    init()

    if (!ProjectorService.instance.host.isNullOrEmpty()) {
      host.selectedItem = ProjectorService.instance.host
    }

    if (!ProjectorService.instance.port.isNullOrEmpty()) {
      port.text = ProjectorService.instance.port
    }

    updateUrls()

    notUsePwdRW.addActionListener {
      tokenRW.text = if (notUsePwdRW.isSelected) null else getSecret()
      tokenRW.isEnabled = !notUsePwdRW.isSelected
      updateUrls()
    }

    notUsePwdRO.addActionListener {
      tokenRO.text = if (notUsePwdRO.isSelected) null else getSecret()
      tokenRO.isEnabled = !notUsePwdRO.isSelected
      updateUrls()
    }

    val keyListener: KeyListener = object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        updateUrls()
      }
    }
    port.addKeyListener(keyListener)
    tokenRW.addKeyListener(keyListener)
    tokenRO.addKeyListener(keyListener)

    host.addActionListener {
      updateUrls()
    }

    copyRW.addActionListener {
      Utils.copyToClipboard(urlRW.text)
    }

    copyRO.addActionListener {
      Utils.copyToClipboard(urlRO.text)
    }
  }

  override fun createDefaultActions() {
    super.createDefaultActions()
    myOKAction.putValue(Action.NAME, "Start")
  }

  override fun createCenterPanel(): JComponent? {
    CenterPanelBuilder(panelWrapper)
      .addNext(JLabel("<html>Do you want to provide remote access to IDE?<br>Please check your connection parameters:"), 4, 10)
      .startNextLine().addNext(JLabel("Host:")).addNext(host, 2)
      .startNextLine().addNext(JLabel("Port:")).addNext(port, 2)
      .startNextLine().addNext(JLabel("Secret read-write:")).addNext(tokenRW, 2).addNext(notUsePwdRW)
      .startNextLine().addNext(JLabel("Secret read-only:")).addNext(tokenRO, 2).addNext(notUsePwdRO)
      .startNextLine().addNext(labelRW, 1, 10).addNext(urlRW, 2).addNext(copyRW)
      .startNextLine().addNext(labelRO, 1, 10).addNext(urlRO, 2).addNext(copyRO)

    return panelWrapper
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

  private fun getSecret(): String {
    return (1..11)
      .map { Random.nextInt(0, charPool.size) }
      .map(charPool::get)
      .joinToString("")
  }

  private fun getUrl(token: String?): String {
    return Utils.getUrl(host.selectedItem as String, port.text, token)
  }

  private fun updateUrls() {
    urlRW.text = getUrl(getTokenRW())
    urlRO.text = getUrl(getTokenRO())

    val notUsePasswords = (notUsePwdRW.isSelected && notUsePwdRO.isSelected)
    labelRO.isVisible = !notUsePasswords
    urlRO.isVisible = !notUsePasswords
    copyRO.isVisible = !notUsePasswords
  }

  fun getTokenRW(): String? {
    return if (notUsePwdRW.isSelected) null else tokenRW.text
  }

  fun getTokenRO(): String? {
    return if (notUsePwdRO.isSelected) null else tokenRO.text
  }

  fun getHost(): String {
    return host.selectedItem as String
  }

  fun getPort(): String {
    return port.text
  }
}
