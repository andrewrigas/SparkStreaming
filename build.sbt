name := "SparkStreaming"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.0.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" % "spark-streaming-kafka-0-8_2.11" % sparkVersion,
  "org.apache.kafka" %% "kafka" % "1.0.0",
  "org.apache.spark" %% "spark-streaming-twitter" % "1.6.1"
    // https://mvnrepository.com/artifact/org.apache.kafka/kafka


)

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-core" % "3.0.3",
  "org.twitter4j" % "twitter4j-stream" % "3.0.3"
)