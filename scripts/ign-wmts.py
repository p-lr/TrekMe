# -*- coding: utf-8 -*-

"""
This script uses the France IGN WMTS (Web Map Tile Service) to fetch tiles in the given bounding box, for each specified
zoom level.
It produces a file tree that can be imported as a map by TrekAdvisor. To make this possible, a specific methodology to
fetch tiles is used. Based on the grid coordinates of the bounding box at the minimum zoom level, the subsequent grid
coordinates for the upper levels are calculated to ensure that every tile at minimum zoom level has its corresponding
tiles at upper levels (no more, no less).

The default layer is GEOGRAPHICALGRIDSYSTEMS.MAPS, but a different one can be specified with the --layer option.
With the option --layer-scan-express-standard, the new GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD is used.

Example of usage :
python ign-wmts.py --api-key API_KEY --user LOGIN --password PASSWORD --layer-scan-express-standard -zmin 14 -zmax 16 -n -21.147418 -w 55.593894 -s -21.346769 -e 55.797828 --output /home/pla/Bureau/ign-api/test --mapname reunion-est

See https://geoservices.ign.fr/documentation/geoservices/wmts.html
See http://professionnels.ign.fr/
"""

import argparse
import errno
import os
import shutil
from math import fabs, log, sin, floor, ceil

import requests

url = "https://wxs.ign.fr/{}/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&LAYER={}&EXCEPTIONS=text/xml&FORMAT=image/jpeg&TILEMATRIXSET=PM&TILEMATRIX={}&TILEROW={}&TILECOL={}&"

web_mercator_resolutions = {'10': 152.8740565704, '11': 76.4370282852, '12': 38.2185141426,
                            '13': 19.1092570713,
                            '14': 9.5546285356, '15': 4.7773142678, '16': 2.3886571339,
                            '17': 1.1943285670}

# The top left corner
# TODO : this looks the same for each zoom level, so this is statically set here. But ultimately this data should be
# fetch for each zoom level, with a http://wxs.ign.fr/CLEF/geoportail/wmts?SERVICE=WMTS&REQUEST=GetCapabilities request
# See https://geoservices.ign.fr/documentation/geoservices/wmts.html
X0 = -20037508.3427892476
Y0 = -X0


def process_bounding_box(north_lat, west_lon, south_lat, east_lon):
    X_left, Y_top = to_web_mercator(north_lat, west_lon)
    X_right, Y_bot = to_web_mercator(south_lat, east_lon)

    print "X_left : {} Y_top : {}".format(X_left, Y_top)
    print "X_right : {} Y_bot : {}".format(X_right, Y_bot)
    return Y_top, X_left, Y_bot, X_right


def to_web_mercator(latitude, longitude):
    """
    Conversion from WGS84 coordinates to ESPG:3857 (Google web mercator).
    :param latitude: latitude in decimal degrees
    :param longitude: longitude in decimal degrees
    :return: (X, Y) ESPG:3857 coordinates
    """
    if fabs(longitude > 180) or fabs(latitude) > 90:
        raise

    num = longitude * 0.017453292519943295  # 2*pi/360
    merc_x = 6378137.0 * num
    a = latitude * 0.017453292519943295
    merc_y = 3189068.5 * log((1.0 + sin(a)) / (1.0 - sin(a)))

    return merc_x, merc_y


def compute_grid_coordinates(levels_, Y_top, X_left, Y_bot, X_right):
    """
    @param levels_: the list of Level
    @type levels_: list[Level]
    @type Y_top: float
    """

    # Special case of the minimum level
    min_level = levels[0]  # type : Level
    assert isinstance(min_level, Level)
    tile_size_meters = 256 * web_mercator_resolutions[str(min_level.get_zoom())]
    col_left = (X_left - X0) / tile_size_meters
    row_top = (Y0 - Y_top) / tile_size_meters
    col_right = (X_right - X0) / tile_size_meters
    row_bot = (Y0 - Y_bot) / tile_size_meters
    col_left = int(floor(col_left))
    row_top = int(floor(row_top))
    col_right = int(ceil(col_right))
    row_bot = int(ceil(row_bot))
    min_level.set_bounding_grid(row_top, col_left, row_bot, col_right)
    print "col_left : {} row_top : {} col_right : {} row_bot : {}".format(col_left, row_top,
                                                                          col_right, row_bot)

    if len(levels) == 1:
        return

    scale_factor = 2
    for level in levels_[1:]:
        assert isinstance(level, Level)
        col_left = min_level.get_col_left() * scale_factor
        row_top = min_level.get_row_top() * scale_factor
        col_right = (min_level.get_col_right() + 1) * scale_factor - 1
        row_bot = (min_level.get_row_bot() + 1) * scale_factor - 1
        level.set_bounding_grid(row_top, col_left, row_bot, col_right)
        scale_factor *= 2
        print "col_left : {} row_top : {} col_right : {} row_bot : {}".format(col_left, row_top,
                                                                              col_right, row_bot)


