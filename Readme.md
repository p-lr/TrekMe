# TrekAdvisor

1. [Overview](#TOC-Overview)
2. [TrekAdvisor Users](#TOC-TrekAdvisor-Users)
3. [How does it work?](#TOC-How-does-it-work)
4. [Create a map](#TOC-Create-a-map)
  * [Image tiling](#TOC-Image-tiling)
    * [Libvips installation](#TOC-Libvips)
    * [Using Libvips](#TOC-Using-Libvips)
  * [Configure the map](#TOC-Configure-the-map)
  * [Import](#TOC-Import)
  * [Map calibration](#TOC-Calibration)
5. [Supported projections](#TOC-Supported_projections)


## <a name="TOC-Overview"></a>Overview

TrekAdvisor is an Android app to get live position on a map and other useful informations, without
ever needing an internet connection.
It differs from other map apps in the source of maps.
Usually, a map is downloaded from the internet and is sometimes cached for an offline access (e.g google map).
TrekAdvisor is designed for people willing to use <b>their own</b> map. If you
possess a paper map and you want to use it for your next trek, just scan it,
prepare and view it in TrekAdvisor. It also works if you possess a map as a huge single file (several Gb),
as TrekAdvisor is designed to work with arbitrary map size. The only limitation
is the memory available on your device (smartphone, tablet).

## <a name="TOC-TrekAdvisor-Users"></a>TrekAdvisor Users

TrekAdvisor is designed for people with advanced understanding of
localisation on a map. However, efforts are made to make this app as easy
to use as possible.

Without any cartographic knowledge, e.g if terms of
[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890) don't
mean anything to you, using TrekAdvisor may introduce you to the world of cartography.
It is not as complicated as it looks.

Beginners are strongly advised to read the Technical Basis from the [User Guide](UserGuide.md).


## <a name="TOC-How-does-it-work"></a>How does it work?

TrekAdvisor works with maps that you put on your SD card in the "trekadvisor" folder on the devices's 
external storage (SD card). The format of tha map is discussed later.
For instance, that step is manual so you have to do the copy/paste of the map on your phone yourself.
Hopefully this will be simplified on a future version, so a map located on your computer would be
imported from the app.

## <a name="TOC-Create-a-map"></a>Create a map

TrekAdvisor was first designed to work with maps that was originally (huge) files. But no
device is able to show a huge image without running out of memory. That's why the image has to be
cut into tiles, so only a small subset of them are displayed (to only show the visble part of the map on
the screen).
The process of tiling a huge image can be ressources demanding, and may require quite some time on
an android device. Not to mention that the original file would have to fit in memory.
To reduce the time needed for map preparation and have less limitations, that part is made on a computer.
Then, when the image is tiled, the map is (almost) ready to be put on the phone. 
The next section is the recommended way to do this.

### <a name="TOC-Image-tiling"></a>Image tiling

An excellent tool for image processing, and by extention image tiling, is [Libvips](https://github.com/jcupitt/libvips).
Actually, TrekAdvisor only supports maps produced with that tool (for instance, but that may change in the future).

#### <a name="TOC-Libvips"></a>Libvips installation

Libvips is supported on Linux and Windows. It can be used from the command line (the most easy way).

* For Linux

  Libvips is packaged in most distributions. For example, on Ubuntu it can be installed with :
  ```
  sudo apt install libvips
  ```
* For Windows

  Libvips binaries can be downloaded from [here](http://www.vips.ecs.soton.ac.uk/supported/current/win32/).
  The executables are under the "bin" folder of the extracted archive.

#### <a name="TOC-Using-Libvips"></a>Using Libvips

If we want to tile the image named big_image.png :

* On Linux

  ```
  vips dzsave big_image.png output --layout=google --tile-size=256 --suffix .jpg --vips-progress
  ```

* On Windows

  For convenience, you can execute `vips.exe` in the image's parent directory. You can right-click
  in the folder while holding the left Shift keystroke, then "Execute a command here".

  ```
  vips.exe dzsave big_image.png output --layout=google --tile-size=256 --suffix .jpg --vips-progress
  ```
In that command, you can specify :
* the size of tiles, with the option `--tile-size`
* the quality and format of the tiles. For example, using `.jpg[Q=90]` will increase the quality but
also the size in MB of map.

At the end of the tiling process, a folder "output" is created. You can rename it to whatever name
you like. That folder contains several subfolders that are number-named (0, 1, 2, etc). Each subfolder
corresponds to a level of the map. This is important for the next step.

### <a name="TOC-Configure-the-map"></a>Configure the map

This is a very important step. We add a file into the previously produced "output" folder that will
contain every informations that TrekAdvisor needs to display the map.
That file must be named `map.json`. The easiest way is to copy-paste this [configuration
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
  the file consistency or the map won't show up in TrekAdvisor.
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

### <a name="TOC-Import"></a>Import

Once the map is tiled and the configuration file created, we just have to put it on the device,
under the <b>trekadvisor</b> folder.<br>
However, transfering a folder containing numerous little files may be (very) slow. This is why
it is recommended to create a zip archive from the produced "output" folder, and put it in
<b>trekadvisor</b> folder. That will be much faster.

Then, launch TrekAdvisor and open the Import menu. You should see the zip file in the list. Just
press the "Unzip" button, and at the end of the process the map will appear in the list of available
maps.

Unziped maps are automatically put in a subfolder named "imported" under the "trekadvisor"
directory. This is to avoid accidental overrides of existing maps. Indeed, to be sure a map folder
will never be overriden by an imported map, move this map folder to another subfolder under
"trekadvisor". A lot of Android apps are meant for file manipulation like this.

### <a name="TOC-Calibration"></a>Map calibration

The last step. A lot of effort have been made to make it easy. The map is on the device, it can be
displayed, but TrekAdvisor needs to know at least two calibration points for the location functionnality
to work.<br>
In TrekAdvisor, under the list of maps, a settings button for each map gives access to the calibration fragment, 
among other things.

For example, if you only know the latitude and longitude for two points and you have no idea what projection
was used to make this map, just set the projection to "None", the number of calibration points to 2 (the
default), and press the "Define calibration points" button to access the calibration screen.

The map will appear with a reticule on it, with an interface to select the calibration point currently
edited. Drag the reticule to the exact position of the first calibration point and enter the values of
latitude and longitude in <b>decimal degrees</b>. You can enter negative values.<br>
Beware not to confuse between latitude and longitude. When you are satisfied with the values, save your
changes and hit the second calibration point button and repeat the procedure.

Don't forget to save your changes!

Once the two calibration points are defined <b>and</b> saved, the map is ready to use (go back to the map
list and open the calibrated map).

PS : for best accuracy, it is advised to zoom the map during the calibration process before positioning
the reticule. You can edit the calibration of a map at any time and fix the position of each calibration
if needed. Don't forget to save your changes.


## <a name="TOC-Supported_projections"></a>Supported projections


1. Popular Visualisation Pseudo-Mercator (EPSG:3856)

   This projection is very popular as it is used by Google for their projected CRS EPSG:3857, used
   in Google Map for example.

2. Universal Transverse Mercator (UTM).

   It can be used with setting, for example, the projection in map.json :

   ```
   "projection": {
     "projection_name": "Universal Transverse Mercator",
     "zone": 14,
     "hemisphere": "N"
   }
   ```

   Possible values for "hemisphere" are "N" or "S".
   Possible values for "zone" are 1 to 60.



