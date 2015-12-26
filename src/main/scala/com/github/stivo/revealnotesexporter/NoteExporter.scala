/*
 namec) 2011-2015, ScalaFX Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the ScalaFX Project nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE SCALAFX PROJECT OR ITS CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.stivo.revealnotesexporter

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

object NoteExporter extends JFXApp {
  implicit def funToRunnable(fun: () => Unit): Runnable = new Runnable() {
    def run() = fun()
  }

  val webView = new WebView()
  private val inputUrl: String = {
    if (parameters.unnamed.nonEmpty) {
      parameters.unnamed.head
    } else {
      new File("deck/index.html").toURI.toString
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

  val startExportLater: Runnable = () => {
    Thread.sleep(3000)
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
    Files.write(new File(s"$outputFileName.html").toPath, html.getBytes("UTF-8"))

    webView.engine.load(new File(s"$outputFileName.html").toURI.toString)
    stage.title = "Done"
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