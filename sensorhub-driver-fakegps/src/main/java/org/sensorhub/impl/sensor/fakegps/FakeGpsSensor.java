/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


/**
 * <p>
 * Driver implementation outputing simulated GPS data after
 * requesting trajectories from Google Directions.
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 2, 2014
 */
public class FakeGpsSensor extends AbstractSensorModule<FakeGpsConfig>
{
    FakeGpsOutput dataInterface;
    
    
    public FakeGpsSensor()
    {
        dataInterface = new FakeGpsOutput(this);
        obsOutputs.put("locationOutput", dataInterface);
        dataInterface.init();
    }
    
    
    @Override
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        AbstractProcess smlSys = super.getCurrentSensorDescription();
        smlSys.setUniqueIdentifier("urn:test:sensors:fakegps");
        return smlSys;
    }


    @Override
    public void start() throws SensorHubException
    {
        dataInterface.start();        
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        dataInterface.stop();
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
       
    }
    
    
    @Override
    public boolean isConnected()
    {
        return true;
    }
}
