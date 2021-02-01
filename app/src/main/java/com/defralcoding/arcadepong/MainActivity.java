package com.defralcoding.arcadepong;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
//"ws://ec2-15-161-238-106.eu-south-1.compute.amazonaws.com:8081/"
    ImageView imageCanvas;
    TextView txtPGiocatore, txtPAvversario;

    Bitmap bmp;
    Canvas canvas;

    private SocketConnettore socket;

    public int cWidth, cHeight;

    public Pallina pallina;
    public Racchetta rGiocatore, rAvversario;
    public int punteggioGiocatore = 0;
    public int punteggioAvversario = 0;
    public boolean masterPartita = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageCanvas = (ImageView) findViewById(R.id.canvas);
        txtPGiocatore = (TextView) findViewById(R.id.txtPuntiGiocatore);
        txtPAvversario = (TextView) findViewById(R.id.txtPuntiAvversario);
        try {
            socket = new SocketConnettore(new URI("ws://ec2-15-161-238-106.eu-south-1.compute.amazonaws.com:8081/"), this);
            this.socket.connect();
            while (!this.socket.isOpen()) {
                //aspetto che il socket sia aperto
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        cWidth = imageCanvas.getWidth();
        cHeight = imageCanvas.getHeight();

        pallina = new Pallina(cWidth / 3, cHeight / 3);
        rGiocatore = new Racchetta((cWidth / 2) - (Racchetta.w / 2), cHeight - Racchetta.h);
        rAvversario = new Racchetta((cWidth / 2) - (Racchetta.w / 2), 0);

        Update();
    }

    private void Update() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //resetto il canvas
                        bmp = Bitmap.createBitmap(cWidth, cHeight, Bitmap.Config.ARGB_8888);
                        canvas = new Canvas(bmp);
                        canvas.drawColor(Color.BLACK);
                        imageCanvas.setImageBitmap(bmp);

                        //aggiorno le posizioni degli elementi
                        pallina.x += pallina.velocitaX;
                        pallina.y += pallina.velocitaY;


                        //controllo se la pallina ha sbattuto contro il muro
                        if (UrtoMuro()) {
                            pallina.velocitaX = -pallina.velocitaX;
                        }

                        if (masterPartita) {
                            if (PassataRacchetta()) {
                                if (UrtoRacchetta()) {

                                    if (pallina.y + pallina.raggio > rGiocatore.y) {
                                        double puntoCollisione = (double) (pallina.x - (rGiocatore.x + rGiocatore.w / 2)) / (rGiocatore.w / 2);
                                        Log.d("puntocollisione", ""+puntoCollisione);
                                        double angoloScontro = Math.PI * puntoCollisione;

                                        pallina.velocitaX = Math.sin(angoloScontro) * -1 * pallina.velocita;
                                        pallina.velocitaY = Math.cos(angoloScontro) * -1 * pallina.velocita;

                                        pallina.velocita += 0.1;
                                    } else {
                                        double puntoCollisione = (double) (pallina.x - (rAvversario.x + rAvversario.w / 2)) / (rAvversario.w / 2);
                                        Log.d("puntocollisiona", ""+puntoCollisione);
                                        double angoloScontro = Math.PI * puntoCollisione;

                                        pallina.velocitaX = Math.cos(angoloScontro) * -1 * pallina.velocita;
                                        pallina.velocitaY = Math.sin(angoloScontro) * -1 * pallina.velocita;

                                        pallina.velocita += 0.1;
                                    }

                                } else {

                                    if (pallina.y + pallina.raggio > rGiocatore.y) {
                                        socket.send("vinto");
                                        punteggioGiocatore += 1;
                                    } else {
                                        socket.send("perso");
                                        punteggioAvversario += 1;
                                    }
                                    ResetPallina();
                                }
                            }

                            socket.send("p:" + ((float) pallina.x / cWidth) + ":" + ((float) pallina.y / (cHeight - Racchetta.h * 2)));

                        }

                        //aggiorno la vista
                        DrawPallina();
                        DrawRacchetta(rGiocatore);
                        DrawRacchetta(rAvversario);
                        AggiornaPunteggi();

                    }
                });
                Update();
            }
        }, 1000 / 30);
    }

    private void AggiornaPunteggi() {
        txtPGiocatore.setText(""+punteggioGiocatore);
        txtPAvversario.setText(""+punteggioAvversario);
    }

    private void DrawPallina() {
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        canvas.drawCircle(pallina.x, pallina.y, pallina.raggio, paint);
        imageCanvas.setImageBitmap(bmp);
    }

    private void DrawRacchetta(Racchetta r) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(r.x, r.y, r.x + r.w, r.y + r.h, paint);
        imageCanvas.setImageBitmap(bmp);
    }

    public void ResetPallina() {
        pallina = new Pallina(cWidth / 2, cHeight / 2);
        pallina.velocita = 10;
        pallina.velocitaX = 10;
        pallina.velocitaY = 10;
    }

    private boolean UrtoMuro() {
        return pallina.x - pallina.raggio < 0 || pallina.x + pallina.raggio > cWidth;
    }

    private boolean PassataRacchetta() {
        return pallina.y + pallina.raggio > rGiocatore.y || pallina.y - pallina.raggio < rAvversario.y;
    }

    private boolean UrtoRacchetta() {
        return pallina.x + pallina.raggio > rGiocatore.x && pallina.x - pallina.raggio < rGiocatore.x + rGiocatore.w || pallina.x + pallina.raggio > rAvversario.x && pallina.x - pallina.raggio < rAvversario.x + rAvversario.w;
    }

    public void MuoviRacchettaSinistra(View view) {
        rGiocatore.x -= 30;
        socket.send("r:" + ((float) rGiocatore.x / cWidth));
    }

    public void MuoviRacchettaDestra(View view) {
        rGiocatore.x += 30;
        socket.send("r:" + ((float) rGiocatore.x / cWidth));
    }
}