<p align="center">
<img src="logo/app_name.png"/>
</p>

1. [Overview](#TOC-Overview)
2. [Features summary](#TOC-Features-sum)
3. [Create a map](#TOC-Create-a-map)
  * [Select an area](#TOC-Select-area)
  * [From an archive](#TOC-Import-from-archive)
  * [Manual map creation](#TOC-The-hard-way)
4. [Features](#TOC-Features)
  * [Measure a distance](#TOC-Measure-distance)
  * [Show the speed](#TOC-Show-speed)
  * [Add markers](#TOC-Add-markers)
  * [Add a landmark](#TOC-Add-landmarks)
  * [Import a GPX track](#TOC-GPX-track-import)
  * [GPX recording](#TOC-GPX-recording)


## <a name="TOC-Overview"></a>Overview

TrekMe is an Android app to get live position on a map and other useful information, without
ever needing an internet connection (except when creating a map).
It differs from other map apps in the source of maps.
Usually, a map is downloaded from the internet and is sometimes cached for an offline access (e.g google map).
TrekMe is designed for people willing to use particular maps like USGS in USA, or IGN in France for example.
You can also use your own map if you possess a paper map and you want to use it for your next trek.
In this case, just scan it, then prepare and view it in TrekMe (see below). It also works if you
possess a huge single file (several Gb), as TrekMe is designed to work with arbitrary map size.
The only limitation is the memory available on your device (smartphone, tablet).

## <a name="TOC-Features-sum"></a>Features summary

* Support in-app map creation from:
	- United States's USGS
	- France IGN (requires a ** free ** registration)
 	- Spain IGN 
 	- OpenStreetMap
* Marker support (with optional comments)
* GPX tracks import
* Lock the view to the current position
* Orientation indicator
* Speed indicator
* Distance indicator
* GPX track recording

## <a name="TOC-Create-a-map"></a>Create a map

There are three ways to create a map:
1. Select an area from an official source provider like IGN or USGS
2. Import from an archive
3. Make it yourself (the hard way)

The preferred and easiest way is the first one. Below are detailed each methods.

### <a name="TOC-Select-area"></a>Select an area

In this mode, you use a specific map provider. Google map is a well known example of map provider. 
But their maps aren't ideal for hiking. When possible, it is better to use maps with more terrain
details. 

For example, France's IGN is ideal when you are in France and its territories (Guadeloupe, Martinique,
Réunion, Tahiti, etc.). There is also USGS for the USA. But not all countries have similar service, 
so sometimes you will have to fallback to OpenStreetMap or Google map.

Some providers require you to subscribe to download their maps. This step is free for individuals, 
unless you have specific needs like heavy loads for your organization.

From the "Create map" option menu, you get to choose between available providers:

<p align="center">
<img src="doc/tuto/wmts-providers.jpg" width="300">
</p>

Except for France's IGN, for which a (free) subscription is required, you can directly select the
provider and continue.

<p align="center">
<img src="doc/tuto/select-area.jpg" width="300">
</p>

From there, you can zoom into the area in the world you want to capture. Then, press the area button
so an erea of selection appears on the screen. This area can be resized. Beware that USGS only provides
detailed levels for USA, the same way that IGN details France and its territories. OpenStreetMap covers
the entire world.

When you're done, press the download button, at the bottom right. A configuration menu pops up:

<p align="center">
<img src="doc/tuto/map-configuration.jpg" width="300">
</p>

WMTS map providers have different zoom levels, generally from 1 to 18. In most cases, you don't want
levels 1 to 10 for your hike, and level 18 is not always necessary. This is why the default presets
have the minimum and maximum levels to 12 and 17 respectively. 

The number of tiles that will be downloaded depends on your choice of minimum and maximum levels.
The lower the minimum zoom, and the higher the maximum zoom, the greater is number of tiles. This
is indicated by the "number of transactions". Downloading thousands of transactions may take hours..
so choose carefully your area and levels to only download the tiles you actually need.

Finally, press the download button. A download service is launched and you get a notification. From
the notification center of your Android device, you can either:

* See the download progression
* Cancel the download

When the service finishes the download, you get a notification and a new map is available in the map
list. It is already calibrated and ready to be used. You can set a presentation image so you can
easily identify it in the map list. To do so, press the edit button at the bottom left of the map
card (in the map list menu). 

From the map configuration view, you can:

* Change the thumbnail image
* Change the projection (only if you know what you're doing)
* Change the calibration points (only if you know what you're doing)
* Change the name
* Delete the map


### <a name="TOC-Import-from-archive"></a>Import from an archive

In this mode you use an archive made from an existing TrekMe map. The archive can be made by
yourself or someone else.
A map can be archived from the map list menu, using the save button at the bottom right corner:

<p align="center">
<img src="doc/tuto/bali.jpg" width="300">
</p>

This creates a zip file (which we call archive) inside the `trekme/archives` folder of your device's SD card.
To use someone else's archive:
1. Copy the zip file inside the `trekme` folder or any of its subdirectories
2. Menu > Import
3. Click the import button of the archive of your choice

This archive feature can also be used for backup purposes, as everything related to the map is saved
(calibration, routes, points of interest, etc.).

### <a name="TOC-The-hard-way"></a>Manual map creation - the hard way

In this mode, basic understanding of localisation on a map is required. However, efforts are made to make this app as easy
to use as possible.

It is advised to be familiar with the following terms:
[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890).

Beginners are strongly advised to read the Technical Basis from the [User Guide](UserGuide.md).

Then, proceed with the [Manual map creation guide](MapCreation-Manual.md).

   
## <a name="TOC-Features"></a>Features

### <a name="TOC-Measure-distance"></a>Measure a distance

This is an option from the top-right menu while viewing a map.
Adjust the distance by dragging two blue circles. This is a "as the crow flies" distance.

<p align="center">
<img src="doc/tuto/distance.jpg" width="300">
</p>

### <a name="TOC-Show-speed"></a>Show the speed

The speed indicator overlays the speed in km/h at the top of the screen. Note that it requires a few seconds before the speed can be displayed.

<p align="center">
<img src="doc/tuto/menu-map-view-highlight.jpg" width="300">
</p>
Then choose "Show the speed". If your screen is large enough, there is an icon to directly access it.

### <a name="TOC-Add-markers"></a>Add markers

Press the marker button to add a new marker at the center of the screen:

<p align="center">
<img src="doc/tuto/new-marker.jpg" width="300">
</p>

With its reds arrows turning around it, its shows that it can be moved by dragging the blue circle.
When you're satisfied with its position, tap on the red circle at the center. It then morphs to its static form.

Tapping a marker displays a popup:

<p align="center">
<img src="doc/tuto/marker-popup2.jpg" width="300">
</p>

From here you can:

* Edit the marker (change its name and set a comment, see below)
* Delete it
* Move it

Here is the marker edition view:

<p align="center">
<img src="doc/tuto/marker-edit.jpg" width="300">
</p>

Nothing is changed until you save your changes.

### <a name="TOC-Add-landmarks"></a>Add landmarks

A landmark is a specific marker. A purple line is draw between it and your current position. So it helps when you need to always know the direction of a specific place, which may be outside of the area that your screen covers.

To add a landmark, it is the same logic as for markers. But this time we use the lighthouse icon.

<p align="center">
<img src="doc/tuto/landmark-1.jpg" width="300">
</p>

Often, we want to display our orientation in the same time. We can also add several landmarks:

<p align="center">
<img src="doc/tuto/landmark-2.jpg" width="300">
</p>

### <a name="TOC-GPX-track-import"></a>Import a GPX track

While viewing a map, press the button below on the upper right corner :

<p align="center">
<img src="doc/tuto/manage-tracks.jpg" width="300">
</p>

Then, a list of currently available tracks opens up: 

<p align="center">
<img src="doc/tuto/track-list.jpg" width="300">
</p>

Here you can:

* Import a new gpx files using the import button
* Manage track visibility
* Remove tracks by swiping them left or right

### <a name="TOC-GPX-recording"></a>GPX recording

It is possible to record your position and create a GPX file, to later import into a map or share
with other people.

From the "GPX Record" option menu, you get the following interface:

<p align="center">
<img src="doc/tuto/gpx-recording.jpg" width="300">
</p>

The recording can be started or stopped from the control panel. When recording, the location service
runs in the background. It continues even if TrekMe is stopped or paused.
An indicator reports back the status of the location service. A panel shows the list of recordings.

From there, when selecting a track, there are two buttons at the bottom which enable you to :

* rename it
* import it into an existing map (press the import button at the bottom of the list, then choose the map)

To delete a recording, long-press on it. The panel transitions into a selection mode, where there is a
remove button at the bottom. To go back to selection mode, long-press again somewhere in the list.
