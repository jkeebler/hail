package org.broadinstitute.hail.methods

import org.apache.hadoop
import breeze.linalg._
import org.broadinstitute.hail.Utils._

import scala.collection.mutable.ArrayBuffer
import scala.io.Source

case class CovariateData(covRowSample: Array[Int], covName: Array[String], data: DenseMatrix[Double])

object CovariateData {

  def read(filename: String, hConf: hadoop.conf.Configuration, sampleIds: IndexedSeq[String]): CovariateData = {
    readFile(filename, hConf) { s =>
      val lines = Source.fromInputStream(s)
        .getLines()
        .filterNot(_.isEmpty)

      val header = lines.next()
      val covName = header.split("\\s+").tail

      val covRowSampleBuffer = new ArrayBuffer[Int]
      val dataBuffer = new ArrayBuffer[Double]
      var nSamplesDiscarded = 0

      for (line <- lines) {
        val entries = line.split("\\s+").iterator
        sampleIds.zipWithIndex.toMap.get(entries.next()) match {
          case Some(i) => covRowSampleBuffer += i; dataBuffer ++= entries.map(_.toDouble)
          case None => nSamplesDiscarded += 1
        }
      }

      val covRowSample = covRowSampleBuffer.toArray

      if (!covRowSample.areDistinct()) // FIXME: should I move this check to the case class body?
        fatal("Covariate sample names are not unique.")

      warnIfSamplesDiscarded(nSamplesDiscarded, "in .cov discarded: missing from variant data set.")

      val data = new DenseMatrix[Double](rows = covRowSample.size, cols = covName.size, data = dataBuffer.toArray,
        offset = 0, majorStride = covName.size, isTranspose = true)

      CovariateData(covRowSample, covName, data)
    }
  }
}