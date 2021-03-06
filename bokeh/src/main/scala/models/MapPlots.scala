package io.continuum.bokeh

@model abstract class MapOptions extends HasFields {
    object lat extends Field[Double]
    object lng extends Field[Double]
    object zoom extends Field[Int](12)
}

@model abstract class MapPlot extends Plot {
    // TODO: object map_options extends Field[MapOptions]
    // and later override this in subclasses
}

@model class GMapOptions extends MapOptions {
    object map_type extends Field[MapType]
    object styles extends Field[Js.Arr]
}

@model class GMapPlot extends MapPlot {
    object map_options extends Field[GMapOptions]
    // TODO: border_fill_color = Override(default="#ffffff")
}
