/*
 * /*
 *  *   "TiMqWay"
 *  *
 *  *    TiMqWay(tm): A gateway to provide an MQTT-View for the Tinkerforge(tm) world (Tinkerforge-MQTT-Gateway).
 *  *
 *  *    Copyright (c) 2016 Bern University of Applied Sciences (BFH),
 *  *    Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *    Quellgasse 21, CH-2501 Biel, Switzerland
 *  *
 *  *    Licensed under Dual License consisting of:
 *  *    1. GNU Affero General Public License (AGPL) v3
 *  *    and
 *  *    2. Commercial license
 *  *
 *  *
 *  *    1. This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Affero General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License, or
 *  *     (at your option) any later version.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Affero General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Affero General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *  *
 *  *    2. Licensees holding valid commercial licenses for TiMqWay may use this file in
 *  *     accordance with the commercial license agreement provided with the
 *  *     Software or, alternatively, in accordance with the terms contained in
 *  *     a written agreement between you and Bern University of Applied Sciences (BFH),
 *  *     Research Institute for Security in the Information Society (RISIS), Wireless Communications & Secure Internet of Things (WiCom & SIoT),
 *  *     Quellgasse 21, CH-2501 Biel, Switzerland.
 *  *
 *  *
 *  *     For further information contact <e-mail: reto.koenig@bfh.ch>
 *  *
 *  *
 */
package ch.quantasy.mqtt.agents.Segment7x4;

import ch.quantasy.gateway.service.device.segment4x7.Segment4x7ServiceContract;
import ch.quantasy.gateway.service.stackManager.ManagerServiceContract;
import ch.quantasy.mqtt.agents.GenericTinkerforgeAgent;
import ch.quantasy.mqtt.agents.GenericTinkerforgeAgentContract;
import ch.quantasy.tinkerforge.stack.TinkerforgeStackAddress;
import java.net.URI;
import org.eclipse.paho.client.mqttv3.MqttException;
import ch.quantasy.tinkerforge.device.TinkerforgeDeviceClass;
import ch.quantasy.tinkerforge.device.segment4x7.DeviceSegments;

/**
 *
 * @author reto
 */
public class Segment7x4Agent extends GenericTinkerforgeAgent {
    private ManagerServiceContract managerServiceContract;

    public Segment7x4Agent(URI mqttURI) throws MqttException, InterruptedException {
        super(mqttURI, "9h83jkl482", new GenericTinkerforgeAgentContract("Segment7x4", "counter"));

        connect();
        if (super.getTinkerforgeManagerServiceContracts().length == 0) {
            System.out.println("No ManagerServcie is running... Quit.");
            return;
        }

        managerServiceContract = super.getTinkerforgeManagerServiceContracts()[0];
        connectTinkerforgeStacksTo(managerServiceContract, new TinkerforgeStackAddress("localhost"));
        Segment4x7ServiceContract segment4x7ServiceContract = new Segment4x7ServiceContract("pPA", TinkerforgeDeviceClass.SegmentDisplay4x7.toString());
        short[] segments = {1, 2, 3, 4};
        short brightness = 4;
        boolean colon = false;

        subscribe(segment4x7ServiceContract.STATUS_SEGMENTS, (topic, payload) -> {
            for (int i = 0; i < segments.length; i++) {
                segments[i] = (short) ((segments[i] + 1) % 128);
            }
            publishIntent(segment4x7ServiceContract.INTENT_SEGMENTS, new DeviceSegments(segments, brightness, false));

        });
        publishIntent(segment4x7ServiceContract.INTENT_SEGMENTS, new DeviceSegments(segments, brightness, true));

    }

    public void finish() {
        super.removeTinkerforgeStackFrom(managerServiceContract, new TinkerforgeStackAddress("localhost"));
        return;
    }

    public static void main(String... args) throws Throwable {
        URI mqttURI = URI.create("tcp://127.0.0.1:1883");
        if (args.length > 0) {
            mqttURI = URI.create(args[0]);
        } else {
            System.out.printf("Per default, 'tcp://127.0.0.1:1883' is chosen.\nYou can provide another address as first argument i.e.: tcp://iot.eclipse.org:1883\n");
        }
        System.out.printf("\n%s will be used as broker address.\n", mqttURI);
        Segment7x4Agent agent = new Segment7x4Agent(mqttURI);
        System.in.read();
        agent.finish();
    }

}
