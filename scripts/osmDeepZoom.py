# -*- coding: utf-8 -*-
"""
This script directly transforms a list of zoom levels of an area, downloaded from OpenStreetMap servers, into a format
usable with TrekAdvisor.

It works with Openstreetmap (osm) tiles pattern, downloaded with a tool taken from the osmdroid
project : osmdroid-packager-5.2-jar-with-dependencies.jar

Example of usage
• First, download tiles

    java -jar osmdroid-packager-5.2-jar-with-dependencies.jar -u http://b.tile.openstreetmap.org/%d/%d/%d.png
     -t Bali-centre -d BaliCentre.zip -zmin 12 -zmax 18 -n -8.0579 -s -8.6245 -e 115.7149 -w 115.0076

  This will produce several folders named 12, 13, ..., 18

• In the parent directory of the produced folder, run the script. You can specify this working directory with the option
  --wd working_directory.
  This will create the map under a 'map' folder (by default). The name can be changed with the option --mapname
"""


import argparse
import errno
import os
import shutil
import math
import re

TILE_SIZE = 256


def get_folders_in_dir(directory):
    list_folder = []
    for filename in os.listdir(directory):
        path = os.path.join(directory, filename)
        if not os.path.isfile(path):
            list_folder.append(filename)
    return list_folder


def num2deg(xtile, ytile, zoom):
    """
    Utility method. Tile numbers to lon./lat.
    Source : http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    """
    n = 2.0 ** zoom
    lon_deg = xtile / n * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * ytile / n)))
    lat_deg = math.degrees(lat_rad)
    return lat_deg, lon_deg


def deg2num(lat_deg, lon_deg, zoom):
    """
    Utility method. Lon./lat. to tile numbers.
    Source : http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    """
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    xtile = (lon_deg + 180.0) / 360.0 * n
    ytile = (1.0 - math.log(math.tan(lat_rad) + (1 / math.cos(lat_rad))) / math.pi) / 2.0 * n
    return xtile, ytile


class Level(object):
    def __init__(self, level, path, shift_row, shift_col):
        self._level = level
        self._path = path
        self._shift_row = int(shift_row)
        self._shift_col = int(shift_col)


def process_shifts(workingDir):
    levels = get_folders_in_dir(workingDir)
    levels = sorted(levels)

    # quick filter
    temp_list = []
    for level in levels:
        try:
            int(level)
            temp_list.append(level)
        except:
            print "ignored level : ", level
    levels = temp_list

    if len(levels) == 0:
        print "No levels found in this directory"
        return
    elif len(levels) == 1:
        print "Only one level found. Shifting makes no sense."
        return

    # get the latitude and longitude of the North West corner of the minimum level
    min_level = levels[0]

    def get_northwest_tile_of_level(level_directory):
        lines = get_folders_in_dir(level_directory)
        lines = sorted(lines)
        images = sorted(os.listdir(os.path.join(level_directory, lines[0])))
        first_image_name = int(re.findall('\d+', images[0])[0])
        return int(lines[0]), first_image_name

    min_level_path = os.path.join(workingDir, min_level)
    no_tile_ref = get_northwest_tile_of_level(min_level_path)
    lat_lon_ref = num2deg(no_tile_ref[0], no_tile_ref[1], int(min_level))

    # add the minimum level in the list, with no shift
    levels_object = []
    levels_object.append(Level(int(min_level), min_level_path, 0, 0))

    # display level size
    show_size_of_map(min_level_path, len(levels) - 1)

    # calculate shifts for each remaining levels
    for level in levels[1:]:
        level_path = os.path.join(workingDir, level)
        virtual_tile = deg2num(lat_lon_ref[0], lat_lon_ref[1], int(level))
        real_tile = get_northwest_tile_of_level(level_path)
        shift_row = real_tile[0] - virtual_tile[0]
        shift_col = real_tile[1] - virtual_tile[1]

        if shift_col < 0 or shift_row < 0:
            print "Negative shifting. Error."
            return None

        levels_object.append(Level(int(level), level_path, shift_row, shift_col))
    return levels_object


def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise


def process_levels(levels, dest_dir):
    """
    Process a list of Level objects.
    :param levels: a list of Level object
    :param dest_dir: the directory which processed Level objects will copied into
    """
    level_index = 0
    for level in levels:
        if not type(level) is Level:
            print "The level must be a Level object"
            return

        dirs = get_folders_in_dir(level._path)

        dirs = sorted(dirs)

        # copy all images and rename them accordingly
        dir_index = level._shift_row
        for dir_ in dirs:
            file_index = level._shift_col
            for file_ in sorted(os.listdir(os.path.join(level._path, dir_))):
                path = os.path.join(level._path, dir_, file_)
                if os.path.isfile(path):
                    dest_file = os.path.join(dest_dir, str(level_index), str(dir_index))
                    mkdir_p(dest_file)
                    shutil.copy(path, os.path.join(dest_dir, str(level_index), str(dir_index), str(file_index) + '.png'))
                file_index += 1
            dir_index += 1
        level_index += 1


def show_size_of_map(level_path, n):
    rows = get_folders_in_dir(level_path)
    n_row = len(rows)
    n_col = len(os.listdir(os.path.join(level_path, rows[0])))

    map_width = n_col * TILE_SIZE * math.pow(2, n-1)
    map_height = n_row * TILE_SIZE * math.pow(2, n-1)
    # height and width are voluntarily inverted. This is due to how TileView works.
    print "Size of viewport at scale 1 : ", map_width, " height | ", map_height, " width"


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--wd", help="working directory")
    parser.add_argument("--mapname", help="Name of the folder which will be created under the working directory." +
                                          " Default is 'map'")
    args = parser.parse_args()

    if args.wd:
        workingDir = os.wd
    else:
        workingDir = os.getcwd()
    print "Working directory set to ", workingDir

    # process shifts
    levels = process_shifts(workingDir)

    # destination dir is 'map' inside the working dir (by default)
    folder_name = args.mapname if args.mapname else 'map'
    destDir = os.path.join(workingDir, folder_name)

    # process levels
    process_levels(levels, destDir)
