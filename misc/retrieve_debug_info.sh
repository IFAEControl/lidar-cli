#!/bin/bash -x

FILE="debug_info_$(date +%Y%m%d-%H%M)"

function lg() {
	/bin/echo -e "$@" >> $FILE
}

lg "Licli version: $(./licli --version)"
lg ""
lg "Doors:" $(./licli motors doors --status)
lg "Petals:" $(./licli motors petals --status) 
lg ""
lg "Telescope: Azimuth:" $(./licli motors telescope --ga)
lg "Telescope: Zenith:" $(./licli motors telescope --gz)
lg ""
lg "Recorded parking position\n$(./licli operation telescope --get-parking)"
lg ""
lg "Sensors\n$(./licli llc sensors) \n"
lg ""
lg "Raw sensors\n$(./licli llc sensors --raw) \n"
lg ""
lg "Arms: $(./licli llc arms --get-pos)"
lg ""
lg "$(./licli llc drivers --status)"
lg ""
lg "Laser temperature: $(./licli llc laser --get-temp)"
lg "Laser shot counter: $(./licli llc laser --get-counter)"
lg ""
lg "$(./licli llc relays --status)"
lg ""
lg "Monitoring board raw values\n$(./licli monitoring motors --raws)\n"
lg "Monitoring board motors decoded\n$(./licli monitoring motors --motors)\n"
lg "Monitoring board encoders decoded\n$(./licli monitoring motors --encoders)\n"
lg ""
lg "Monitoring sensors: last humidity: $(./licli monitoring sensors --humidity --last-value)"
lg "Monitoring sensors: last env-temperature: $(./licli monitoring sensors --env-temperature --last-value)"
lg ""
lg "Server config (partial): $(./licli config --get)"
lg ""

# First list is from the last hour, second from the second hour and so
lg "Get trace of commands from the last 8 hours\n$(./licli trace --last-hours=8)\n"