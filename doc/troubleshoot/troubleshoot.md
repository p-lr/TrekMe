# What should I do if...

* [I can't get my position on the map](#i-cant-get-my-position-on-the-map)
* [As I'm creating a new map, a message tells me to check my internet connection](#as-im-creating-a-new-map-a-message-tells-me-to-check-my-internet-connection)
* [My GPX recording sometimes stops by itself](#my-gpx-recording-sometimes-stops-by-itself)
* [Straight lines appear on my GPX recording](#straight-lines-appear-on-my-gpx-recording)
* [I have another issue](#i-have-another-issue)


## I can't get my position on the map

There are several possible causes for this issue. Please follow those checks:

1. Are you in a location where the GPS signal can be received?\
Closed environments like houses, flats, etc. make it difficult to get the GPS signal (remember, it
comes from satellites).

2. Have you authorized TrekMe to access your location?\
You can check that in your Android settings, for the TrekMe app. Authorizations are listed, and
"Location" should be checked. 

3. Is the location enabled on your device?\
While all necessary authorizations are granted to TrekMe, the location can be disabled on the device,
which prevents all apps (including TrekMe) to access your location.

## As I'm creating a new map, a message tells me to check my internet connection

While TrekMe is meant for hiking while being offline, you need to have an internet access when 
creating a new map. If you can confirm that your internet connection works fine, then it might be an
issue with map providers - it happens sometimes. In the latter case, it is advised to wait a few
minutes before making another attempt.

## My GPX recording sometimes stops by itself

A GPX recording is actually an Android service which runs in the background. However, some devices
put a hard limit on the number of background services allowed to run.
It's not uncommon for Android to abruptly stop a service, if it considers that stopping it would 
extend battery life. In the case of a GPX recording, it's reasonable to consume a bit of power while
you're aware of it.

To fix this issue, you have to go to your device settings -> "Battery". Then, you should see a menu
"Battery optimization" or similar. You should see a list of applications. Scroll down to find TrekMe
and disable the battery optimization.

## Straight lines appear on my GPX recording

This problem can have two root causes:

1. The battery optimization is active for TrekMe. Follow instructions for [My GPX recording sometimes stops by itself](#my-gpx-recording-sometimes-stops-by-itself).

2. The location permission isn't set to "Allow all the time". Allowing the location just when using 
the app isn't enough.

## I have another issue

If your issue isn't listed above, contact me at plr.devs@gmail.com

I'll do my best to help you.



