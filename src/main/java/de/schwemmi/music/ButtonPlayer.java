package de.schwemmi.music;

import io.flic.fliclib.javaclient.Bdaddr;
import io.flic.fliclib.javaclient.ButtonConnectionChannel;
import io.flic.fliclib.javaclient.FlicClient;
import io.flic.fliclib.javaclient.GeneralCallbacks;
import io.flic.fliclib.javaclient.GetInfoResponseCallback;
import io.flic.fliclib.javaclient.enums.BdAddrType;
import io.flic.fliclib.javaclient.enums.BluetoothControllerState;
import io.flic.fliclib.javaclient.enums.ClickType;
import io.flic.fliclib.javaclient.enums.ConnectionStatus;
import io.flic.fliclib.javaclient.enums.CreateConnectionChannelError;
import io.flic.fliclib.javaclient.enums.DisconnectReason;
import io.flic.fliclib.javaclient.enums.RemovedReason;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Component
public class ButtonPlayer {

    @Value("${client.search:false}")
    private Boolean shouldSearch;

    private FlicClient client;
    private NewDiscoveryClient connectionClient;
    private SchlusibengPlayer player;

    private ButtonConnectionChannel.Callbacks buttonCallbacks = new ButtonConnectionChannel.Callbacks() {
        @Override
        public void onCreateConnectionChannelResponse(ButtonConnectionChannel channel, CreateConnectionChannelError createConnectionChannelError, ConnectionStatus connectionStatus) {
            System.out.println("Create response " + channel.getBdaddr() + ": " + createConnectionChannelError + ", " + connectionStatus);
        }

        @Override
        public void onRemoved(ButtonConnectionChannel channel, RemovedReason removedReason) {
            System.out.println("Channel removed for " + channel.getBdaddr() + ": " + removedReason);
        }

        @Override
        public void onConnectionStatusChanged(ButtonConnectionChannel channel, ConnectionStatus connectionStatus, DisconnectReason disconnectReason) {
            System.out.println("New status for " + channel.getBdaddr() + ": " + connectionStatus + (connectionStatus == ConnectionStatus.Disconnected ? ", " + disconnectReason : ""));
        }

        @Override
        public void onButtonUpOrDown(ButtonConnectionChannel channel, ClickType clickType, boolean wasQueued, int timeDiff) throws IOException {
           if(clickType.equals(clickType.ButtonDown) && player.isPlayCompleted()) {
               player.play("/home/pi/slippers.wav");
           }
        }
    };

    public ButtonPlayer(NewDiscoveryClient connectionClient, SchlusibengPlayer player) {
        this.connectionClient = connectionClient;
        this.player = player;
        try {
            client = new FlicClient("localhost");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void startActions() throws IOException {


        client.getInfo(new GetInfoResponseCallback() {
            @Override
            public void onGetInfoResponse(BluetoothControllerState bluetoothControllerState, Bdaddr myBdAddr,
                                          BdAddrType myBdAddrType, int maxPendingConnections, int maxConcurrentlyConnectedButtons,
                                          int currentPendingConnections, boolean currentlyNoSpaceForNewConnection, Bdaddr[] verifiedButtons) throws IOException {

                for (final io.flic.fliclib.javaclient.Bdaddr bdaddr : verifiedButtons) {
                    client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
                }
            }
        });
        client.setGeneralCallbacks(new GeneralCallbacks() {
            @Override
            public void onNewVerifiedButton(Bdaddr bdaddr) throws IOException {
                System.out.println("Another client added a new button: " + bdaddr + ". Now connecting to it...");
                client.addConnectionChannel(new ButtonConnectionChannel(bdaddr, buttonCallbacks));
            }
        });
        new Thread(() -> {
            try {
                client.handleEvents();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @PostConstruct
    public void startListening() {
        try {
            if(shouldSearch != null && shouldSearch) {
                connectionClient.discover();

            }
            this.startActions();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @PreDestroy
    public void destroy() {
        System.out.println("Pre desctory of client");
        try {
            this.client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
