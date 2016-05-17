package org.speedd.ml.util.data

import org.jfree.chart.{ChartFactory, ChartPanel}
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.ui.{ApplicationFrame, RefineryUtilities}

object Plotter {

  private final val PLOTTER = "SPEEDD Plotter"

  private final class XYScatterPlot(applicationTitle: String, chartTitle: String, data: XYSeriesCollection,
                                    xLabel: String, yLabel: String) extends ApplicationFrame(applicationTitle) {

    val chart = ChartFactory
      .createScatterPlot(chartTitle, xLabel, yLabel, data, PlotOrientation.VERTICAL, true, true, false)

    val chartPanel = new ChartPanel(chart)
    chartPanel.setPreferredSize( new java.awt.Dimension(800, 600) )
    val plot = chart.getPlot
    chart.setBackgroundPaint(null)
    plot.setBackgroundPaint(null)
    chartPanel.setBackground(null)
    setContentPane(chartPanel)
  }

  def plot(data: Seq[(Seq[(Double, Double)], String)], title: String, xLabel: String, yLabel: String) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val chart = new XYScatterPlot(PLOTTER, title, collection, xLabel, yLabel)
    chart.pack()
    RefineryUtilities.centerFrameOnScreen(chart)
    chart.setVisible(true)
  }
}
