Timebridge
=============

Application to stream data from Cosm to Timestreams. Developed as part of the [Relate project](http://horizab1.miniserver.com/relate/).

Usage
-----
1. Set up an account on Cosm
2. In Cosm add an API key
3. Make a note of the URL and datastream of the Cosm data that you want to stream
4. Compose a URL for the datastream like so (replacing the items with : before them with your values)--  http://api.cosm.com/v2/feeds/:id.json?key=:apikey&interval=:interval&datastreams=:name 
5. In Timestreams add a new measurement container
6. Make a note of the measurement container's table name and compose a URL for the datastream such as --
http://timestreams.wp.horizon.ac.uk/wp-content/plugins/timestreams/2/measurements/:tablename 
7. Go to timestreams>API keys, create new api keys and reveal the private key
8. java -jar timebridge.jar [String cosmFeedUrl] [String timestreams measurement container Url] [timestreams public key] [timestreams private key] [interval in seconds for how often to poll the Cosm feed]

Example parameters:
-------------------
http://api.cosm.com/v2/feeds/84353.json?key=*****&interval=1&datastreams=kwh 
http://timestreams.wp.horizon.ac.uk/wp-content/plugins/timestreams/2/measurements/wp_1_ts_EnergyUsage_211 
a135b81ae5 
********* 
20

Additional Libraries
--------------------
To run Timebridge you need lib subdirectory with [commons-codec-1.7.jar]( https://commons.apache.org/codec/download_codec.cgi)

To compile the Java code, you will need [the JSON-java files](https://github.com/douglascrockford/JSON-java).

Contributing
------------

1. Fork it
2. Create a branch (`git checkout -b my_markup`)
3. Commit your changes (`git commit -am "Added Snarkdown"`)
4. Push to the branch (`git push origin my_markup`)
5. Create an [Issue][1] with a link to your branch
6. Enjoy a refreshing Diet Coke and wait

License
------------
Copyright (C) 2012 Horizon Digital Economy Research Institute

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.