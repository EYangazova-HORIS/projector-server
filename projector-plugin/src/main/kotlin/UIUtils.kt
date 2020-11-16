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
import com.intellij.openapi.ui.ComboBox
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.*

class LinearPanelBuilder(private var panelWrapper: JPanel) {
  private val constraints = GridBagConstraints()

  init {
    panelWrapper.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT
    panelWrapper.layout = GridBagLayout()
    constraints.fill = GridBagConstraints.HORIZONTAL
    constraints.gridx = 0
    constraints.gridy = 0
  }

  fun addNextComponent(
    c: Component, gridCount: Int = 1, width: Double = 1.0,
    leftGap: Int = 0, rightGap: Int = 0, topGap: Int = 0, bottomGap: Int = 0,
  ): LinearPanelBuilder {
    constraints.gridwidth = gridCount
    constraints.weightx = width
    constraints.insets = Insets(topGap, leftGap, bottomGap, rightGap)
    panelWrapper.add(c, constraints)
    constraints.gridx += gridCount
    return this
  }

  fun startNextLine(): LinearPanelBuilder {
    constraints.gridx = 0
    constraints.gridy += 1
    return this
  }
}

class HostPanel(host: String?, onChangeAction: () -> Unit) : JPanel() {
  private val hostLabel = JLabel(UIUtils.HOST_TEXT)
  val hostBox: JComboBox<String> = ComboBox(Utils.getHosts())

  init {
    LinearPanelBuilder(this)
      .addNextComponent(hostLabel, width = 0.1)
      .addNextComponent(hostBox)

    if (!host.isNullOrEmpty()) {
      hostBox.selectedItem = host
    }

    hostBox.addActionListener {
      onChangeAction()
    }
  }
}

class PortPanel(port: String?, onChangeAction: () -> Unit) : JPanel() {
  private val portLabel = JLabel(UIUtils.PORT_TEXT)
  val portField: JTextField = JTextField(Utils.getPort())

  init {
    LinearPanelBuilder(this)
      .addNextComponent(portLabel, width = 0.1)
      .addNextComponent(portField)

    if (!port.isNullOrEmpty()) {
      portField.text = port
    }

    val keyListener: KeyListener = object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        onChangeAction()
      }
    }
    portField.addKeyListener(keyListener)
  }
}

class UrlPanel(url: String) : JPanel() {
  private val urlField: JTextField = UIUtils.createSelectableLabel(url)
  private val copyButton = JButton(AllIcons.Actions.Copy)

  init {
    urlField.columns = 30
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    add(urlField)
    add(copyButton)

    copyButton.addActionListener {
      Utils.copyToClipboard(urlField.text)
    }
  }

  fun setUrl(url: String) {
    urlField.text = url
  }
}

class TokenPanel(accessType: String, token: String?, onChangeAction: () -> Unit) : JPanel() {
  private val tokenField: JTextField = JTextField(token)
  private val requiredPwd: JCheckBox = JCheckBox("Require password for $accessType access:")

  init {
    tokenField.columns = 15
    LinearPanelBuilder(this)
      .addNextComponent(requiredPwd)
      .addNextComponent(tokenField)

    val keyListener: KeyListener = object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        onChangeAction()
      }
    }
    tokenField.addKeyListener(keyListener)

    requiredPwd.addActionListener {
      tokenField.text = if (requiredPwd.isSelected) Utils.getPassword() else null
      tokenField.isEnabled = requiredPwd.isSelected
      onChangeAction()
    }
    requiredPwd.isSelected = (token != null)
  }

  fun isRequiredPwd(): Boolean {
    return requiredPwd.isSelected
  }

  fun getToken(): String? {
    return if (requiredPwd.isSelected) tokenField.text else null
  }
}

object UIUtils {
  const val RW_ACCESS = "read-write"
  const val RO_ACCESS = "read-only"
  const val HOST_TEXT = "Host:"
  const val PORT_TEXT = "Port:"
  const val LINKS_TEXT = "Invitation Links:"
  const val URL_RW_TEXT = "Full Access URL:"
  const val URL_RO_TEXT = "View Only URL:"

  fun createSelectableLabel(text: String? = null): JTextField {
    val field = JTextField(text)
    field.isEditable = false
    field.background = null
    field.border = null
    return field
  }

  fun useDifferentTokens(rwPanel: TokenPanel, roPanel: TokenPanel): Boolean {
    return (rwPanel.isRequiredPwd() || roPanel.isRequiredPwd()) && (rwPanel.getToken() != roPanel.getToken())
  }
}