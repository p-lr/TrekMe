# TrekMe User Guide

_This document isn't finished yet_

1. [Technical Basis](#TOC-Technical-Basis)
  * [Understand Map Projections](#TOC-Understand-Map-Projection)
  * [Map Calibration](#TOC-Map-Calibration)
    * [What if i don't know the map projection?](#TOC-What-if)
2. [Using TrekMe](#TOC-Using-TrekMe)



## <a name="TOC-Technical-Basis"></a>Technical Basis

Everyone knows about latitude and longitude. This is what we get from any GPS (and,
by extent, smartphones and tablets).
But how do we actually use those to locate ourselves on a map?

### <a name="TOC-Understand-Map-Projection"></a>Understand Map Projections

Every point on the earth has unique latitude and longitude.

[add image illustration here in the future]

These are coordinates of a point on surface of the globe which is approximately a sphere.

A map is obviously not a sphere : it's a flat surface. But a sphere's surface cannot be
flatten without distortion. To understand this, try to wrap a ball inside a sheet of paper
without making any folds : that's impossible. This is why map projections come into place
to represent a portion of a sphere on a flat surface.

So depending of the projection used to build a map, a point of the the earth which
coordinates are (lat, lon) will have a specific coordinates on the map (X, Y). Most often,
the projected coordinates are given as (E, N), where E stands for "Easting" and N is the
"Northing". But to keep it simple, we will keep the (X, Y) notation in this guide.
As the projection is rigorously defined, mathematical formulas exists to convert
from (lat, lon) to (X, Y). Each projection has its own set of mathematical formulas.
Of course, we won't need to know the details nor do any calculations to locate ourselves
on the map. All we need to know is which projection was used to build the map we
have, and TrekMe will do the rest.<p>
Well, almost. There is one more thing we need to do for localization to work. This
bring us to map calibration.

### <a name="TOC-Map-Calibration"></a>Map Calibration

Let's say we have a map built with [Web Mercator](https://en.wikipedia.org/wiki/Web_Mercator)
projection, which is the projection used by Google Map. So when we receive a GPS
update, we get our updated (lat, lon) coordinates. As TrekMe knows the projection
of the map, the corresponding (X, Y) web mercator coordinates are quickly calculated.
But how TrekMe can guess to which point of the map this (X, Y) corresponds to?
We have to know at least two points and tell that the first point, which is e.g
at the top left corner, has coordinates (X0, Y0). Similarly, the second point located
at the bottom right corner has coordinates (X1, Y1).<p>
The key aspect here is that we know (X0, Y0) and (X1, Y1). Alternatively, we may just
know the corresponding latitude and longitude, that is to say (lat0, lon0) and
(lat1, lon1). The later is probably simpler. Either way, TrekMe provides a
way to define calibration points : these are points of the map for which we know
their latitude-longitude or their projected coordinates.

#### <a name="TOC-What-if"></a>What if i don't know the map projection?

That may not be a problem, depending on :
- the actual projection
- the size of the map
- your location on the earth

This needs explanations. If you don't know the projection, TrekMe will use the
latitude and longitude of the defined calibration points as projected coordinates
and thus will perform linear interpolation between those values for each GPS update.
But the mathematical formulas to convert (lat, lon) to (X, Y) are anything but linear.
So the computed position will be incorrect. However, the smaller the map is, the smaller
is the position error. If your map is, e.g a plan of city, the error will be negligible
and you won't even see the difference with a well calibrated map. However, if your map
is quite big, you will probably notice the position error. And this error will grow
the further you are from your calibration points.<p>
And finally, maps built with Web Mercator have distortions around the poles. The closer you
are from the poles, the smaller is the area where the position error is negligible.<p>

## <a name="TOC-Using-TrekMe"></a>Using TrekMe

### <a name="TOC-Configure-the-map"></a>Advanced usage

When it loads, TrekMe recursively looks for configuration files under the "trekadvisor" folder.
While the Readme explains how to import a map using the "Import" capability of the app, it is not
 the only way to achieve this. In fact, if you know the structure of those configuration files, you
 don't have to do that.
This section explains the format of the configuration file. Is has the following requirements :

* It must be placed right under the previously produced "output" folder (map tiling).
* It must be named `map.json`.

The easiest way is to copy-paste this [configuration
file](app/src/main/assets/map-example/map.json) an adapt it.

Important steps :

1. Specify the correct number of levels. For example, a level in json file is represented by :
  ```
  "level": 5,
  "tile_size": {
      "x": 256,
      "y": 256
  }
  ```
  There must be as many levels as there are subfolders in the "output" folder. You may
  have to add or remove some levels in the example json file. Be careful not to alter
  the file consistency or the map won't show up in TrekMe.
  Specify the correct values for the `tile_size` for each level.

2. Specify the size of the map. For example :
  ```
  "size": {
    "x": 12456,
    "y": 23412
  }
  ```
  The values to set are the size in pixels of the original image (the "big_image.png" in the example).
  One way to get it is right-clicking on the image file and read its properties. <br>
  `x` is the width <br>
  `y` is the height

_[TODO : a tutorial with screeshots]_