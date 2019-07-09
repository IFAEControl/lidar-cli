#!/bin/bash

set -e

FNAME="$(date +%Y-%m-%d@%H:%M).dat"

alias licli=./build/distributions/lidar-cli/bin/licli

function w_date() {
    date >> ${FNAME}
}

function w_sensors() {
    licli llc sensors --converted 2>/dev/null | grep -iE "(Temperature|Humidity)" | tr n t >> ${FNAME}
}

w_date
w_sensors
licli motors doors --open=1
w_date
licli operation --arm-init
licli operation --arm-align
w_date
licli motors petals -open=1
w_date
licli operation telescope --to-max-zenith
w_date
# Maybe here we should go to max + 1 and check if position == max to check that end switch works well
licli operation telescope --to-min-azimuth
licli operation telescope --to-max-azimuth
w_date

