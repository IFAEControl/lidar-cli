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

function get_door_status() {
    licli motors doors --status 2>/dev/null | grep status | cut -d " " -f 2
}

function get_azimuth_pos() {
    licli motors --ga 2>/dev/null | cut -d " " -f 2
}

function get_zenith_pos() {
    licli motors --gz 2>/dev/null | cut -d " " -f 3
}

w_date
w_sensors
licli motors doors --open=1
while [[ "$(get_door_status)" != "OPENED" ]]; do
    sleep 10
done
w_date
licli operation --arm-init
licli operation --arm-align
w_date
licli motors petals --open=1
w_date
licli operation telescope --to-max-zenith
while [[ "$(get_zenith_pos)" -ne 952 ]]; do
    sleep 10
done
w_date
licli motors --sa=900
while [[ "$(get_azimuth_pos)" -ne 900 ]]; do
    sleep 10
done
licli operation --go-azimuth-parking
while [[ "$(get_azimuth_pos)" -ne 1120 ]]; do
    sleep 10
done
w_date
licli operation --go-zenith-parking
while [[ "$(get_zenith_pos)" -ne 94 ]]; do
    sleep 10
done
licli motors petals --close=1
w_date
w_sensors
licli operation --laser-power-on
w_date
LASER_TEMP="$(get_laser_temp)"
if [[ "$LASER_TEMP" -lt 32 ]]; then
    licli llc relay --hotwind-on
fi
while [[ "$LASER_TEMP" -lt 32 ]]; do
    sleep 15
    LASER_TEMP="$(get_laser_temp)"
    echo $LASER_TEMP
    w_val "$LASER_TEMP"
done
licli llc relay --hotwind-off
licli motors --sz=300
while [[ "$(get_zenith_pos)" -ne 300 ]]; do
    sleep 10
done
while ! licli llc drivers --gs | grep "LASER" | grep "OFF"; do
    licli operation --laser-init
    sleep 25
done

licli llc laser --stop
licli operation --go-zenith-parking
while [[ "$(get_zenith_pos)" -ne 94 ]]; do
    sleep 10
done
w_date
licli operation --ramp-up
licli operation --ramp-down
licli operation acq --shots=3
licli motors doors --close=1
w_date
w_sensors
