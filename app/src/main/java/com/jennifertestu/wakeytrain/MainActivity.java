package com.jennifertestu.wakeytrain;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.util.Log;


public class MainActivity extends AppCompatActivity {


    ObjectAnimator textColorAnim;

    TextView tv;
    AutoCompleteTextView et;
    Button b;
    ImageButton b_help;

    long[] pattern_vib = {0, 200, 500, 1000};
    Vibrator mVibrator;

    LocationManager locationManager;
    LocationListener locationListener;

    Gare destination;

    double curr_longitude, curr_latitude;

    Boolean alerte = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tv = (TextView) findViewById(R.id.Viewer);
        et = (AutoCompleteTextView) findViewById(R.id.editText);
        b = (Button) findViewById(R.id.button);
        b_help = (ImageButton) findViewById(R.id.helpButton);

        b_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder alerte = new AlertDialog.Builder(MainActivity.this);
                alerte.setTitle("Comment utiliser l'application ?");
                alerte.setMessage("- Rentrez le début du nom de votre gare \n" +
                        "- Parmi la liste des propositions, appuyez sur votre gare\n" +
                        "- Appuyez sur valider\n" +
                        "- Votre téléphone se mettra à vibrer à environ 1.5km de votre gare d’arrivée \n");
                alerte.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                alerte.create();
                alerte.show();

            }
        });

        Scanner scanner = new Scanner(getResources().openRawResource(R.raw.gare));
        List<Gare> listeGares = new ArrayList<Gare>();
        while (scanner.hasNext()) {
            String[] ligne = scanner.nextLine().split(";");
            String nom = ligne[0];
            double longitude = Double.valueOf(ligne[1]);
            double latitude = Double.valueOf(ligne[2]);

            listeGares.add(new Gare(nom,longitude,latitude));
        }
        ArrayAdapter<Gare> adapter =
                new ArrayAdapter<Gare>(this, android.R.layout.simple_list_item_1, listeGares );
        et.setAdapter(adapter);
        et.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                destination = (Gare) arg0.getAdapter().getItem(arg2);
                /*Toast.makeText(MainActivity.this,
                        "Clicked " + arg2 + " name: " + destination.getNom(),
                        Toast.LENGTH_SHORT).show();*/
            }
        });
        scanner.close();



        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                curr_latitude = location.getLatitude();
                curr_longitude = location.getLongitude();

                Log.e("Actuellement : ", curr_latitude + " " + curr_longitude);

                if ((destination.getLatitude() - 0.03) < curr_latitude && curr_latitude < (destination.getLatitude() + 0.03) && (destination.getLongitude() - 0.03) < curr_longitude && curr_longitude < (destination.getLongitude() + 0.03)) {

                    alerte = true;

                    tv.setText("Vous êtes arrivé ! ");
                    tv.setTextColor(Color.RED);
                    //blink();

                    b.setText("Arrêter l'alerte");

                    mVibrator.vibrate(pattern_vib, -1);

                } else {
                    //stop_blink();
                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };


        b.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                String s;
                String a = String.valueOf(et.getText());


                if (alerte) {

                    alerte = false;

                    mVibrator.cancel();
                    //textColorAnim.cancel();
                    tv.setTextColor(Color.BLACK);
                    tv.setText("");
                    b.setText("Valider");

                    locationManager.removeUpdates(locationListener);

                } else {


                        //s = readLine(a);

                        //String[] tab = s.split(";");

                        /*dest_longitude = Double.parseDouble(tab[1]);
                        dest_latitude = Double.parseDouble(tab[2]);*/

                        tv.setText("L'alarme enclenchée avec pour gare de destination " + destination.getNom());

                        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    Activity#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for Activity#requestPermissions for more details.
                            return;
                        }
                        locationManager.requestLocationUpdates("gps", 1000, 0, locationListener);


                }



            }
        });


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET
                }, 10);

                return;
            } else {
                //configureButton();
            }
        }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    //configureButton();
                    return;
        }
    }



    private void blink(){
        textColorAnim = ObjectAnimator.ofInt(tv, "textColor", Color.RED, Color.TRANSPARENT);
        textColorAnim.setDuration(1000);
        textColorAnim.setEvaluator(new ArgbEvaluator());
        textColorAnim.setRepeatCount(ValueAnimator.INFINITE);
        textColorAnim.setRepeatMode(ValueAnimator.REVERSE);
        textColorAnim.start();
    }

    private void stop_blink(){
        if(textColorAnim != null) {
            textColorAnim.cancel();
            tv.setTextColor(Color.BLACK);
        }
    }



}
