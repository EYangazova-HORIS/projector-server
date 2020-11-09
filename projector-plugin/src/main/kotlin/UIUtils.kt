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
import java.awt.Component
import java.awt.ComponentOrientation
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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

  fun addNextComponent(c: Component, width: Int = 1, padY: Int = 0): LinearPanelBuilder {
    constraints.gridwidth = width
    constraints.ipady = padY
    panelWrapper.add(c, constraints)
    constraints.gridx += width
    return this
  }

  fun startNextLine(): LinearPanelBuilder {
    constraints.gridx = 0
    constraints.gridy += 1
    return this
  }
}

class UrlPanel(url: String) : JPanel() {
  private val urlField: JTextField = UIUtils.createSelectableLabel(url)
  private val copyButton = JButton(AllIcons.Actions.Copy)

  init {
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    urlField.columns = 35
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

class TokenPanel(token: String?, onChangeAction: () -> Unit) : JPanel() {
  private val tokenField: JTextField = JTextField(token)
  private val notUseFlag: JCheckBox = JCheckBox("Not use")

  init {
    layout = BoxLayout(this, BoxLayout.LINE_AXIS)
    add(tokenField)
    add(notUseFlag)

    val keyListener: KeyListener = object : KeyAdapter() {
      override fun keyReleased(e: KeyEvent) {
        onChangeAction()
      }
    }
    tokenField.addKeyListener(keyListener)

    notUseFlag.addActionListener {
      tokenField.text = if (notUseFlag.isSelected) null else Utils.getPassword()
      tokenField.isEnabled = !notUseFlag.isSelected
      onChangeAction()
    }
    notUseFlag.isSelected = (token == null)
  }

  fun getNotUseFlag(): Boolean {
    return notUseFlag.isSelected
  }

  fun getToken(): String? {
    return if (notUseFlag.isSelected) null else tokenField.text
  }
}

object UIUtils {
  const val PWD_RW_TEXT = "Password read-write:"
  const val PWD_RO_TEXT = "Password read-only:"
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
    val notUsePasswords = (rwPanel.getNotUseFlag() && roPanel.getNotUseFlag())
    return !notUsePasswords && (rwPanel.getToken() != roPanel.getToken())
  }
}