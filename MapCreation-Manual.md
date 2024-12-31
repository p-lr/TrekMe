# Manual map creation

**Summary**

1. [Introduction](#TOC-Intro)
2. [Create a map](#TOC-Create-a-map)
  * [Image tiling](#TOC-Image-tiling)
    * [Libvips installation](#TOC-Libvips)
    * [Using Libvips](#TOC-Using-Libvips)
  * [Configure the map](#TOC-Configure-the-map)
  * [Import](#TOC-Import)
  * [Map calibration](#TOC-Calibration)
3. [Supported projections](#TOC-Supported_projections)

# <a name="TOC-Intro"></a>Introduction

Manual map creation is required when no suitable map provider can produce the desired map, or we have
a file that we want to put inside TrekAdvisor.

TrekAdvisor works with maps that you put on your SD card in the "trekadvisor" folder on the devices's 
external storage (SD card). The format of the map is discussed later.


## <a name="TOC-Create-a-map"></a>Create a map

TrekAdvisor can load maps that were originally (huge) files. As no device is able to show a huge
image without running out of memory, it is cut into tiles and only the visible subset of them are
displayed.

The process of tiling a huge image can be resources demanding, and may require quite some time on
an android device. Not to mention that the original file would have to fit in memory.
To reduce the time needed for map preparation and have less limitations, that part is made on a computer.
Then, when the image is tiled, the map is (almost) ready to be put on the phone. 
The next section is the recommended way to do this.

### <a name="TOC-Image-tiling"></a>Image tiling

An excellent tool for image processing, and by extension image tiling, is [Libvips](https://github.com/jcupitt/libvips).
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

### <a name="TOC-Import-the-map"></a>Import the map on your device

Once the map is tiled and the configuration file created, we just have to put it on the device,
under the <b>trekadvisor</b> folder.<br>
However, transfering a folder containing numerous little files may be (very) slow. This is why
it is recommended to create a zip archive from the produced "output" folder, and put it in
<b>trekadvisor</b> folder. That will be much faster.

Then, launch TrekAdvisor and open the Import menu. You should see the zip file in the list. Just
press the "Import" button, and at the end of the process the map will appear in the list of available
maps.

Imported maps are extracted in the same directory the of the zip file. To avoid accidental overrides of
existing maps, it is recommended to place zip files in a subdirectory under the "trekadvisor" folder.
You can give this subdirectory the name you want.

Once a map is imported, it can be viewed by selecting it in the "Map Choice" panel of the app. But,
it's not yet calibrated (sadly, that part can't be automated). See next section.


### <a name="TOC-Calibration"></a>Map calibration

The last step. A lot of effort has been made to simplify this. The map is on the device, it can be
displayed, but TrekAdvisor needs to know at least two calibration points for the location functionality
to work.<br>
In TrekAdvisor, under the list of maps, a settings button for each map gives access to the calibration, 
among other things.

For example, if you only know the latitude and longitude of two points and you have no idea what projection
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
if needed. Again, don't forget to save your changes.


## <a name="TOC-Supported_projections"></a>Supported projections


1. Popular Visualisation Pseudo-Mercator (EPSG:3856)

   This projection is very popular as it is used by Google for their projected CRS EPSG:3857, used
   in Google Map for example.

2. Universal Transverse Mercator (UTM).

   It can be used with setting, for example, the projection in map.json (advanced usage only):

   ```
   "projection": {
     "projection_name": "Universal Transverse Mercator",
     "zone": 14,
     "hemisphere": "N"
   }
   ```

   Possible values for "hemisphere" are "N" or "S".
   Possible values for "zone" are 1 to 60.