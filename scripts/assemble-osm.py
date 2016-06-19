#!/usr/bin/env python

"""
This script uses the Python bindings of libvips to assemble tiles tp produce a resulting single
output image 'out.jpg'.
It works with Openstreetmap (osm) tiles pattern, downloaded with a tool taken from the osmdroid
project : osmdroid-packager-5.2-jar-with-dependencies.jar
This jar is available in the release section : https://github.com/peterLaurence/TrekAdvisor/releases/tag/v1.0.0
Or a newer version may be available on the osmdroid repository.
"""

import os
import sys

import gi
gi.require_version('Vips', '8.0')
from gi.repository import Vips

Vips.progress_set(True)

currentDir = os.getcwd()
dirs = []
for filename in os.listdir(currentDir):
	path = os.path.join(currentDir, filename)
	if not os.path.isfile(path):
		dirs.append(filename)

dirs = sorted(dirs)

imageNames = []
for filename in os.listdir(os.path.join(currentDir, dirs[0])):
	imageNames.append(filename)

imageNames = sorted(imageNames)
print imageNames

allImages = []
for image in imageNames:
	for folder in dirs:
		allImages.append(Vips.Image.new_from_file(os.path.join(currentDir, folder + "/" + image),
												  access = Vips.Access.SEQUENTIAL_UNBUFFERED))

join = Vips.Image.arrayjoin(allImages, across = len(dirs))
join.write_to_file("out.jpg")