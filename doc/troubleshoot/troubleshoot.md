# What should I do if...

* [I can't get my position on the map](#TOC-loc)
* [As I'm creating a new map, a message tells me to check my internet connection](#TOC-no-internet)
* [My GPX recording sometimes stops by itself](#TOC-record-gpx-stop)
* [I have another issue](#TOC-other)


## <a name="TOC-loc"></a> I can't get my position on the map

There are several possible causes for this issue. Please follow those checks:

1. Are you in a location where the GPS signal can be received?

  Closed environments like houses, flats, etc. make it difficult to get the GPS signal (remember, it
  comes from satellites).

2. Have you authorized TrekMe to access your location?

  You can check that in your Android settings, for the TrekMe app. Authorizations are listed, and
  "Location" should be checked. 

3. Is the map calibrated
  You can check that in the list of maps. Under the relevant map's name, "Calibrated" should be
  mentioned. 
  After a map download finishes, the map is automatically calibrated. However, if for some reason
  the map isn't calibrated, you have two options left:
  
  * Calibrate the map yourself (a tutorial will be added soon), or
  * Re-download the map

## <a name="TOC-no-internet"></a> As I'm creating a new map, a message tells me to check my internet connection

While TrekMe is meant for hiking while being offline, you need to have an internet access when 
creating a new map. If you can confirm that your internet connection works fine, then it might be an
issue with map providers - it happens sometimes. In the latter case, it is advised to wait a few
minutes before making another attempt.

## <a name="TOC-record-gpx-stop"></a> My GPX recording sometimes stops by itself

A GPX recording is actually an Android service which runs in the background. However, some devices
put a hard limit on the number of background services allowed to run.
It's not uncommon for Android to abruptly stop a service, if it considers that stopping it would 
extend battery life. In the case of a GPX recording, it's reasonable to consume a bit of power while
you're aware of it.

To fix this issue, you have to go to your device settings -> "Battery". Then, you should see a menu
"Battery optimization" or similar. You should see a list of applications. Scroll down to find TrekMe
and disable the battery optimization.

## <a name="TOC-other"></a> I have another issue

If your issue isn't listed above, contact me at plr.devs@gmail.com

I'll do my best to help you.