def create_map(output, map_name, levels_):
    """
    @param output: the directory in which the map will be created
    @param map_name: the name of the map
    @param levels_: a list of #Level
    """

    # Make the parent dir
    map_dir = os.path.join(output, map_name)
    mkdir_p(os.path.join(map_dir))

    # For each level, create the expected file structure
    assert isinstance(levels_, list)
    level_index = 0
    for level in levels_:
        level_dir = os.path.join(map_dir, str(level_index))
        mkdir_p(level_dir)
        create_rows(level, level_dir)
        level_index += 1


def create_rows(level, level_dir):
    row_index = 0
    assert isinstance(level, Level)
    for row in range(level.get_row_top(), level.get_row_bot() + 1):
        row_dir = os.path.join(level_dir, str(row_index))
        mkdir_p(row_dir)
        fetch_tiles_in_row(level, row_dir, row)
        row_index += 1


def fetch_tiles_in_row(level, row_dir, row):
    col_index = 0
    for col in range(level.get_col_left(), level.get_col_right() + 1):
        download_tile(level.get_layer(), level.get_zoom(), row, col, row_dir, col_index)
        col_index += 1


def download_tile(layer, zoom, row, col, row_dir, col_index):
    print "downloading {} row {} col {}".format(zoom, row, col)
    tile_url = make_url(layer, zoom, row, col)

    r = requests.get(tile_url, auth=(user, passwd), stream=True)
    if r.status_code == 200:
        with open(os.path.join(row_dir, str(col_index) + '.jpg'), 'wb') as f:
            r.raw.decode_content = True
            shutil.copyfileobj(r.raw, f)
    else:
        print r.status_code


def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError as exc:
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise


def make_url(layer, zoom, row, col):
    return url.format(api_key, layer, zoom, row, col)


class Level(object):
    def __init__(self, zoom, layer):
        self._zoom = zoom
        self._layer = layer
        self._row_top = None
        self._col_left = None
        self._row_bot = None
        self._col_right = None

    def set_bounding_grid(self, row_top, col_left, row_bot, col_right):
        self._row_top = row_top
        self._col_left = col_left
        self._row_bot = row_bot
        self._col_right = col_right

    def get_zoom(self):
        return self._zoom

    def get_col_left(self):
        return self._col_left

    def get_col_right(self):
        return self._col_right

    def get_row_top(self):
        return self._row_top

    def get_row_bot(self):
        return self._row_bot

    def get_layer(self):
        return self._layer


if __name__ == '__main__':

    # First, get the arguments
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=os.getcwd(), help='output directory')
    parser.add_argument("--mapname", default="map",
                        help="Name of the folder which will be created under the working directory" +
                             " Default is 'map'")
    parser.add_argument("--layer", default='GEOGRAPHICALGRIDSYSTEMS.MAPS',
                        help='The desired type of layer')
    parser.add_argument("--layer-scan-express-standard", dest='layer', action='store_const',
                        const="GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD",
                        help='A new layer like the default IGN maps, but using pastel colors')
    requiredNamed = parser.add_argument_group('required named arguments')
    requiredNamed.add_argument("--user", required=True, help='The IGN user authentication')
    requiredNamed.add_argument("--password", required=True, help='The IGN password authentication')
    requiredNamed.add_argument("--api-key", required=True, help='The IGN api key')
    requiredNamed.add_argument("-zmin", type=int, required=True, help='The minimum zoom level')
    requiredNamed.add_argument("-zmax", type=int, required=True, help='The maximum zoom level')
    requiredNamed.add_argument("-n", "--north-lat", type=float, required=True,
                               help='The north latitude of the bounding box')
    requiredNamed.add_argument("-w", "--west-lon", type=float, required=True,
                               help='The west longitude of the bounding box')
    requiredNamed.add_argument("-s", "--south-lat", type=float, required=True,
                               help='The south latitude of the bounding box')
    requiredNamed.add_argument("-e", "--east-lon", type=float, required=True,
                               help='The east longitude of the bounding box')
    args = parser.parse_args()

    # Credentials
    api_key = args.api_key
    user = args.user
    passwd = args.password

    # Layer used
    layer_used = args.layer
    print "Using layer", layer_used

    # Create the model objects. For instance, all levels use the same layer
    levels = []
    for zoom_level in range(args.zmin, args.zmax + 1):
        levels.append(Level(zoom_level, layer_used))

    # Compute the bounding box in EPSG:3857 (Web Mercator Spherical)
    Y_top, X_left, Y_bot, X_right = process_bounding_box(args.north_lat, args.west_lon,
                                                         args.south_lat, args.east_lon)

    # Compute the col & rows for each level
    compute_grid_coordinates(levels, Y_top, X_left, Y_bot, X_right)

    # Map creation
    create_map(args.output, args.mapname, levels)
