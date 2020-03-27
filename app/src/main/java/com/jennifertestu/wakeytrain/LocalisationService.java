package com.jennifertestu.wakeytrain;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;


public class LocalisationService extends Service {

    private LocationListener listener;
    private LocationManager locationManager;

    //HandlerThread handlerThread;
    Looper looper;

    Vibrator mVibrator;
    long[] pattern_vib = {0, 200, 500, 1000, 200, 500, 1000, 200, 500, 1000, 200, 500, 1000, 200, 500, 1000, 200, 500, 1000};

    Gare gare;

    public static final String CHANNEL_ID = "WakeyTrainService";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // A la création du service
    @Override
    public void onCreate() {

        super.onCreate();

        // Creation du gestionnaire de position
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        // Creation du service de vibration
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    }

    // Au lancement du service, ou à sa relance
    @RequiresApi(api = Build.VERSION_CODES.M)
    public int onStartCommand(Intent intent, int flags, int startId) {

        // On récupére la gare de destination qui a été envoyé par MainActivity
        Bundle extras = intent.getExtras();
        gare = (Gare) extras.getSerializable("Gare");

        Log.e("Destination : ", gare.toString());

        // On crée le canal de notification qui servira a communiquer avec l'utilisateur
        createNotificationChannel();

        // On envoie une notification statique pour avertir du fonctionnement du service en arrière plan
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wakey Train")
                .setSmallIcon(R.drawable.ic_notif)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);


        // Définition de l'écoute
        listener = new LocationListener() {

            // Méthode appelée si la localisation a changé
            @Override
            public void onLocationChanged(Location location) {

                Log.e("Dans le service ? ", location.getLongitude() + " " + location.getLatitude());

                // Si la localisation est < à celle de la gare de destination
                if ((gare.getLatitude() - 0.03) < location.getLatitude() && location.getLatitude() < (gare.getLatitude() + 0.03) && (gare.getLongitude() - 0.03) < location.getLongitude() && location.getLongitude() < (gare.getLongitude() + 0.03)) {

                    Log.e("Arrive ? ", "oui");

                    // On envoie le message "arrive" a MainActivity
                    Intent i = new Intent("location_update");
                    i.putExtra("arrive", true);
                    sendBroadcast(i);

                    // On envoie une notification d'arrivée
                    arriveNotification();
                    // On arrete l'écoute de la localisation
                    locationManager.removeUpdates(listener);


                } else {
                    Log.e("Arrive ? ", "non");
                }

            }

            // Methode appelée quand il y a un changement de fournisseur de localisation
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            // Methode appelée quand le fournisseur de localisation est activé
            @Override
            public void onProviderEnabled(String s) {

            }

            // Methode appelée quand le fournisseur de localisation est désactivé
            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

/*
        Log.e(TAG, "creating handlerthread and looper");
        handlerThread = new HandlerThread("MyHandlerThread");
        handlerThread.start();
        looper = handlerThread.getLooper();
*/
        // Si les autorisations le permettent
        if (checkLocationPermission()) {
            // On active la localisation via le GPS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,listener, looper);
            return START_STICKY;
        }

        return START_STICKY;
    }

    // Méthode appelée à la destruction du service
    @Override
    public void onDestroy() {
        super.onDestroy();
        // On désactive le gestionnaire de localisation
        if(locationManager != null){
            locationManager.removeUpdates(listener);
        }
        // On désactive la vibration
        mVibrator.cancel();
        // On arrete le service et ses notifications
        this.stopForeground(true);
        this.stopSelf();

    }

    // Méthode pour la vérification de l'autorisation d'accès a la localisation
    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    // Méthode pour la création du canal de notification
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Wakey Train",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    // Méthode pour la création d'une notification lors de l'arrivée a la gare de destination
    private void arriveNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Wakey Train")
                .setContentText("Vous êtes arrivé ! ")
                .setSmallIcon(R.drawable.ic_notif)
                .setContentIntent(pendingIntent)
                .build();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification);

        // On fait vibrer le téléphone selon un certain pattern
        mVibrator.vibrate(pattern_vib, 1);

    }

    // Méthode appelée quand l'appli est stoppée
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("onTaskRemoved invoqué");
        super.onTaskRemoved(rootIntent);

        // On désactive la vibration
        mVibrator.cancel();
        // On arrete le service et ses notifications
        this.stopForeground(true);
        this.stopSelf();
    }
}