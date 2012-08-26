package com.ayosec.wikispray

import org.scalatest.matchers.{Matcher, MatchResult}

trait ExtraMatchers {

  def beOfType(cls: Class[_]) = Matcher { obj: Any =>
    MatchResult(
      obj.getClass == cls,
      obj.toString + " was not an instance of " + cls.toString,
      obj.toString + " was an instance of " + cls.toString
    )
  }

}
