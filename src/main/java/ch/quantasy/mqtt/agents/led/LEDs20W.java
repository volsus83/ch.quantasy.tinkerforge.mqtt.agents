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
package ch.quantasy.mqtt.agents.led;

import ch.quantasy.mqtt.agents.led.abilities.AnLEDAbility;

import ch.quantasy.gateway.service.device.ledStrip.LEDStripServiceContract;
import ch.quantasy.gateway.service.stackManager.ManagerServiceContract;
import ch.quantasy.mqtt.agents.GenericTinkerforgeAgent;
import ch.quantasy.mqtt.agents.GenericTinkerforgeAgentContract;
import ch.quantasy.mqtt.agents.led.abilities.Fire;
import ch.quantasy.mqtt.gateway.client.GCEvent;
import ch.quantasy.tinkerforge.device.TinkerforgeDeviceClass;
import ch.quantasy.tinkerforge.device.led.LEDStripDeviceConfig;
import ch.quantasy.tinkerforge.stack.TinkerforgeStackAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author reto
 */
public class LEDs20W extends GenericTinkerforgeAgent {

    private final List<AnLEDAbility> abilities;
    private final int frameDurationInMillis;
    private final int amountOfLEDs;

    public LEDs20W(URI mqttURI) throws MqttException {
        super(mqttURI, "xmastree4985", new GenericTinkerforgeAgentContract("XMasTreeLED", "XMasTreeLed01"));
        connect();
        frameDurationInMillis = 100;
        amountOfLEDs = 4;
        abilities = new ArrayList<>();

        if (super.getTinkerforgeManagerServiceContracts().length == 0) {
            System.out.println("No ManagerServcie is running... Quit.");
            return;
        }

        ManagerServiceContract managerServiceContract = super.getTinkerforgeManagerServiceContracts()[0];
        connectTinkerforgeStacksTo(managerServiceContract,new TinkerforgeStackAddress("lights10"));

        LEDStripDeviceConfig config = new LEDStripDeviceConfig(LEDStripDeviceConfig.ChipType.WS2811, 2000000, frameDurationInMillis, amountOfLEDs, LEDStripDeviceConfig.ChannelMapping.GRB);

        LEDStripServiceContract ledServiceContract1 = new LEDStripServiceContract("xe9", TinkerforgeDeviceClass.LEDStrip.toString());
        publishIntent(ledServiceContract1.INTENT_CONFIG, config);

        abilities.add(new Fire(this, ledServiceContract1, config));

        subscribe(ledServiceContract1.EVENT_LAGING, (topic, payload) -> {
            GCEvent<Long>[] lag = (GCEvent<Long>[]) toEventArray(payload, Boolean.class);
            Logger.getLogger(LEDs20W.class.getName()).log(Level.INFO, "Laging:", Arrays.toString(lag));
        });
        for (AnLEDAbility ability : abilities) {
            new Thread(ability).start();
        }
    }

    public static void main(String[] args) throws Throwable {
        URI mqttURI = URI.create("tcp://127.0.0.1:1883");
        if (args.length > 0) {
            mqttURI = URI.create(args[0]);
        } else {
            System.out.printf("Per default, 'tcp://127.0.0.1:1883' is chosen.\nYou can provide another address as first argument i.e.: tcp://iot.eclipse.org:1883\n");
        }
        System.out.printf("\n%s will be used as broker address.\n", mqttURI);
        LEDs20W agent = new LEDs20W(mqttURI);
        System.in.read();
    }
}
