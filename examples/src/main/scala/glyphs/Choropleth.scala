package org.continuumio.bokeh
package examples
package glyphs

import sampledata.USState.{AK,HI}

object Choropleth extends App {
    val excluded_states: Set[sampledata.USState] = Set(AK, HI)

    val us_states = sampledata.us_states -- excluded_states

    val us_counties = sampledata.us_counties.filterNot { case (_, county) =>
        excluded_states contains county.state
    }

    val unemployment = sampledata.unemployment

    val colors: List[Color] = List("#F1EEF6", "#D4B9DA", "#C994C7", "#DF65B0", "#DD1C77", "#980043")

    val county_colors = us_counties
        .keys
        .toList
        .map(unemployment.get)
        .map {
            case Some(rate) => colors(math.min(rate/2 toInt, 5))
            case None => Color.Black
        }

    val state_source = new ColumnDataSource()
        .addColumn('state_xs, us_states.values.map(_.lons).toArray)
        .addColumn('state_ys, us_states.values.map(_.lats).toArray)

    val county_source = new ColumnDataSource()
        .addColumn('county_xs, us_counties.values.map(_.lons).toArray)
        .addColumn('county_ys, us_counties.values.map(_.lats).toArray)
        .addColumn('county_colors, county_colors)

    val xdr = new DataRange1d().sources(state_source.columns('state_xs) :: Nil)
    val ydr = new DataRange1d().sources(state_source.columns('state_ys) :: Nil)

    val county_patches = new Patches()
        .xs('county_xs)
        .ys('county_ys)
        .fill_color('county_colors)
        .fill_alpha(0.7)
        .line_color(Color.White)
        .line_width(0.5)

    val state_patches = new Patches()
        .xs('state_xs)
        .ys('state_ys)
        .fill_alpha(0.0)
        .line_color("#884444")
        .line_width(2)

    val county_renderer = new Glyph()
        .data_source(county_source)
        .xdata_range(xdr)
        .ydata_range(ydr)
        .glyph(county_patches)

    val state_renderer = new Glyph()
        .data_source(state_source)
        .xdata_range(xdr)
        .ydata_range(ydr)
        .glyph(state_patches)

    val plot = new Plot()
        .x_range(xdr)
        .y_range(ydr)
        .data_sources(List(state_source, county_source))
        .border_fill(Color.White)
        .title("2009 Unemployment Data")
        .width(1300)
        .height(800)

    val resizetool = new ResizeTool().plot(plot)

    plot.renderers := List(county_renderer, state_renderer)
    plot.tools := List(resizetool)

    val session = new HTMLFileSession("choropleth.html")
    session.save(plot)
    println(s"Wrote ${session.file}. Open ${session.url} in a web browser.")
}
