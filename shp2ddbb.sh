#!/bin/sh

SOURCE=$1
TMP='sql.tmp'

shp2pgsql -W LATIN1 "${SOURCE}" > "$TMP"

sudo -u postgres psql -f "$TMP" -d tfg -U postgres --password
