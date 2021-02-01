package com.defralcoding.arcadepong;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;

public class SocketConnettore extends WebSocketClient {

    private MainActivity mainActivity;

    public SocketConnettore(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public SocketConnettore(URI serverURI) {
        super(serverURI);
    }

    public SocketConnettore(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public SocketConnettore(URI serverURI, MainActivity mainActivity) {
        super(serverURI);
        this.mainActivity = mainActivity;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        super.send("connesso");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received: " + message);

        if (message.equals("connesso")) {
            super.send("partitaIniziata");
            mainActivity.masterPartita = true;
            mainActivity.pallina.velocita = 10;
            mainActivity.pallina.velocitaX = 10;
            mainActivity.pallina.velocitaY = 10;
        }

        if (message.contains("r:")) {
            mainActivity.rAvversario.x = (int) (mainActivity.cWidth - (Float.valueOf(String.valueOf(message.split(":")[1])) * (float) mainActivity.cWidth) - Racchetta.w);
        }

        if (message.contains("p:")) {
            mainActivity.pallina.x = (int) (mainActivity.cWidth - (Float.valueOf(String.valueOf(message.split(":")[1])) * (float) mainActivity.cWidth));
            mainActivity.pallina.y = (int) (mainActivity.cWidth - (Float.valueOf(String.valueOf(message.split(":")[2])) * (float) mainActivity.cWidth));
        }

        if (message.equals("perso")) {
            mainActivity.punteggioGiocatore += 1;
        }

        if (message.equals("vinto")) {
            mainActivity.punteggioAvversario += 1;
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println(
                "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

}