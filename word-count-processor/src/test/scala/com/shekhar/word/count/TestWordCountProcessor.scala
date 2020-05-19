package com.shekhar.word.count

import org.apache.nifi.flowfile.FlowFile
import org.apache.nifi.util.{TestRunner, TestRunners}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}

import scala.collection.JavaConverters._

class TestWordCountProcessor extends WordSpec with Matchers with BeforeAndAfterEach {

  // for simplicity, we will stick to only these stop words.
  private val stopWords: Seq[String] = Seq("of", "this", "the", "me", "myself", "his", "and", "a")

  private val inputData: String = "Before you judge a man, walk a mile in his shoes." +
    " After that who cares?... He’s a mile away and you’ve got his shoes!"

  "Word count processor" should {
    "count occurance of stop words as well when 'skip stop words' property is false" in {
      val testRunner: TestRunner = TestRunners.newTestRunner(new WordCountProcessor)
      testRunner.setProperty(WordCountProcessor.skipStopWordsPropertyName, "false")

      testRunner.enqueue(inputData)
      testRunner.run()

      val uniqueWordsInSentence = 20
      testRunner.assertTransferCount(WordCountProcessor.successRelationName, uniqueWordsInSentence)
      testRunner.assertTransferCount(WordCountProcessor.failureRelationName, 0)

      val wordCounts: Seq[FlowFile] =
        testRunner.getFlowFilesForRelationship(WordCountProcessor.successRelationName).asScala

      val stopWordFlowFiles: Seq[FlowFile] =
        wordCounts.filter(ff => stopWords.contains(ff.getAttribute("word").toLowerCase))

      stopWordFlowFiles.isEmpty shouldBe false
    }

    "not emmit any output flow file when input flow file is null" in {
      val testRunner: TestRunner = TestRunners.newTestRunner(classOf[WordCountProcessor])
      testRunner.setProperty(WordCountProcessor.skipStopWordsPropertyName, "false")
      testRunner.run()
      testRunner.assertTransferCount(WordCountProcessor.failureRelationName, 0)
      testRunner.assertTransferCount(WordCountProcessor.successRelationName, 0)
    }

    "not emmit any output flow file when input flow file contents are empty" in {
      val testRunner: TestRunner = TestRunners.newTestRunner(classOf[WordCountProcessor])
      testRunner.setProperty(WordCountProcessor.skipStopWordsPropertyName, "false")
      val inputData: String = ""
      testRunner.enqueue(inputData)
      testRunner.run()

      testRunner.assertTransferCount(WordCountProcessor.failureRelationName, 0)
      testRunner.assertTransferCount(WordCountProcessor.successRelationName, 0)
    }

    "not count occurance of stop words when 'skip stop words' property is true" in {
      val testRunner: TestRunner = TestRunners.newTestRunner(classOf[WordCountProcessor])
      testRunner.setProperty(WordCountProcessor.skipStopWordsPropertyName, "true")
      testRunner.enqueue(inputData)
      testRunner.run()

      testRunner.assertTransferCount(WordCountProcessor.failureRelationName, 0)

      val flowFilesInSuccessRelation: Seq[FlowFile] =
        testRunner.getFlowFilesForRelationship(WordCountProcessor.successRelationName).asScala

      val uniqueWordsInSentence = 20
      val numberOfStopWordsInSentence = 3
      val expectedFlowFiles: Int = uniqueWordsInSentence - numberOfStopWordsInSentence
      flowFilesInSuccessRelation.length shouldEqual expectedFlowFiles

      val stopWordFlowFiles: Seq[FlowFile] =
        flowFilesInSuccessRelation.filter(ff => stopWords.contains(ff.getAttribute("word").toLowerCase))

      stopWordFlowFiles.isEmpty shouldBe true
    }
  }
}
