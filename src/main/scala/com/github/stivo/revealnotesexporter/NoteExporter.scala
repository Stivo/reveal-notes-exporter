package com.github.stivo.revealnotesexporter

import java.awt.{EventQueue, Desktop}
import java.io.File
import java.nio.file.Files

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.pegdown.PegDownProcessor

import scala.io.Source
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.scene.Scene
import scalafx.scene.paint._
import scalafx.scene.web.WebView
import scalafx.stage.StageStyle
import language.implicitConversions

object NoteExporter extends JFXApp {
  implicit def funToRunnable(fun: () => Unit): Runnable = new Runnable() {
    def run() = fun()
  }

  val webView = new WebView()
  private val inputUrl: String = {
    if (parameters.unnamed.nonEmpty) {
      parameters.unnamed.head
    } else {
      new File("decks/fast-track-to-scala/index.html").toURI.toString
    }
  }
  private val outputFileName: String = parameters.named.getOrElse("fileName", "notes")

  webView.engine.load(inputUrl)
  webView.engine.javaScriptEnabled = true

  private val exportNotesScript: String = readScript("exportNotes.js")
  private val getTitleScript: String = readScript("getTitle.js")

  def readScript(name: String): String = {
    Source.fromFile(new File("src/main/javascript/"+name)).mkString
  }

  val startExportLater = () => {
    Thread.sleep(5000)
    Platform.runLater {
      println("Starting script")
      doExport()
    }
  }

  def doExport(): Unit = {
    val title = readTitle()
    val notes = readNotes()

    val markdown: String = convertToMarkdown(title, notes)
    Files.write(new File(s"$outputFileName.md").toPath, markdown.getBytes("UTF-8"))

    val html: String = createHtml(markdown)
    val outputHtmlFile: File = new File(s"$outputFileName.html")
    Files.write(outputHtmlFile.toPath, html.getBytes("UTF-8"))

    EventQueue.invokeLater {() =>
      Desktop.getDesktop.open(outputHtmlFile)
      System.exit(0)
    }
  }

  def createHtml(markdown: String): String = {
    s"""<html>
       | <head>
       |   <meta charset="UTF-8">
       | </head>
       | <body>
       |  ${new PegDownProcessor().markdownToHtml(markdown)}
       | </body>
       |</html>""".stripMargin
  }

  def readTitle(): String = webView.engine.executeScript(getTitleScript) match {
    case title: String => title
  }

  def readNotes(): Seq[NoteEntry] = {
    def readNext() = {
      webView.engine.executeScript(exportNotesScript) match {
        case x: String =>
          val notes: NoteEntry = parseNotes(x)
          if (notes != null) {
            stage.title = "Note Exporter at " + notes.number
            println("Exporting notes for "+notes.number)
          }
          Option(notes)
      }
    }
    Stream.continually(readNext()).takeWhile(_.isDefined).flatten.toList
  }

  new Thread(startExportLater).start()
  stage = new PrimaryStage {
    initStyle(StageStyle.UNIFIED)
    title = "Note Exporter"
    scene = new Scene {
      fill = Color.rgb(38, 38, 38)
      content = webView
    }
  }

  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def parseNotes(s: String): NoteEntry = mapper.readValue[NoteEntry](s)

  def convertToMarkdown(title: String, list: Seq[NoteEntry]): String = {
    val entries = list.map(_.toMarkdown).mkString("\n")
    s"""## Notes for "$title"
       |$entries
     """.stripMargin
  }
}

case class NoteEntry(number: Int, notes: String, title: String) {
  def toMarkdown: String = {
    s"""#### $number: $title
       |$notes
       |""".stripMargin
  }
}