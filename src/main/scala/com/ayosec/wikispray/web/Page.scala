package com.ayosec.wikispray.web

import com.ayosec.wikispray.moon.MoonDB
import com.ayosec.wikispray.moon.MoonDocument
import com.ayosec.wikispray.moon.DocumentContext
import org.joda.time.DateTime

class Page(context: DocumentContext) extends MoonDocument(context) {

  // Getters
  def summary = read[String]("summary")
  def content = read[String]("content")
  def date = read[java.util.Date]("date") map { new DateTime(_) }

  // Setters
  def summary_=(s: String) = write("summary", s)
  def content_=(c: String) = write("content", c)
  def date_=(d: DateTime) = write("date", d.toDate)

}

object Page {
  def apply(summary: String, content: String, date: DateTime)(implicit moon: MoonDB) = {
    val page = moon("pages").build().mutate(new Page(_))
    page.summary = summary
    page.content = content
    page.date = date
    page
  }

}
