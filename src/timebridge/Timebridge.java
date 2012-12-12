/*
 * Simple application to demonstrate loading data from Cosm into Timestreams 
 * Author Jesse Blum (JMB)
Produced for the Relate Project, Horizon Digital Economy Institute, University of Nottingham

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package timebridge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 *
 * @author pszjmb
 */
public class Timebridge {

    private String tsPub;
    private String tsPri;

    public Timebridge(String tsPub, String tsPri) {
        this.tsPub = tsPub;
        this.tsPri = tsPri;
    }

    /**
     * Fetches data from a cosm source
     * @param source is the Path to the Cosm source
     * @param lasttime is the last time that this was done. If null, will
     * attempt to retrieve 6 hours of data (max Cosm allows).
     * @return The data as a String
     */
    public String fetchDataFromSource(URL source, Date lasttime) throws Exception {
        URLConnection yc = source.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String inputLine;
        StringBuilder sb = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }
        in.close();
        return sb.toString();
    }

    /**
     * Loads data into a timestream
     * @param data
     * @param target 
     */
    public void loadData(String data, URL target) {
        System.out.println(data);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.err.println("Incorrect number of parameters.");
            System.err.println("Usage: java Timebridge [String cosmFeedUrl] "
                    + " [String timestreamUrl] [String tsPub] [String tsPri] [String startdate] [int interval seconds]");
            System.exit(1);
        }
        System.out.println("Arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("" + i + ": " + args[i]);
        }
        Timebridge tb = new Timebridge(args[2], args[3]);
        URL cosm = new URL(args[0]);
        URL timestreams = new URL(args[1]);
        Long interval = Long.parseLong(args[5]);
        Long dateval = Long.parseLong(args[4]);
        Date date = new Date(dateval);
        while (true) {
            tb.loadData(tb.fetchDataFromSource(cosm, date), timestreams);
            Thread.sleep(interval*1000);
            date = new Date(dateval + interval);
        }
    }
}
