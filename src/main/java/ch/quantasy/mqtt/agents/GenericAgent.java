/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.quantasy.mqtt.agents;

import ch.quantasy.gateway.service.stackManager.ManagerServiceContract;
import ch.quantasy.mqtt.gateway.client.AyamlClientContract;
import ch.quantasy.mqtt.gateway.client.GatewayClient;
import ch.quantasy.tinkerforge.stack.TinkerforgeStackAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author reto
 */
public class GenericAgent extends GatewayClient<AyamlClientContract> {

    private final Map<TinkerforgeStackAddress, Boolean> stacks;
    private final Set<ManagerServiceContract> managerServiceContracts;
    private final Map<ManagerServiceContract, Set<TinkerforgeStackAddress>> managedStacks;

    public GenericAgent(URI mqttURI, String clientID, AyamlClientContract contract) throws MqttException {
        super(mqttURI, clientID, contract);
        stacks = new HashMap<>();
        managerServiceContracts = new HashSet<>();
        managedStacks = new HashMap<>();
    }

    @Override
    public void connect() throws MqttException {
        super.connect();
        subscribe("TF/Manager/U/+/S/connection", (topic, payload) -> {
            System.out.println("Message arrived from: " + topic);
            synchronized (managerServiceContracts) {
                String managerUnit = topic.split("/")[3];
                managerServiceContracts.add(new ManagerServiceContract(managerUnit));
                System.out.println(managerUnit);
                managerServiceContracts.notifyAll();
            }
        });
        subscribe("TF/Manager/U/+/S/stack/address/#", (topic, payload) -> {
            System.out.println("Message arrived from: " + topic);
            synchronized (managedStacks) {
                System.out.println("--->" + topic);
                String managedStackAddressParts[] = topic.split("/");
                ManagerServiceContract managerServiceContract = new ManagerServiceContract(managedStackAddressParts[3]);
                Set<TinkerforgeStackAddress> addresses = managedStacks.get(managerServiceContract);
                if (addresses == null) {
                    addresses = new HashSet<>();
                    managedStacks.put(managerServiceContract, addresses);
                }
                String[] stackAddressParts = managedStackAddressParts[7].split(":");
                System.out.println(Arrays.toString(stackAddressParts));
                if (payload != null) {
                    addresses.add(new TinkerforgeStackAddress(stackAddressParts[0], Integer.parseInt(stackAddressParts[1])));
                    System.out.println(stackAddressParts[0] + " available.");

                } else {
                    managedStacks.remove(new TinkerforgeStackAddress(stackAddressParts[0], Integer.parseInt(stackAddressParts[1])));
                    System.out.println(stackAddressParts[0] + " gone.");
                }
                managedStacks.notifyAll();
            }

        }
        );
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            //Well this is ok
        }
    }

    public ManagerServiceContract[] getManagerServiceContracts() {
        synchronized (managerServiceContracts) {
            if (managerServiceContracts.isEmpty()) {
                try {
                    managerServiceContracts.wait(3000);
                } catch (InterruptedException ex) {
                    //that is ok
                }
            }
            return managerServiceContracts.toArray(new ManagerServiceContract[0]);
        }
    }

    public Set<TinkerforgeStackAddress> getManagedStacks(ManagerServiceContract managerServiceContract) {
        Set<TinkerforgeStackAddress> addresses = new HashSet<>();
        Set stackSet = this.managedStacks.get(managerServiceContract);
        if (stackSet != null) {
            addresses.addAll(stackSet);
        }
        return addresses;
    }

    public void removeStackFrom(ManagerServiceContract managerServiceContract, TinkerforgeStackAddress address) {
        publishIntent(managerServiceContract.INTENT_STACK_ADDRESS_REMOVE, address);
    }

    public void connectStacksTo(ManagerServiceContract managerServiceContract, TinkerforgeStackAddress... addresses) {
        for (TinkerforgeStackAddress address : addresses) {
            connectStackTo(managerServiceContract, address);
        }
    }

    public void connectStackTo(ManagerServiceContract managerServiceContract, TinkerforgeStackAddress address) {
        if (!address.getHostName().equals("localhost")) {
            for (Set<TinkerforgeStackAddress> managedStacks : managedStacks.values()) {
                if (managedStacks.contains(address)) {
                    return;
                }
            }
        }
        String stackName = address.getHostName() + ":" + address.getPort();
        synchronized (stacks) {
            stacks.put(address, false);
        }
        System.out.println("Subscribing to " + address);
        subscribe(managerServiceContract.STATUS_STACK_ADDRESS + "/" + stackName, (topic, payload) -> {
            System.out.println("Message arrived from: " + topic);
            Boolean isConnected = false;
            if (payload.length>0) {
                isConnected = getMapper().readValue(payload, Boolean.class);
            }
            synchronized (stacks) {
                stacks.put(address, isConnected);
                stacks.notifyAll();
            }
        });
        System.out.println("Connecting: " + stackName);

        publishIntent(managerServiceContract.INTENT_STACK_ADDRESS_ADD, address);

        synchronized (stacks) {
            while (!stacks.get(address)) {
                try {
                    stacks.wait(500);
                } catch (InterruptedException ex) {
                    //That is ok 
                }
            }
        }
        System.out.println("Connected: " + stackName);

        //This is an ugly hack in order to cope with a race-condition:
        //As soon as the stack is ready, it spawns new threads as soon as it detects a new Brick(let).
        //Unfortunately, it is not known, when this process is finished (@see IPConnection#enumerate)
        //Thus waiting 3 seconds might be fine.
        //This is not a solution! This states an intrinsic problem.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            //That is fine.
        }
    }
}
