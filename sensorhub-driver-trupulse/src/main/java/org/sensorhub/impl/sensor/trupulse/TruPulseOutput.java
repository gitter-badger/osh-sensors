/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Botts Innovative Research Inc. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.trupulse;

import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.sensor.SensorDataEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import org.vast.swe.SWEHelper;


public class TruPulseOutput extends AbstractSensorOutput<TruPulseSensor>
{
    private static String MSG_PREFIX = "$PLTIT";
    private static String MSG_TYPE_HV = "HV";
    //private static String MSG_TYPE_HT = "HT";
    //private static String MSG_TYPE_ML = "ML";
    private static double FEET_TO_METERS = 0.304800610;
    private static double YARDS_TO_METERS = 0.9144;
    
    DataComponent laserData;
    DataEncoding dataEncoding;
    BufferedReader msgReader;
    boolean sendData;
    
    
    public TruPulseOutput(TruPulseSensor parentSensor)
    {
        super(parentSensor);
    }


    @Override
    public String getName()
    {
        return "rangeData";
    }


    public void init()
    {
        SWEHelper fac = new SWEHelper();
        
        // build SWE Common record structure
        laserData = fac.newDataRecord(5);
        laserData.setName(getName());
        laserData.setDefinition("http://sensorml.com/ont/swe/property/LaserRangeData");
        
        // add time, horizontalDistance, azimuth, inclination, and slopeDistance
        laserData.addComponent("time", fac.newTimeStampIsoUTC());        
        laserData.addComponent("horizDistance", fac.newQuantity(SWEHelper.getPropertyUri("HorizontalDistance"), "Horizontal Distance", null, "m"));
        laserData.addComponent("slopeDistance", fac.newQuantity(SWEHelper.getPropertyUri("LineOfSightDistance"), "Line-of-Sight Distance", null, "m"));
        
        // for azimuth (trueHeading), we also specify a reference frame
        Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("TrueHeading"), "True Heading", null, "deg");
        q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q.setAxisID("z");
        laserData.addComponent("azimuth", q);
        
        // for inclination, we also specify a reference frame
        q = fac.newQuantity(SWEHelper.getPropertyUri("Inclination"), "Inclination", null, "deg");
        q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
        q.setAxisID("y");
        laserData.addComponent("inclination", q);
     
        // also generate encoding definition as text block
        dataEncoding = fac.newTextEncoding(",", "\n");        
    }
    

    /* TODO: only using HV message; add support for HT and ML */
    private void pollAndSendMeasurement()
    {
    	double hd = Double.NaN;
    	double incl = Double.NaN;
    	double az = Double.NaN;
    	double sd = Double.NaN;
    	
    	try
    	{
    	    long msgTime = 0;
    	    boolean gotHvMsg = false;
    	    while (!gotHvMsg)
    		{
        	    String line = msgReader.readLine();
        	    msgTime = System.currentTimeMillis();
        	    String val, unit;
        	    
                // parse the data string
        	    TruPulseSensor.log.debug("Message received: {}", line);
                String[] tokens = line.split(",");
                
                val = tokens[0];
                if (!val.equals(MSG_PREFIX))
        		{
                    TruPulseSensor.log.warn("Message initial token does NOT equal expected string {}", MSG_PREFIX);
        			continue;
        		}
        
            	// check for desired message type HV
                val = tokens[1];
            	if (!val.equals(MSG_TYPE_HV))
            	{
            	    TruPulseSensor.log.warn("Unsupported message type {}", val);
        			continue;
        		}
            	
            	// get horizontal distance measure and check units (convert if not meters)
            	val = tokens[2];
            	unit = tokens[3];
            	if (val.length() > 0 && unit.length() > 0)
            	{
                	hd = Double.parseDouble(val);
                	hd = convert(hd, unit);
            	}
            	
            	// get azimuth angle measure (units should be degrees)
            	val = tokens[4];
                unit = tokens[5];
                if (val.length() > 0 && unit.length() > 0)
                    az = Double.parseDouble(val);
        
            	// get inclination angle measure (units should be degrees)
                val = tokens[6];
                unit = tokens[7];
                if (val.length() > 0 && unit.length() > 0)
                    incl = Double.parseDouble(val);
        
            	// get slope distance measure and check units (should be meters)
                val = tokens[8];
                unit = tokens[9];
                if (val.length() > 0 && unit.length() > 0)
                {
                    sd = Double.parseDouble(val);
                    sd = convert(hd, unit);
                }
                
                gotHvMsg = true;
    		}
    	    
    	    // create and populate datablock
            DataBlock dataBlock;
            if (latestRecord == null)
                dataBlock = laserData.createDataBlock();
            else
                dataBlock = latestRecord.renew();
            
            dataBlock.setDoubleValue(0, msgTime / 1000.);
            dataBlock.setDoubleValue(1, hd);
            dataBlock.setDoubleValue(2, sd);
            dataBlock.setDoubleValue(3, az);
            dataBlock.setDoubleValue(4, incl);
            
            // update latest record and send event
            latestRecord = dataBlock;
            latestRecordTime = System.currentTimeMillis();
            eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, TruPulseOutput.this, dataBlock)); 
    	}
    	catch(IOException e)
    	{
           if (sendData)
                TruPulseSensor.log.error("Unable to parse TruPulse message", e);
        }     
    }
    
    
    protected double convert(double val, String unit)
    {
        // feet to meters
        if (unit.equals("F"))
            return val * FEET_TO_METERS;
        
        // yards to meters
        else if (unit.equals("Y"))
            return val * YARDS_TO_METERS;
        
        return val;
    }


    protected void start(ICommProvider<?> commProvider)
    {
        if (sendData)
            return;
        
        sendData = true;
        
        // connect to data stream
        try
        {
            msgReader = new BufferedReader(new InputStreamReader(commProvider.getInputStream()));
            TruPulseSensor.log.info("Connected to TruPulse data stream");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while initializing communications ", e);
        }
        
        // start main measurement thread
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                while (sendData)
                {
                    pollAndSendMeasurement();
                }
            }
        });
        t.start();
    }


    protected void stop()
    {
        sendData = false;
        
        if (msgReader != null)
        {
            try { msgReader.close(); }
            catch (IOException e) { }
            msgReader = null;
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
    	return 1200.0; // 20min
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return laserData;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return dataEncoding;
    }
}
