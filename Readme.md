<p align="center">
<img src="logo/app_name.png"/>
</p>

<p align="center">
<img src="doc/images/map-list-en.jpg" width="200"> &nbsp&nbsp&nbsp <img src="doc/images/trekme-example-en.jpg" width="200">
</p>

## Summary

1. [Overview](#overview)
2. [Features summary](#features-summary)
3. [Create a map](#create-a-map)
  * [Select an area](#select-an-area)
  * [From an archive](#import-from-an-archive)
  * [Receive a map](#map-sharing)
  * [Manual map creation](#manual-map-creation---the-hard-way)
4. [Features](#features)
  * [Measure a distance](#measure-a-distance)
  * [Show the speed](#show-the-speed)
  * [Add markers](#add-markers)
  * [Add a landmark](#add-landmarks)
  * [Lock the view on current position](#lock-the-view-on-current-position)
  * [Visualize a recording in real time](#visualize-a-recording-in-real-time)
  * [Import a GPX track](#import-a-gpx-track)
  * [GPX recording](#gpx-recording)
  * [Follow a track](#follow-a-track)
5. [Settings](#settings)
  * [Start on the last map](#start-on-the-last-map)
  * [Download folder](#download-folder)
  * [Rotation mode](#rotation-mode)
6. [Save your maps](#save-your-maps)
7. [Share your maps](#map-sharing)
8. [What should I do when...](doc/troubleshoot/troubleshoot.md)


## Overview

TrekMe is an Android trekking app to get live position on a map and other useful information, without
ever needing an internet connection (except when creating a map).
TrekMe is designed to work with any WMTS source such as USGS in USA, IGN France, Swiss topo, 
OpenStreetMap and so on. 
You can download an area of your choice so the cached tiles are available for offline usage.

Perhaps most importantly, TrekMe is _designed_ to consume few CPU resources, to preserve the device's battery. 

## Features summary

* Support in-app map creation from:
	- United States's USGS
	- France IGN (requires a subscription)
 	- Spain IGN 
 	- Swiss Topo
 	- OpenStreetMap
* Marker support (with optional comments, and photos)
* GPX tracks import
* Lock the view to the current position
* Orientation indicator
* Speed indicator
* Distance indicator
* GPX track recording

Some features require a premium subscription, such as:

* Create an unlimited number of folders to organize your tracks
* Remove the limit of map size
* Add beacons to be alerted when getting close to specific locations
* … and more

## Create a map

There are three ways to create a map:

1. Select an area from an official source provider like IGN or USGS,
2. Import from an archive,
3. Receive a map from a nearby TrekMe user (over Wifi)

The preferred and easiest way is the first one. Below are detailed each of these methods.

### Select an area

In this mode, you use a specific map provider. Google map is a well known example of map provider. 
But their maps aren't ideal for hiking. When possible, it is better to use maps with more terrain
details. 

For example, USGS is USA's official map provider. France's IGN is ideal when you are in France and 
its territories. However, not all countries have similar service.
OpenStreetMap has a worldwide coverage. In particular, OpenStreetMap HD has a much better quality. 

Some providers require a subscription to download their maps. The others are free for maps of 
reasonable size.

From the "Create map" option menu, you get to choose between available providers:

<p align="center">
<img src="doc/images/wmts-providers.jpg" width="300">
</p>

When you've made your choice, the map appears shortly. Beware that USGS only provides detailed 
levels for USA. Actually, other providers only cover their relevant country, except for 
OpenStreetMap which covers the entire world.

From there, you can zoom into the area in the world you want to capture. If this isn't the most handy
way to find your area of interest, there are others ways:

- You can center on your current location, using the location button at the top.
- You can search for a particular place, using the search button in the top bar.
- Import a GPX track, using the menu at top-right corner. This feature is available as part of the 
Plus and Pro offer.

When you found the place you're looking for, you may adjust the position of the blue circles, which
define the area to download. When you're ready, press the "Validate" button at the bottom of the screen.
You then see the menu below, from where you will start the download.

<p align="center">
<img src="doc/images/map-configuration-en.jpg" width="300">
</p>

WMTS map providers have different zoom levels, generally from 1 to 18. In most cases, you don't want
levels 1 to 10 for your hike, and level 17 is not always necessary. Unless you know what you're 
doing, it's advised to keep the default level presets.

The number of tiles that will be downloaded depends on the size of the area, and on minimum and 
maximum levels. So the simplest thing to do is to adjust the area to download.
The number of tiles increases when the minimum zoom is low and the maximum zoom is high. This
is indicated by the estimated size in Mb. Downloading hundreds of Mb may take hours..
so choose carefully your area to only download the tiles you actually need.

Finally, press the download button. A download service is launched and you get a notification. From
the notification center of your Android device, you can either:

* See the download progression,
* Cancel the download

When the service finishes the download, you get a notification and a new map is available in the map
list. You can set a presentation image so you can easily identify it in the map list. To do so, 
press the edit button at the bottom left of the map card (in the map list menu). 

From the map configuration view, you can:

* Change the thumbnail image,
* Change the name,
* Save the map

#### Resume a stopped download

When a map download was stopped (either manually or e.g when shutting down the device), the map is
now incomplete. You can tell that by the warning bellow:

<p align="center">
<img src="doc/images/download-aborted.jpg" width="150">
</p>

You can resume the download by using EDIT > "Analyze & repair". The map repair will fetch the 
missing tiles. This is possible if you have TrekMe Plus or Pro. Otherwise, it's advised to delete the 
incomplete map.

### Import from an archive

A map can also be created from an existing archive. The archive can be made by yourself or someone 
else (see below to create an archive). An archive is a zip file.
To import from an archive, use the main menu and choose "Import a map". Then, press the button 
"Import from folder" in the middle of the screen. Navigate to the folder which contains the archive(s)
and select that folder. Then, TrekMe shows you the recognized archives, which you can individually
import.

### Receive a map

See [Map Sharing](#map-sharing).

   
## Features

### Measure a distance

Distance can be measured using two different tools in TrekMe:

*As the crow flies distance*

This is an option from the top-right menu while viewing a map: 
Adjust the distance by dragging two blue circles.

<p align="center">
<img src="doc/images/distance.jpg" width="300">
</p>

*Along track distance*

While following a track, it's sometimes handy to know the distance between two points on that track. 
For example, you can evaluate whether you have enough time to reach some point, then go around before
 nightfall.

This is an option from the top-right menu while viewing a map: "Distance on track". It can be 
activated/disabled. When enabled, two blue circles appear on the nearest track from the center of the
screen. The portion of the track between the two blue circles is highlighted in red, and its 
distance is shown.

<p align="center">
<img src="doc/images/dist-on-track.jpg" width="300">
</p>

The distance accounts for elevation *only if* the track contains elevation data for each point.

### Show the speed

The speed indicator overlays the speed in km/h at the top of the screen. Note that it requires a few 
seconds before the speed can be displayed.

<p align="center">
<img src="doc/images/speed.jpg" width="300">
</p>

### Add markers

Press the marker button to add a new marker at the center of the screen:

<p align="center">
<img src="doc/images/new-marker.jpg" width="300">
</p>

The marker can be moved by dragging the blue circle. When you're satisfied with its position, tap 
once anywhere on the blue circle.

Tapping a marker displays a popup:

<p align="center">
<img src="doc/images/marker-popup-en.jpg" width="300">
</p>

From here you can:

* Navigate to the marker using Google Maps (top-right icon),
* Edit the marker (change its name, add a comment or a photo),
* Move it,
* Delete it

### Add landmarks

A landmark is a specific marker. A purple line is drawn between it and your current position. So it helps when you need to always know the direction of a specific place, which may be outside of the area that your screen covers.

<p align="center">
<img src="doc/images/landmark-1.jpg" width="300">
</p>

Often, we want to display our orientation in the same time. We can also add several landmarks:

<p align="center">
<img src="doc/images/landmark-2.jpg" width="300">
</p>

### Lock the view on current position

Sometimes, you want the view to automatically follow your position. To do that, use the menu as shown bellow:

<p align="center">
<img src="doc/images/menu-map-view-highlight.jpg" width="300">
</p>

Then select "Lock on position". Now, everytime the application gets a location update (approximately 
every 2 seconds, up to 5 seconds), the view centers on this new location.

### Visualize a recording in real time

When you start a recording from the "GPX Record" option menu, the recording can be seen in real time 
on any map that covers your current area. It appears as an orange route.

Even if you close TrekMe, you will find your live route the next time you open it, until you stop the recording.

<p align="center">
<img src="doc/images/live-route.jpg" width="300">
</p>

### Import a GPX track

There's two different ways for importing a gpx track. Using the first one, you import a track for a
specific map, while using the second one you import a track for all maps which can display the track.

#### Import gpx for a specific map

While viewing a map, press the menu button at the upper right corner, then select "Manage tracks".
You land on the track manage screen. From there, press the the menu button at the upper right corner,
then select "Import GPX file".

#### Import gpx for all maps

From the main menu > My trails, click on the main button at the bottom right of the screen, then
select "Import GPX files".

You can then select the file(s) you wish to import. The track(s) will then be imported for all maps
which can display the track(s).

### GPX recording

It is possible to record your position and create a GPX file, to later import into a map or share
with other people.

From inside any map, there's a button at the top left corner:

<p align="center">
<img src="doc/images/mountainview.jpg" width="300">
</p>

The recording can be started, stopped, or paused. When recording, the location service
runs in the background. It continues to run even if TrekMe is stopped, until you decide to stop it.
If you have Android 10 or above, you need to:

- make sure that the location permission for TrekMe is set to "Allow all the time", and not just when using the app.
- ensure that the battery optimization is disabled for TrekMe 

Otherwise, some points won't be recorded and straight lines will appear on the track.

### Follow a track

Sometimes, we want to use the phone as little as possible. However, we take the risk to take a wrong
path and realize it a bit too late.

To avoid this problem, the track following feature alerts you when you get off the track. The alert
threshold is 50m by default but can be changed in the settings. This feature is only available with
premium offers.

The track following can be started from any map, in the top-right menu:

<p align="center">
<img src="doc/images/track-follow-en.jpg" width="300">
</p>

Then, select the track to follow by pressing on it on the map. The selected track is then highlighted
with a thin black stroke:

<p align="center">
<img src="doc/images/track-follow-selected.jpg" width="300">
</p>

The track following feature runs as a background service, which only works when all below conditions are met:
- Battery optimization is disabled for TrekMe
- Location authorization is set to "allow all the time"
- Location is enabled on the device

## Settings

Settings are accessible from the main menu > Settings.

### Start on the last map

By default, TrekMe starts on the list of maps. But it's possible to start on the last viewed map. 
In the "General" section > "Start TrekMe on"


### Download folder

By default, TrekMe stores everything on the internal memory. But if you have an SD card, **and** if it's mounted as portable device, you can use it to store some of your maps.

**Beware**

All your maps on the SD card will be deleted if TrekMe is uninstalled (Android enforces that).
This is the reason why it's highly recommended to save the maps you wouldn't want to loose.
To learn more about that, go to the "Save your maps" section.

In the "Root folder" section > "Selected folder", you can chose between two directories if you have an SD card. Otherwise, you can only use the internal memory:

<p align="center">
<img src="doc/images/download_dir.jpg" width="300">
</p>

The first directory always corresponds to the internal memory. The second, if available, corresponds to a directory on the SD card. This directory is `Android/data/com.peterlaurence.trekme/downloaded`.

Once the download folder is changed, your next map download will use it. But existing maps aren't moved.

### Rotation mode

Three rotation modes are available:

* No rotation (by default)
* Rotate only when showing orientation
* Free rotation

*Rotate only when showing orientation*

In this mode, the map is rotated along with your device's orientation *if* you enable orientation
display (while displaying a map, in the upper right menu > Show orientation).
When orientation display is enabled, a small compass appears in the bottom right of the screen - the
red side indicates the North. Whenever you like, you can disable orientation display. In this case, 
the map is aligned to the North and the compass disappears. Also note that in this mode, pressing the
compass has no effect. An example:

<p align="center">
<img src="doc/images/rotation-oriented.jpg" width="300">
</p>

*Free rotation*

In this mode, you can rotate the map as will. The compass is always displayed, and pressing it aligns
the map to the North. You can also enable or disable orientation display - it won't have consequences 
on the orientation of the map.

### Save your maps

Starting from Android 10, all maps (no matter if they're on the internal memory or SD card) are
deleted when TrekMe is uninstalled. Consequently, it's highly recommended to use the backup feature
of TrekMe. You'll be able to restore your maps if you e.g change for a new device.

To create an archive, go to the list of maps and show map options with either a long press on a map
or ussing the top right menu > show map options.

In the map options, you'll find a "Save" button. Then, a dialog explains that you're about to choose
the folder in which the archive will be created. You can choose whichever folder you want (or create
a new one), but don't select a subdirectory of TrekMe. If you continue, the archive will be created
in background - you can see the progression in the notification area of the device.

An archive contains everything related to the map (calibration, routes, points of interest, etc.).

Once archived, a map can be restored using the import feature.

### Map sharing

A map is sometimes heavy and takes quite some time to download. When a friend also has TrekMe, it's
possible send him/her one of your maps. This feature requires the Wifi to be enabled on the two devices,
even if there is no router or hotspot in the vicinity - the sender can directly send to the receiving
device over Wifi. Make sure that the two devices stay relatively close to each other. Even if some
loads have completed successfully with several meters between the two devices, experience has shown 
that keeping the two devices close to each other lowers the risk of error.

Go ahead in the main menu > "Receive and send". There's a button to receive and another one to send.
When you choose to receive, the device is awaiting for a connection with the sender. This step can
take several minutes - please be patient. Meanwhile, the sender uses the send button and the map to send.

When the connection is established, the load starts and you can see the progression. If everything goes
well, a gorgeous emoticon is shown. Otherwise, and especially if the load got interrupted, you're advised
to start again (Wifi isn't 100% reliable).

If it really takes too long for devices to establish a connection (more than 5 min), try to restart
the receiving and sending on both phones. As last resort, reboot the two devices and retry the
procedure.

