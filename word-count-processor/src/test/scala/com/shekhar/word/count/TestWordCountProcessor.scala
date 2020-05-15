package com.shekhar.word.count

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class TestWordCountProcessor extends WordSpec with Matchers with BeforeAndAfterAll {

  "Word count processor" should {
    "not emmit any output flow file when input flow file is null" in {
    }
    "not emmit any output flow file when input flow file contents are empty" in {
    }
    "count occurance of stop words as well when 'process stop words' property is true" in {
    }
    "not count occurance of stop words as well when 'process stop words' property is false" in {
    }
  }
}
