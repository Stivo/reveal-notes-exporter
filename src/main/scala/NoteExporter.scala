/*
 * Copyright (c) 2011-2015, ScalaFX Project
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

package hello

import java.io.File
import java.nio.file.{Path, Files}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

import scala.io.Source
import scalafx.application.{Platform, JFXApp}
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.paint._
import scalafx.scene.web.WebView
import scalafx.stage.StageStyle

object NoteExporter extends JFXApp {
  implicit def funToRunnable(fun: () => Unit) = new Runnable() {
    def run() = fun()
  }

  val webView = new WebView()
  private val string: String = new File("deck/index.html").toURI.toString
  println(string)
  webView.engine.load(string)
  webView.engine.javaScriptEnabled = true

  private val script: String = Source.fromFile(new File("src/main/javascript/exportNotes.js")).mkString
  println(script)
  val startExportLater: Runnable = () => {
    Thread.sleep(2000)
    Platform.runLater {
      println("Starting script")
      exportNotes()
    }
  }

  def exportNotes(): Unit = {
    def getNext() = {
      webView.engine.executeScript(script) match {
        case x: String => Option(parseNotes(x))
      }
    }
    val list = Stream.continually(getNext()).takeWhile(_.isDefined).flatten.toList
    val markdown: String = convertToMarkdown(list)
    Files.write(new File("markdown.md").toPath, markdown.getBytes("UTF-8"))

    System.exit(1)
  }
  new Thread(startExportLater).start()
  stage = new PrimaryStage {
    initStyle(StageStyle.UNIFIED)
    title = "ScalaFX Hello World"
    scene = new Scene {
      fill = Color.rgb(38, 38, 38)
      content = webView
    }
  }

  def parseNotes(s: String) = {
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val entries = mapper.readValue[NoteEntry](s)
    entries
  }

  def convertToMarkdown(list: Seq[NoteEntry]): String = {
    val entries = list.map(_.toMarkdown()).mkString("\n")
    ""
  }
}

case class NoteEntry(number: Int, notes: String, title: String) {
  def toMarkdown(): String = {
    s"""#### $number: $title
       |$notes
       |""".stripMargin
  }
}