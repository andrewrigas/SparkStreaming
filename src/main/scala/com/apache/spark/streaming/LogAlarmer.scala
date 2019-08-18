
package com.apache.spark.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.storage.StorageLevel
import java.util.regex.Pattern
import java.util.regex.Matcher

import com.utilities.Utilities._
import java.util.concurrent._
import java.util.concurrent.atomic._

import org.apache.spark.streaming.dstream.DStream

/** Monitors a stream of Apache access logs on port 9999, and prints an alarm
 *  if an excessive ratio of errors is encountered.
 */
object LogAlarmer {
  
  def main(args: Array[String]) {

    //For this exercise you have to execute
    //nc -kl 9999 < access_log.tx
    //THis will create a streaming pipeline on 9999 port and collected it by our receiver

    // Create the context with a 1 second batch size
    val ssc = new StreamingContext("local[*]", "LogAlarmer", Seconds(1))
    
    setupLogging()
    
    // Construct a regular expression (regex) to extract fields from raw Apache log lines
    val pattern = apacheLogPattern()

    // Create a socket stream to read log data published via netcat on port 9999 locally
    val lines = ssc.socketTextStream("127.0.0.1", 9999, StorageLevel.MEMORY_AND_DISK_SER)
    
    // Extract the status field from each log line
    val statuses: DStream[String] = lines.map(x => {
          val matcher:Matcher = pattern.matcher(x)
          if (matcher.matches()) matcher.group(6) else "[error]"
        }
    )
    
    // Now map these status results to success and failure
    val successFailure: DStream[String] = statuses.map(x => {
      val statusCode = util.Try(x.toInt) getOrElse 0
      if (statusCode >= 200 && statusCode < 300) {
        "Success"
      } else if (statusCode >= 500 && statusCode < 600) {
        "Failure"
      } else {
        "Other"
      }
    })
    
    // Tally up statuses over a 5-minute window sliding every second
    val statusCounts: DStream[(String, Long)] = successFailure.countByValueAndWindow(Seconds(300), Seconds(1))
    
    // For each batch, get the RDD's representing data from our current window
    statusCounts.foreachRDD((rdd, _) => {
      // Keep track of total success and error codes from each RDD
      var totalSuccess:Long = 0
      var totalError:Long = 0

      if (rdd.count() > 0) {
        val elements = rdd.collect()
        for (element <- elements) {
          val result = element._1
          val count = element._2
          if (result == "Success") {
            totalSuccess += count
          }
          if (result == "Failure") {
            totalError += count
          }
        }
      }

      // Print totals from current window
      println("Total success: " + totalSuccess + " Total failure: " + totalError)
      
      // Don't alarm unless we have some minimum amount of data to work with
      if (totalError + totalSuccess > 100) {
        // Compute the error rate
        // Note use of util.Try to handle potential divide by zero exception
        val ratio:Double = util.Try( totalError.toDouble / totalSuccess.toDouble ) getOrElse 1.0
        // If there are more errors than successes, wake someone up
        if (ratio > 0.5) {
          // In real life, you'd use JavaMail or Scala's courier library to send an
          // email that causes somebody's phone to make annoying noises, and you'd
          // make sure these alarms are only sent at most every half hour or something.
          println("Wake somebody up! Something is horribly wrong.")
        } else {
          println("All systems go.")
        }
      }
    })
    
    // Also in real life, you'd need to monitor the case of your site freezing entirely
    // and traffic stopping. In other words, don't use this script to monitor a real
    // production website! There's more you need.
    
    // Kick it off
    ssc.checkpoint("src/main/resources/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }
}

