package org.speedd.ml.util.data

import java.awt._
import java.awt.geom._
import javax.swing.{JPanel, JSlider}
import javax.swing.event.{ChangeEvent, ChangeListener}
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

    val dot = new Ellipse2D.Double(-1.5, -1.5, 3, 3)

    for (idx <- 0 until data.getSeries.size)
      chart.getXYPlot.getRenderer.setSeriesShape(idx, dot)

    // Set plot fonts for title and axis
    chart.getTitle.setFont(new Font("SansSerif", Font.BOLD, 14))
    chart.getXYPlot.getDomainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    chart.getXYPlot.getRangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))

    val chartPanel = new ChartPanel(chart)
    chartPanel.setPreferredSize(new Dimension(800, 600))
    chart.getPlot.setBackgroundPaint(null)
    setContentPane(chartPanel)
  }

  private final class XYScatterSlidingPlot(applicationTitle: String, data: SlidingXYDataset, window: Int, chartTitle: String,
                                           xLabel: String, yLabel: String) extends ApplicationFrame(applicationTitle) {

    val chart = ChartFactory
      .createScatterPlot(chartTitle, xLabel, yLabel, data, PlotOrientation.VERTICAL, true, true, false)

    val dot = new Ellipse2D.Double(-1.5, -1.5, 3, 3)
    for (idx <- 0 until data.getUnderlyingDataset.getSeriesCount)
      chart.getXYPlot.getRenderer.setSeriesShape(idx, dot)

    // Set plot fonts for title and axis
    chart.getTitle.setFont(new Font("SansSerif", Font.BOLD, 14))
    chart.getXYPlot.getDomainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    chart.getXYPlot.getRangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))

    val chartPanel = new ChartPanel(chart)
    chartPanel.setPreferredSize(new Dimension(800, 600))
    chart.getPlot.setBackgroundPaint(null)

    val slidingPanel = new SlidingPanel(data, window)
    slidingPanel.add(chartPanel)

    val dashboard = new JPanel(new BorderLayout())
    dashboard.add(slidingPanel.slider)
    slidingPanel.add(dashboard, BorderLayout.SOUTH)

    setContentPane(slidingPanel)
  }

  private final class SlidingPanel(dataset: SlidingXYDataset, window: Int)
    extends JPanel(new BorderLayout()) with ChangeListener {

    val slider: JSlider =
      new JSlider(0, dataset.getUnderlyingDataset.getItemCount(0) - window - 1, 0)

    slider.setPaintLabels(true)
    slider.setPaintTicks(true)
    slider.setMajorTickSpacing(10000)
    slider.addChangeListener(this)

    /**
      * Handles a state change event.
      *
      * @param event the event
      */
    def stateChanged(event: ChangeEvent) = {
      dataset.setFirstItemIndex(slider.getValue)
    }
  }

  /**
    * Displays a plot of a set of given datasets.
    *
    * @param data sequence of datasets to be plotted
    * @param title a title for the plot
    * @param xLabel x label of the plot
    * @param yLabel y label of the plot
    */
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


  /**
    * Displays a slidinig plot of a set of given datasets. The plot
    * can slide over the datasets by the window size specified.
    *
    * @param data sequence of datasets to be plotted
    * @param window window slide size
    * @param title a title for the plot
    * @param xLabel x label of the plot
    * @param yLabel y label of the plot
    */
  def slidingPlot(data: Seq[(Seq[(Double, Double)], String)], window: Int,
                  title: String, xLabel: String, yLabel: String) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val dataset = new SlidingXYDataset(collection, 0, window)

    val chart = new XYScatterSlidingPlot(PLOTTER, dataset, window, title, xLabel, yLabel)
    chart.pack()
    RefineryUtilities.centerFrameOnScreen(chart)
    chart.setVisible(true)

  }
}
