package org.psbttoolkit.gui.crypto

import org.bitcoins.crypto.{ECPrivateKey, ECPublicKey, NetworkElement}
import org.psbttoolkit.gui.TaskRunner
import org.psbttoolkit.gui.crypto.dialog.HashDataDialog
import org.psbttoolkit.gui.transactions.dialog.ConstructTransactionDialog
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.TextArea
import scalafx.stage.Window

class CryptoPaneModel(resultArea: TextArea) {
  var taskRunner: TaskRunner = _

  // Sadly, it is a Java "pattern" to pass null into
  // constructors to signal that you want some default
  val parentWindow: ObjectProperty[Window] =
    ObjectProperty[Window](null.asInstanceOf[Window])

  def setResult(str: String): Unit = {
    resultArea.text = str
  }

  def setResult(tx: NetworkElement): Unit = {
    setResult(tx.hex)
  }

  def genPrivateKey(): Unit = {
    taskRunner.run("Generate Private Key",
                   setResult(ECPrivateKey.freshPrivateKey))
  }

  def genPublicKey(): Unit = {
    taskRunner.run("Generate Public Key", setResult(ECPublicKey.freshPublicKey))
  }

  def hashData(): Unit = {
    val resultOpt = HashDataDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Hash Data",
      op = resultOpt match {
        case Some(hash) =>
          setResult(hash)
        case None =>
          ()
      }
    )
  }
}
