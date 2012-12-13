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
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author pszjmb
 */
public class Timebridge {

    private String tsPub;
    private String tsPri;
    Date lasttime;
    Date prevtime;
    private String fail = null;

    public Timebridge(String tsPub, String tsPri) {
        this.tsPub = tsPub;
        this.tsPri = tsPri;
    }

    public void setLasttime(Date lasttime) {
        this.lasttime = lasttime;
    }

    /**
     * Fetches data from a cosm source
     * @param source is the Path to the Cosm source
     * @param lasttime is the last time that this was done. If null, will
     * attempt to retrieve 6 hours of data (max Cosm allows).
     * @return The data as a String
     */
    public String fetchDataFromSource(String source) throws Exception {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date now = new Date();
        String url = source + "&start=" + dt.format(lasttime)
                + "&end=" + dt.format(new Date());
        prevtime = lasttime;
        setLasttime(now);
        System.out.println("source: " + url);
        URLConnection yc = new URL(url).openConnection();
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
     * Returns a HMAC for given parameters
     * 
     * @param params
     *            are all the parameters to hash
     * @return the HMAC as a String
     */
    private String getSecurityString(List<String> params) {
        java.util.Collections.sort(params);
        String toHash = "";
        ListIterator<String> it = params.listIterator();
        while (it.hasNext()) {
            toHash += it.next() + "&";
        }
        System.out.println("To hash:" + toHash);
        return hmacString(toHash, tsPri, "HmacSHA256");
    }

    /**
     * Generate an HMAC Based on example:
     * http://stackoverflow.com/questions/6312544
     * /hmac-sha1-how-to-do-it-properly-in-java
     * 
     * @param value
     *            is a String to hash
     * @param key
     *            is the private key to hash with#
     * @param type
     *            is the Mac format to use such as HmacSHA256
     * @return The hmac
     */
    private String hmacString(String value, String key, String type) {
        try {
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, type);

            // Get a Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(type);
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(value.getBytes());

            // Convert raw bytes to Hex
            byte[] hexBytes = new Hex().encode(rawHmac);

            // Covert array of Hex bytes to a String
            return new String(hexBytes, "UTF-8");
        } catch (Exception e) {
            fail = e.getLocalizedMessage();
            return null;
        }
    }

    /**
     * Loads data into a timestream
     * @param data
     * @param target 
     */
    public void loadData(String data, String target) {
        System.out.println("data: " + data);
        try{
            JSONObject jsonObj = new JSONObject(data.trim());
        JSONArray array = jsonObj.getJSONArray("datastreams").getJSONObject(0).getJSONArray("datapoints");
        String measurements =
        "{\"measurements\":[";
        for (int i = 0; i < array.length(); i++) {
            measurements +="{\"v\":\"" + array.getJSONObject(i).getString("value") +
            "\",\"t\":" + 
            "\""+ array.getJSONObject(i).getString("at") + "\"},";
        }
        measurements = measurements.replaceAll(",$", "");
        measurements += "]}";
        long time = lasttime.getTime();
        String now = Long.toString(time).substring(0, 10);
        
        List<String> params = new ArrayList<String>();
        params.add(tsPub);
        params.add(now);
        params.add(measurements);
        String names[] = target.split("/");
        String name = names[names.length-1];
        params.add(name);
        String hmac = getSecurityString(params);
        String paramsstr = "name=" + URLEncoder.encode(name, "UTF-8")
                + "&measurements=" + URLEncoder.encode(measurements, "UTF-8")
                + "&pubkey=" + URLEncoder.encode(tsPub, "UTF-8") + "&now="
                + URLEncoder.encode(now, "UTF-8") + "&hmac="
                + URLEncoder.encode(hmac, "UTF-8");
        URL url = new URL(target);
        doPost(url, paramsstr);
        }catch(Exception e){
           System.out.println(e.getLocalizedMessage());           
         setLasttime(prevtime);
        }
    }

    /**
     * Performs HTTP post for a given URL
     * 
     * @param url
     *            is the URL to get
     * @param params
     *            is a URL encoded string in the form x=y&a=b...
     * @return a String with the contents of the get
     */
    private void doPost(URL url, String params) throws Exception {
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        connection.setRequestProperty("Content-Length",
                "" + Integer.toString(params.getBytes().length));

        connection.setDoInput(true);
        connection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(
                connection.getOutputStream());
        wr.writeBytes(params);
        wr.flush();
        wr.close();
        Map<String, List<String>> responseHeaderFields = connection.getHeaderFields();
        System.out.println(responseHeaderFields);
        if (responseHeaderFields.get(null).get(0).contains("200 OK")) {
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(is));
            String line;
            StringBuffer response = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            System.out.println(response);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Incorrect number of parameters.");
            System.err.println("Usage: java Timebridge [String cosmFeedUrl] "
                    + " [String timestreamUrl] [String tsPub] [String tsPri] "
                    + "[int interval seconds]");
            System.exit(1);
        }
        System.out.println("Arguments:");
        for (int i = 0; i < args.length; i++) {
            System.out.println("" + i + ": " + args[i]);
        }
        Timebridge tb = new Timebridge(args[2], args[3]);
        String cosm = args[0];
        String timestreams = args[1];
        Long interval = Long.parseLong(args[4]);
        tb.setLasttime(new Date(new Date().getTime()-1000*60*50));
         while (true) {
        tb.loadData(tb.fetchDataFromSource(cosm), timestreams);
            Thread.sleep(interval*1000);
        }
    }
    public static final String myData = "{     \"datastreams\":[      {         \"datapoints\":[            {               \"value\":\"0.001118\",               \"at\":\"2012-12-12T12:00:28.564799Z\"            }         ],         \"current_value\":\"0.001145\",         \"at\":\"2012-12-12T13:50:44.805730Z\",         \"max_value\":\"0.012933\",         \"unit\":{            \"label\":\"kWh\"         },         \"min_value\":\"0.000332\",         \"id\":\"kwh\"      }   ]}";
}
