package org.psbttoolkit.gui.psbts.dialog

import org.bitcoins.core.crypto.ExtKey
import org.bitcoins.core.hd.BIP32Path
import org.bitcoins.crypto.ECPublicKey
import org.psbttoolkit.gui.GlobalData
import scalafx.Includes._
import scalafx.geometry.Insets
import scalafx.scene.control.{ButtonType, Dialog, Label, TextField}
import scalafx.scene.layout.GridPane
import scalafx.stage.Window

import scala.util.{Failure, Success, Try}

object AddKeyPathDialog {

  def showAndWait(
      isInput: Boolean,
      parentWindow: Window): Option[(Int, ExtKey, ECPublicKey, BIP32Path)] = {

    val typeStr = if (isInput) "Input" else "Output"

    val dialog = new Dialog[Option[(Int, ExtKey, ECPublicKey, BIP32Path)]]() {
      initOwner(parentWindow)
      title = s"Add $typeStr Key Path"
    }

    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
    dialog.dialogPane().stylesheets = GlobalData.currentStyleSheets

    val indexTF = new TextField()
    val extKeyTF = new TextField() {
      promptText = "hex or base64"
    }
    val pubKeyTF = new TextField()
    val keyPathTF = new TextField()

    dialog.dialogPane().content = new GridPane {
      hgap = 10
      vgap = 10
      padding = Insets(20, 100, 10, 10)

      var nextRow: Int = 0

      def addRow(label: String, textField: TextField): Unit = {
        add(new Label(label), 0, nextRow)
        add(textField, 1, nextRow)
        nextRow += 1
      }

      addRow(s"$typeStr Index", indexTF)
      addRow("Ext Key", extKeyTF)
      addRow("Pub Key", pubKeyTF)
      addRow("Key Path", keyPathTF)
    }

    // Enable/Disable OK button depending on whether all data was entered.
    val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)
    // Simple validation that sufficient data was entered
    okButton.disable <== indexTF.text.isEmpty || extKeyTF.text.isEmpty || pubKeyTF.text.isEmpty || keyPathTF.text.isEmpty

    // When the OK button is clicked, convert the result to a T.
    dialog.resultConverter = dialogButton =>
      if (dialogButton == ButtonType.OK) {
        Some(
          (indexTF.text.value.toInt,
           getExtKey(extKeyTF.text.value).get,
           ECPublicKey(pubKeyTF.text.value),
           getBIP32Path(keyPathTF.text.value).get))
      } else None

    dialog.showAndWait() match {
      case Some(
            Some(
              (index: Int, ext: ExtKey, key: ECPublicKey, path: BIP32Path))) =>
        Some((index, ext, key, path))
      case Some(_) | None => None
    }
  }

  private def getExtKey(str: String): Try[ExtKey] = {
    ExtKey.fromStringT(str) match {
      case Success(key) => Success(key)
      case Failure(_) =>
        Try(ExtKey.fromHex(str))
    }
  }

  private def getBIP32Path(str: String): Try[BIP32Path] = {
    Try(BIP32Path.fromString(str)) match {
      case Success(key) => Success(key)
      case Failure(_) =>
        Try(BIP32Path.fromHex(str))
    }
  }
}
