package org.speedd.ml.util.data

import java.awt._
import java.awt.geom._
import java.io._
import javax.swing.{JPanel, JSlider}
import javax.swing.event.{ChangeEvent, ChangeListener}
import com.itextpdf.awt.DefaultFontMapper
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfWriter
import org.jfree.chart._
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.xy.{XYSeries, XYSeriesCollection}
import org.jfree.ui.{ApplicationFrame, RefineryUtilities}

object Plotter {

  private final val PLOTTER = "SPEEDD Plotter"

  private final class XYScatterPlot(applicationTitle: String, chartTitle: String, data: XYSeriesCollection,
                                    xLabel: String, yLabel: String, lbRange: Option[Double] = None,
                                    ubRange: Option[Double] = None) extends ApplicationFrame(applicationTitle) {

    val chart = ChartFactory
      .createScatterPlot(chartTitle, xLabel, yLabel, data, PlotOrientation.VERTICAL, true, true, false)

    val dot = new Ellipse2D.Double(-1.5, -1.5, 2, 2)

    for (idx <- 0 until data.getSeries.size)
      chart.getXYPlot.getRenderer.setSeriesShape(idx, dot)

    // Set plot fonts for title and axis
    chart.getTitle.setFont(new Font("SansSerif", Font.BOLD, 14))
    chart.getXYPlot.getDomainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    chart.getXYPlot.getRangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    if (lbRange.isDefined && ubRange.isDefined)
      chart.getXYPlot.getRangeAxis.setRange(lbRange.get, ubRange.get)

    val chartPanel = new ChartPanel(chart)
    chartPanel.setPreferredSize(new Dimension(800, 600))
    chart.getPlot.setBackgroundPaint(null)
    setContentPane(chartPanel)
  }

  private final class XYScatterSlidingPlot(applicationTitle: String, data: SlidingXYDataset, window: Int, chartTitle: String,
                                           xLabel: String, yLabel: String, lbRange: Option[Double] = None,
                                           ubRange: Option[Double] = None) extends ApplicationFrame(applicationTitle) {

    val chart = ChartFactory
      .createScatterPlot(chartTitle, xLabel, yLabel, data, PlotOrientation.VERTICAL, true, true, false)

    val dot = new Ellipse2D.Double(-1.5, -1.5, 3, 3)
    for (idx <- 0 until data.getUnderlyingDataset.getSeriesCount)
      chart.getXYPlot.getRenderer.setSeriesShape(idx, dot)

    // Set plot fonts for title and axis
    chart.getTitle.setFont(new Font("SansSerif", Font.BOLD, 14))
    chart.getXYPlot.getDomainAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    chart.getXYPlot.getRangeAxis.setLabelFont(new Font("SansSerif", Font.BOLD, 12))
    if (lbRange.isDefined && ubRange.isDefined)
      chart.getXYPlot.getRangeAxis.setRange(lbRange.get, ubRange.get)

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
    * @param lbRange lower bound for range axis (optional)
    * @param ubRange upper bound for range axis (optional)
    */
  def plot(data: Seq[(Seq[(Double, Double)], String)], title: String, xLabel: String, yLabel: String,
           lbRange: Option[Double] = None, ubRange: Option[Double] = None) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val chart = new XYScatterPlot(PLOTTER, title, collection, xLabel, yLabel, lbRange, ubRange)
    chart.pack()
    RefineryUtilities.centerFrameOnScreen(chart)
    chart.setVisible(true)
  }


  /**
    * Displays a sliding plot of a set of given datasets. The plot
    * can slide over the datasets by the window size specified.
    *
    * @param data sequence of datasets to be plotted
    * @param window window slide size
    * @param title a title for the plot
    * @param xLabel x label of the plot
    * @param yLabel y label of the plot
    * @param lbRange lower bound for range axis (optional)
    * @param ubRange upper bound for range axis (optional)
    */
  def slidingPlot(data: Seq[(Seq[(Double, Double)], String)], window: Int, title: String,
                  xLabel: String, yLabel: String, lbRange: Option[Double] = None, ubRange: Option[Double] = None) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val dataset = new SlidingXYDataset(collection, 0, window)

    val chart = new XYScatterSlidingPlot(PLOTTER, dataset, window, title, xLabel, yLabel, lbRange, ubRange)
    chart.pack()
    RefineryUtilities.centerFrameOnScreen(chart)
    chart.setVisible(true)
  }

  /**
    * Saves the plot of a set of given datasets as a png image.
    *
    * @param data sequence of datasets to be plotted
    * @param xLabel x label of the plot
    * @param yLabel y label of the plot
    * @param filename a filename for the image, e.g. output.png
    * @param title a title for the plot (optional)
    * @param lbRange lower bound for range axis (optional)
    * @param ubRange upper bound for range axis (optional)
    * @param width the width of the image in pixels (default 800)
    * @param height the height of the image in pixels (default 600)
    */
  def plotImage(data: Seq[(Seq[(Double, Double)], String)], xLabel: String, yLabel: String,
                filename: String, title: Option[String] = None, lbRange: Option[Double] = None,
                ubRange: Option[Double] = None, width: Int = 800, height: Int = 600) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val chart = title match {
      case None =>
        new XYScatterPlot(PLOTTER, "", collection, xLabel, yLabel, lbRange, ubRange)
      case Some(name) =>
        new XYScatterPlot(PLOTTER, name, collection, xLabel, yLabel, lbRange, ubRange)
    }

    ChartUtilities.saveChartAsPNG(new File(s"./$filename"), chart.chart, width, height)
  }

  /**
    * Saves the plot of a set of given datasets as a pdf file.
    *
    * @param data sequence of datasets to be plotted
    * @param xLabel x label of the plot
    * @param yLabel y label of the plot
    * @param filename a filename for the pdf, e.g. output.pdf
    * @param title a title for the plot (optional)
    * @param lbRange lower bound for range axis (optional)
    * @param ubRange upper bound for range axis (optional)
    * @param width the width of the pdf plot in pixels (default 800)
    * @param height the height of the pdf plot in pixels (default 600)
    */
  def plotPDF(data: Seq[(Seq[(Double, Double)], String)], xLabel: String, yLabel: String,
              filename: String, title: Option[String] = None, lbRange: Option[Double] = None,
              ubRange: Option[Double] = None, width: Int = 500, height: Int = 400) = {

    val collection = new XYSeriesCollection
    data.foreach { dataSet =>
      val sequence = new XYSeries(dataSet._2)
      dataSet._1.foreach { case (x, y) =>
        sequence.add(x, y)
      }
      collection.addSeries(sequence)
    }

    val chart = title match {
      case None =>
        new XYScatterPlot(PLOTTER, "", collection, xLabel, yLabel, lbRange, ubRange)
      case Some(name) =>
        new XYScatterPlot(PLOTTER, name, collection, xLabel, yLabel, lbRange, ubRange)
    }

    val document = new Document

    try {
      val writer = PdfWriter.getInstance(document, new FileOutputStream(s"./$filename"))
      document.open()

      val contentByte = writer.getDirectContent
      val template = contentByte.createTemplate(width, height)

      val graphics2d = template.createGraphics(width, height, new DefaultFontMapper())
      val rectangle2d = new Rectangle2D.Double(0, 0, width, height)

      chart.chart.draw(graphics2d, rectangle2d)

      graphics2d.dispose()
      contentByte.addTemplate(template, 0, 0)

    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    finally {
      document.close()
    }
  }

}
