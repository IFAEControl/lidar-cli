#!/bin/bash

# README
# Before running this script in a loop check that:
#    1. hotwind works
#    2. get the endswitch positions and configure it

set -e

FNAME="$(date +%Y-%m-%d@%H:%M).dat"

function licli() {
    ./build/install/lidar-cli/bin/licli $@
    echo "Executed: $@" >> ${FNAME}
}

function w_val() {
    echo "$1" >> ${FNAME}
}

function w_date() {
    date >> ${FNAME}
}

function w_sensors() {
    licli llc sensors --converted 2>/dev/null | grep -iE "(Temperature|Humidity)" | tr n t >> ${FNAME}
}

function get_laser_temp() {
    licli llc laser --get-temp 2>/dev/null | cut -d "." -f 1
}

w_date
w_sensors
licli motors doors --open=1
w_date
licli operation --arm-init
licli operation --arm-align
w_date
licli motors petals --open=1
w_date
licli operation telescope --to-max-zenith
w_date
# Maybe here we should go to max + 1 and check if position == max to check that end switch works well
licli operation telescope --to-min-azimuth
licli operation telescope --to-max-azimuth
licli operation --go-azimuth-parking
w_date
licli operation --go-zenith-parking
licli motors petals --close=1
w_date
w_sensors
licli operation --laser-power-on
# can we do this ¿? licli operation --laser-init
w_date
LASER_TEMP="$(get_laser_temp)"
while [[ "$LASER_TEMP" -lt 32 ]]; do
    licli llc relay --hotwind-on
done
while [[ "$LASER_TEMP" -lt 32 ]]; do
    sleep 15
    LASER_TEMP="$(get_laser_temp)"
    echo $LASER_TEMP
    w_val "$LASER_TEMP"
done
licli llc relay --hotwind-off
w_date
licli operation --ramp-up
licli operation --ramp-down
licli operation acq --shots=3
licli motors doors --close=1
w_date
w_sensors
