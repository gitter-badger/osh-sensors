/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.mti;

import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


public class MtiConfig extends SensorConfig
{
    
    @DisplayInfo(desc="Communication settings to connect to IMU data stream")
    public CommProviderConfig<?> commSettings;
    
    @DisplayInfo(label="Decimation Factor", desc="Decimation factor of attitude measurements")
    public int decimFactor = 10;
    
    
    public MtiConfig()
    {
        this.moduleClass = MtiSensor.class.getCanonicalName();
    }
}
