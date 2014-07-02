package org.continuumio.bokeh
package examples
package glyphs

import sampledata.iris.flowers

import math.{Pi=>pi}

object IrisSplom extends App {
    val colormap = Map("setosa" -> Color.Red, "versicolor" -> Color.Green, "virginica" -> Color.Blue)

    val source = new ColumnDataSource()
        .addColumn('petal_length, flowers.petal_length)
        .addColumn('petal_width, flowers.petal_width)
        .addColumn('sepal_length, flowers.sepal_length)
        .addColumn('sepal_width, flowers.sepal_width)
        .addColumn('color, flowers.species.map(colormap))

    val text_source = new ColumnDataSource()
        .addColumn('xcenter, Array(125))
        .addColumn('ycenter, Array(145))

    val columns = List('petal_length, 'petal_width, 'sepal_width, 'sepal_length)

    val xdr = new DataRange1d().sources(source.columns(columns: _*) :: Nil)
    val ydr = new DataRange1d().sources(source.columns(columns: _*) :: Nil)

    def make_plot(xname: Symbol, yname: Symbol, xax: Boolean=false, yax: Boolean=false, text: Option[String]=None) = {
        val plot = new Plot()
            .x_range(xdr)
            .y_range(ydr)
            .data_sources(source :: Nil)
            .background_fill("#ffeedd")
            .plot_width(250)
            .plot_height(250)
            .border_fill(Color.White)
            .title("")
            .min_border(2)

        val xaxis = if (xax) Some(new LinearAxis().plot(plot).dimension(0).location(Location.Bottom)) else None
        val yaxis = if (yax) Some(new LinearAxis().plot(plot).dimension(1).location(Location.Left)) else None

        val axes = xaxis.toList ++ yaxis.toList

        // val xgrid = new Grid().plot(plot).dimension(0)
        // val ygrid = new Grid().plot(plot).dimension(1)

        val grids = Nil // List(xgrid, ygrid)

        val circle = new Circle()
            .x(xname)
            .y(yname)
            .fill_color('color)
            .fill_alpha(0.2)
            .size(4)
            .line_color('color)

        val circle_renderer = new Glyph()
            .data_source(source)
            .xdata_range(xdr)
            .ydata_range(ydr)
            .glyph(circle)

        val pantool = new PanTool().plot(plot)
        val wheelzoomtool = new WheelZoomTool().plot(plot)

        plot.renderers := axes ++ grids ++ List(circle_renderer)
        plot.tools := List(pantool, wheelzoomtool)

        text.foreach { text =>
            val text_glyph = new Text()
                .x('xcenter, Units.Screen)
                .y('ycenter, Units.Screen)
                .text(text.replaceAll("_", " "))
                .angle(pi/4)
                .text_font_style(FontStyle.Bold)
                .text_baseline(TextBaseline.Top)
                .text_color("#ffaaaa")
                .text_alpha(0.5)
                .text_align(TextAlign.Center)
                .text_font_size("28pt")
            val text_renderer = new Glyph()
                .data_source(text_source)
                .xdata_range(xdr)
                .ydata_range(ydr)
                .glyph(text_glyph)

            plot.data_sources := text_source :: plot.data_sources.value
            plot.renderers := text_renderer :: plot.renderers.value
        }

        plot
    }

    val xattrs = columns
    val yattrs = xattrs.reverse

    val plots: List[List[Plot]] = yattrs.map { y =>
        xattrs.map { x =>
            val xax = y == yattrs.last
            val yax = x == xattrs(0)
            val text = if (x == y) Some(x.name) else None
            make_plot(x, y, xax, yax, text)
        }
    }

    val grid = new GridPlot().children(plots).title("iris_splom")

    val session = new HTMLFileSession("iris_splom.html")
    session.save(grid)
    println(s"Wrote ${session.file}. Open ${session.url} in a web browser.")
}
