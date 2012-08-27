package com.ayosec.wikispray

import org.scalatest.matchers.{Matcher, BeMatcher, MatchResult}

trait ExtraMatchers {

  def beOfType[T: Manifest] = Matcher { obj: Any =>
    val cls = manifest[T].erasure
    MatchResult(
      obj.getClass == cls,
      obj.toString + " was not an instance of " + cls.toString,
      obj.toString + " was an instance of " + cls.toString
    )
  }

  def ofType[T: Manifest] = BeMatcher { obj: Any =>
    val cls = manifest[T].erasure
    MatchResult(
      obj.getClass == cls,
      obj.toString + " was not an instance of " + cls.toString,
      obj.toString + " was an instance of " + cls.toString
    )
  }

}
