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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.table.JBTable
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.jetbrains.projector.common.protocol.toClient.toClientAddressList
import java.awt.Dimension
import javax.swing.*
import javax.swing.table.DefaultTableModel


class ConnectionPanel : JPanel() {
  private val logger = Logger.getInstance(ConnectionPanel::class.java)
  private val title = JLabel("Current connections")
  private val clientListPath = "/clientList"
  private val disconnectByIpPath = "/disconnectByIp?ip="
  private val disconnectAllPath = "/disconnectAll"
  private val disconnectButton = JButton("Disconnect").apply {
    addActionListener {
      disconnect()
    }
  }
  private val disconnectAllButton = JButton("Disconnect All").apply {
    addActionListener {
      disconnectAll()
    }
  }
  private val updateButton = JButton("Update").apply {
    addActionListener {
      update()
    }
  }
  private val columnNames = arrayOf("Address", "Host Name")
  private val clientTable = JBTable().apply {
    preferredScrollableViewportSize = Dimension(100, 100)
    setDefaultEditor(Any::class.java, null)
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  }

  init {
    isVisible = ProjectorService.isSessionRunning
    if (ProjectorService.isSessionRunning) {
      val buttonPanel = JPanel()
      LinearPanelBuilder(buttonPanel)
        .addNextComponent(updateButton).addNextComponent(disconnectButton).addNextComponent(disconnectAllButton)

      LinearPanelBuilder(this).addNextComponent(title, topGap = 5, bottomGap = 5)
        .startNextLine().addNextComponent(JScrollPane(clientTable))
        .startNextLine().addNextComponent(buttonPanel, topGap = 5)

      update()
    }
  }

  private fun sendHttpRequest(path: String): String {
    val client = HttpClient()
    val url = "http://${ProjectorService.currentSession.host}:${ProjectorService.currentSession.port}$path"
    val response = runBlocking {
      client.get<String>(url)
    }
    logger.debug("Send Get Request\nUrl: $url\nResponse: $response")
    client.close()
    return response
  }

  private fun update() {
    val list = sendHttpRequest(clientListPath).toClientAddressList()
    clientTable.model = DefaultTableModel(
      list.map { arrayOf(it.address, it.hostName) }.toTypedArray(),
      columnNames)
    if (clientTable.model.rowCount > 0) {
      clientTable.setRowSelectionInterval(0, 0)
      disconnectButton.isEnabled = true
      disconnectAllButton.isEnabled = true
    }
    else {
      disconnectButton.isEnabled = false
      disconnectAllButton.isEnabled = false
    }
  }

  private fun disconnectAll() {
    sendHttpRequest(disconnectAllPath)
    update()
  }

  private fun disconnect() {
    val ip = clientTable.model.getValueAt(clientTable.selectedRow, 0).toString()
    sendHttpRequest(disconnectByIpPath + ip)
    update()
  }
}