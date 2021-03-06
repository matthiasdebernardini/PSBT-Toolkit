package org.psbttoolkit.gui.transactions

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.stream.Materializer
import akka.util.ByteString
import org.bitcoins.commons.jsonmodels.SerializedTransaction
import org.bitcoins.core.config.{MainNet, RegTest, SigNet, TestNet3}
import org.bitcoins.core.protocol.script.ScriptPubKey
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.core.script.constant.{
  ScriptConstant,
  ScriptNumberOperation,
  ScriptToken
}
import org.bitcoins.crypto.NetworkElement
import org.psbttoolkit.gui.GlobalData.system
import org.psbttoolkit.gui.transactions.dialog._
import org.psbttoolkit.gui.{GlobalData, TaskRunner}
import play.api.libs.json.Json
import scalafx.beans.property.ObjectProperty
import scalafx.scene.control.TextArea
import scalafx.stage.Window

import scala.concurrent.ExecutionContextExecutor
import scala.util.Try

class TransactionsPaneModel(resultArea: TextArea) {
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

  def getTransactionOpt: Option[Transaction] = {
    Try(Transaction.fromHex(resultArea.text.value)).toOption
  }

  def getScriptOpt: Option[ScriptPubKey] = {
    Try(ScriptPubKey.fromHex(resultArea.text.value)).toOption
  }

  def broadcastTx(): Unit = {
    implicit val m: Materializer = Materializer(system)
    implicit val ec: ExecutionContextExecutor = m.executionContext

    taskRunner.run(
      caption = "Broadcast Transaction",
      op = getTransactionOpt match {
        case Some(tx) =>
          val url = GlobalData.network match {
            case MainNet =>
              "https://blockstream.info/api/tx"
            case TestNet3 =>
              "https://blockstream.info/testnet/api/tx"
            case RegTest | SigNet =>
              throw new IllegalArgumentException(
                "Unable broadcast a regtest or signet transaction")
          }

          val resultF = Http()
            .singleRequest(Post(url, tx.hex))
            .flatMap(response =>
              response.entity.dataBytes
                .runFold(ByteString.empty)(_ ++ _)
                .map(payload => payload.decodeString(ByteString.UTF_8)))

          resultF.map { result =>
            if (result != tx.txIdBE.hex) {
              throw new RuntimeException(result)
            }
          }
        case None =>
          throw new RuntimeException("Missing Transaction")
      }
    )
  }

  def decodeTransaction(): Unit = {
    val resultOpt = getTransactionOpt.map { tx =>
      val decoded = SerializedTransaction.decodeRawTransaction(tx)
      DecodedDataDialog.showAndWait(parentWindow.value,
                                    "Decoded Transaction",
                                    Json.prettyPrint(decoded.toJson))
    }

    taskRunner.run(
      caption = "Decode Transaction",
      op = getTransactionOpt match {
        case Some(_) =>
          resultOpt.map(_ => ())
        case None =>
          throw new RuntimeException("Missing Transaction")
      }
    )
  }

  private def tokenToString(token: ScriptToken): String = {
    token match {
      case numOp: ScriptNumberOperation => numOp.toString
      case constOp: ScriptConstant      => constOp.bytes.toString
      case otherOp: ScriptToken         => otherOp.toString
    }
  }

  def decodeScript(): Unit = {
    val resultOpt = getScriptOpt.map { spk =>
      val tokens = spk.asm
      val decoded = tokens.map(tokenToString).mkString(" ")
      DecodedDataDialog.showAndWait(parentWindow.value,
                                    "Decoded Script",
                                    decoded)
    }

    taskRunner.run(
      caption = "Decode Script",
      op = getScriptOpt match {
        case Some(_) =>
          resultOpt.map(_ => ())
        case None =>
          throw new RuntimeException("Missing or invalid Script")
      }
    )
  }

  def constructTransaction(): Unit = {
    val resultOpt = ConstructTransactionDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Construct Transaction",
      op = resultOpt match {
        case Some(tx) =>
          setResult(tx)
        case None =>
          ()
      }
    )
  }

  def createPubKeyScript(): Unit = {
    val resultOpt = CreatePubKeyScriptDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Create Public Key Script",
      op = resultOpt match {
        case Some(spk) =>
          setResult(spk)
        case None =>
          ()
      }
    )
  }

  def createP2SHScript(): Unit = {
    val resultOpt = CreateP2SHScriptDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Create P2SH Script",
      op = resultOpt match {
        case Some(spk) =>
          setResult(spk)
        case None =>
          ()
      }
    )
  }

  def createMultiSigScript(): Unit = {
    val resultOpt = CreateMultisigSPKDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Create Multisig Script",
      op = resultOpt match {
        case Some(spk) =>
          setResult(spk)
        case None =>
          ()
      }
    )
  }

  def createConditionalScript(): Unit = {
    val resultOpt =
      CreateConditionalScriptDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Create Conditional Script",
      op = resultOpt match {
        case Some(spk) =>
          setResult(spk)
        case None =>
          ()
      }
    )
  }

  def createAddress(): Unit = {
    val resultOpt =
      CreateAddressDialog.showAndWait(parentWindow.value)

    taskRunner.run(
      caption = "Create Address",
      op = resultOpt match {
        case Some(address) =>
          setResult(address.toString())
        case None =>
          ()
      }
    )
  }
}
