![Logo](Logo/horizontal.png)

1. [Overview](#TOC-Overview)
2. [Features summary](#TOC-Features-sum)
3. [Create a map](#TOC-Create-a-map)
  * [Select an area](#TOC-Select-area)
  * [From an archive](#TOC-Import-from-archive)
  * [Manual map creation](#TOC-The-hard-way)
4. [Features](#TOC-Features)
  * [Import a GPX track](#TOC-GPX-track-import)


## <a name="TOC-Overview"></a>Overview

TrekAdvisor is an Android app to get live position on a map and other useful information, without
ever needing an internet connection.
It differs from other map apps in the source of maps.
Usually, a map is downloaded from the internet and is sometimes cached for an offline access (e.g google map).
TrekAdvisor is designed for people willing to use particular maps like USGS in USA, or IGN in France for example.
You can also use your own map if you possess a paper map and you want to use it for your next trek.
In this case, just scan it, then prepare and view it in TrekAdvisor (see below). It also works if you
possess a huge single file (several Gb), as TrekAdvisor is designed to work with arbitrary map size.
The only limitation is the memory available on your device (smartphone, tablet).

## <a name="TOC-Features-sum"></a>Features summary

* Support in-app map creation from United States's USGS, France's IGN map servers, and OpenStreetMap.
* Marker support (with optional comments)
* GPX tracks import
* Lock the view to the current position
* Orientation indicator
* Speed indicator
* Distance indicator
* GPX track recording

### On TODO list

* Publish the app on Google Play.

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

[TODO: link here a guide]()

### <a name="TOC-Import-from-archive"></a>Import from an archive

In this mode you use an archive made from an existing TrekAdvisor map. The archive can be made by
yourself or someone else.
A map can be archived from the map list menu, using the save button at the bottom right corner:

<p align="center">
<img src="https://user-images.githubusercontent.com/15638794/43673418-3522e7da-97c3-11e8-8173-0fecd1d02fbd.jpg">
</p>

This creates a zip file (which we call archive) inside the `trekadvisor/archives` folder of your device's SD card.
To use someone else's archive:
1. Copy the zip file inside the `trekadvisor` folder or any of its subdirectories
2. Menu > Import
3. Click the import button of the archive of your choice

This archive feature can also be used for backup purposes, as everything related to the map is saved
(calibration, routes, points of interest, etc.).

### <a name="TOC-The-hard-way"></a>Manual map creation - the hard way

In this mode, basic understanding of localisation on a map is required. However, efforts are made to make this app as easy
to use as possible.

Without any cartographic knowledge, e.g if terms of
[Map projection](https://en.wikipedia.org/wiki/Map_projection),
[WGS84](https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84),
[Mercator](https://en.wikipedia.org/wiki/Mercator_projection?oldid=9506890) don't
mean anything to you, using TrekAdvisor may introduce you to the world of cartography.

Beginners are strongly advised to read the Technical Basis from the [User Guide](UserGuide.md).

Then, proceed with the [Manual map creation guide](MapCreation-Manual.md).

   
## <a name="TOC-Features"></a>Features

### <a name="TOC-GPX-track-import"></a>Import a GPX track

While viewing a map, select the option menu on the upper right corner :

<p align="center">
<img src="https://user-images.githubusercontent.com/15638794/27984165-a9ee2658-63ce-11e7-9ae5-c64b3e750ce4.png">
</p>

Then, a list of currently available tracks opens up. There is a button to import a new track from a 
.gpx file :

<p align="center">
<img src="https://user-images.githubusercontent.com/15638794/27984164-a9eb421c-63ce-11e7-97c6-b11a8dbdcd79.png">
</p>


