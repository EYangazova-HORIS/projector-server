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
import com.intellij.util.containers.toArray
import org.jetbrains.projector.server.ProjectorServer
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.random.Random

object Utils {
  private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

  fun copyToClipboard(string: String) {
    Toolkit
      .getDefaultToolkit()
      .systemClipboard
      .setContents(StringSelection(string), null)
  }

  fun getPassword(): String {
    return (1..11)
      .map { Random.nextInt(0, charPool.size) }
      .map(charPool::get)
      .joinToString("")
  }

  fun getHosts(): Array<String> {
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

  fun getPort(): String {
    val port = System.getProperty(ProjectorServer.PORT_PROPERTY_NAME)?.toIntOrNull() ?: ProjectorServer.DEFAULT_PORT
    return port.toString()
  }

  fun getUrl(host: String, port: String, token: String?): String {
    return "http://${host}:${port}" + if (token == null) "" else "/?token=${token}"
  }
}