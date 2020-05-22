package com.shekhar.word.count

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util

import com.typesafe.scalalogging.LazyLogging
import org.apache.nifi.annotation.documentation.{CapabilityDescription, Tags}
import org.apache.nifi.components.PropertyDescriptor
import org.apache.nifi.flowfile.FlowFile
import org.apache.nifi.processor.io.InputStreamCallback
import org.apache.nifi.processor.util.StandardValidators
import org.apache.nifi.processor.{AbstractProcessor, ProcessContext, ProcessSession, ProcessorInitializationContext, Relationship}
import org.apache.nifi.stream.io.StreamUtils

import scala.collection.JavaConverters._

/**
 * As name suggests, this processor does word count operation on flow files.
 * It extends AbstractProcessor class given by NiFi and
 * it also extends LazyLogging trait give by typesafe logging.
 */
@Tags(Array("word", "count", "custom"))
@CapabilityDescription(
  "This processor reads contents of a flow file," +
    " converts it to a string and splits the string on whitespace," +
    " counts occurance of each word and finally" +
    " emits one flow file for each word." +
    " Each flow file has two attributes called 'word' and 'count'."
)
class WordCountProcessor extends AbstractProcessor with LazyLogging {

  /**
   * successful flow files will be directed to this relation
   */
  val SUCCESS: Relationship = new Relationship.Builder().name(WordCountProcessor.successRelationName).build
  /**
   * Flow files will be redirected to this relation if processor encounters problem
   * while processing a flow file.
   */
  val FAILURE: Relationship = new Relationship.Builder().name(WordCountProcessor.failureRelationName).build

  // for simplicity, we will stick to only these stop words.
  private val stopWords: Seq[String] = Seq("of", "this", "the", "me", "myself", "his", "and", "a")

  /**
   * Processor will use value of this property descriptor to determine if
   * we need to consider stop words like of, the, will be counted too or not.
   * If value for this property is true then stop words will be counted;
   * otherwise not.
   */
  var skipStopWords: PropertyDescriptor = _

  /**
   * Build the property descriptor object
   *
   * @param context
   */
  override def init(context: ProcessorInitializationContext): Unit = {
    skipStopWords = new PropertyDescriptor.Builder()
      .name(WordCountProcessor.skipStopWordsPropertyName)
      .displayName("Skip stop words")
      .description("Set to true so that processor will not count stop words like the, of, etc.")
      .required(false)
      .defaultValue("true")
      .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
      .build()
  }

  override def getSupportedPropertyDescriptors: util.List[PropertyDescriptor] = {
    Seq(skipStopWords).asJava
  }

  override def getRelationships: util.Set[Relationship] = {
    Set(SUCCESS, FAILURE).asJava
  }

  /**
   * We will process incoming flow file through this method.
   *
   * @param processContext
   * @param processSession
   */
  override def onTrigger(processContext: ProcessContext
                         , processSession: ProcessSession): Unit = {
    val flowFile: FlowFile = processSession.get()
    if (flowFile != null) {
      val skipStopWords: Boolean = processContext
        .getProperty(WordCountProcessor.skipStopWordsPropertyName)
        .getValue
        .toBoolean
      process(flowFile, skipStopWords, processSession)
    }
  }

  private def process(inputFlowFile: FlowFile
                      , skipStopWords: Boolean
                      , session: ProcessSession): Unit = {
    val flowFileContents: String = this.readFlowFileContents(inputFlowFile, session)

    // we want to filter out blank strings
    // and when skipStopWords is set to true then we should filter out stop words
    val words: Seq[String] = flowFileContents
      .toLowerCase
      .split(" ")
      .filterNot(_.trim.length == 0)
      .filterNot(word => skipStopWords && stopWords.contains(word))

    // We will do word count here
    val wordCounts: Map[String, Int] = words
      .groupBy((word: String) => word)
      .mapValues(_.length)

    // Generate flow file for each word.
    // for each flow file add word attribute and
    // also other attribute called count
    val outputFlowFiles: Seq[FlowFile] = wordCounts
      .map(wordCount => {
        val outputFlowFile: FlowFile = session.clone(inputFlowFile)
        session.putAttribute(outputFlowFile, "word", wordCount._1)
        session.putAttribute(outputFlowFile, "count", wordCount._2.toString)
        outputFlowFile
      }).toSeq
    // transfer all the generated flow files to success relationship.
    session.transfer(outputFlowFiles.asJava, SUCCESS)

    // Now we will remove original input file from which we read all the words
    session.remove(inputFlowFile)
  }

  /**
   * This method reads all the contents of given flow file and converts contents to String
   * @param flowFile flow file from which content will be read
   * @param session We need process session object to read contents of flow file
   * @return contents of flow file in String format
   */
  private def readFlowFileContents(flowFile: FlowFile, session: ProcessSession): String = {
    val flowFileContentsBuffer = new Array[Byte](flowFile.getSize.toInt)
    session.read(flowFile, new InputStreamCallback {
      override def process(inputStream: InputStream): Unit = {
        StreamUtils.fillBuffer(inputStream, flowFileContentsBuffer, true)
      }
    })
    new String(flowFileContentsBuffer, StandardCharsets.UTF_8)
  }
}

object WordCountProcessor {
  /**
   * We can get reference to property descriptor using this name.
   * Once we have reference to property descriptor, we can get its current value
   */
  val skipStopWordsPropertyName: String = "skip-stop-words"

  /**
   * We can get reference to relationship using this name
   */
  val successRelationName: String = "success"
  val failureRelationName: String = "failure"
}
