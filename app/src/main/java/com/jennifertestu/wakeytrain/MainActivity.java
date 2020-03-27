package com.jennifertestu.wakeytrain;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

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

    Gare destination = null;

    Boolean alerte = false;

    private BroadcastReceiver broadcastReceiver;

    // Permet d'être à l'écoute de LocalisationService pour savoir quand on est arrivé
    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, final Intent intent) {

                    Log.e("Activity : ","\n" +intent.getExtras().get("arrive"));

                    // Si on recoit le message "arrive"
                    if(intent!=null && intent.getExtras().getBoolean("arrive")==true){
                        alerte = true; // ?
                        // On dit dans l'activité que l'on est arrivé
                        tv.setText("Vous êtes arrivé ! ");
                        tv.setTextColor(Color.RED);
                        //blink();
                        // Le bouton change pour pouvoir arreter l'alarme
                        b.setText("Arrêter l'alerte");


                    }else {

                    }

                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // On demande les autorisations necessaires au bon fonctionnement de l'application, c'est a dire ici la localisation
        autorisations();

        // On récupére dans le fichier XML tous les composants de l'interface graphique
        tv = (TextView) findViewById(R.id.Viewer);
        et = (AutoCompleteTextView) findViewById(R.id.editText);
        b = (Button) findViewById(R.id.button);
        b_help = (ImageButton) findViewById(R.id.helpButton);

        // Le clique sur le bouton "?" fait apparaitre une popup afin d'expliquer le fonctionnement de l'application
        b_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder alerte = new AlertDialog.Builder(MainActivity.this);
                alerte.setTitle("Comment utiliser l'application ?");
                alerte.setMessage("- Entrez le début du nom de votre gare \n" +
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

        // On ouvre le fichier CSV contenant toutes les gares et leurs coordonnées
        Scanner scanner = new Scanner(getResources().openRawResource(R.raw.gare));
        List<Gare> listeGares = new ArrayList<Gare>();
        // On parcourt le fichier CSV
        while (scanner.hasNext()) {
            // On découpe chaque ligne selon la position du ;
            String[] ligne = scanner.nextLine().split(";");
            // La 1ere chaine de caracteres désigne le nom de la gare
            String nom = ligne[0];
            // La 2eme chaine de caracteres désigne la longitude de la gare
            double longitude = Double.valueOf(ligne[1]);
            // La 3eme chaine de caracteres désigne la la latitude de la gare
            double latitude = Double.valueOf(ligne[2]);

            // A partir des informations collectées, on crée une gare que l'on ajoute à une liste de gares
            listeGares.add(new Gare(nom,longitude,latitude));
        }

        // ArrayAdapter va nous permettre d'afficher la liste de gares pour l'autocompletion, ici un ArrayAdapter personnalisé est appelé afin de gérer les caractères à accent
        ArrayAdapterAccents<Gare> adapter =
                new ArrayAdapterAccents<Gare>(this, android.R.layout.simple_list_item_1, listeGares );
        et.setAdapter(adapter);
        et.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                destination = (Gare) arg0.getAdapter().getItem(arg2);

            }
        });
        scanner.close();

        // Appel de la méthode pour la création des événements du bouton selon differentes situations
        action_bouton(this.getApplication());


    }

    // Méthode pour pour les actions a effectuer selon la situation
    public void action_bouton(final Application app){
        b.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                // Préparation de la communication avec le composant LocalisationService
                Intent i =new Intent(getApplicationContext(),LocalisationService.class);

                // Si l'alerte est true quand on appuie sur le bouton, c'est à dire qu'avant le clique on est à l'écoute de la destination et on souhaite arreter
                if (alerte) {
                    // Le boolean alerte passe à false pour signifier qu'on est plus à l'écoute
                    alerte = false;

                    // On enlève le texte du textview et on change le texte du bouton
                    tv.setTextColor(Color.parseColor("#7A7A7A"));
                    tv.setText("");
                    b.setText("Valider");
                    // On enlève le texte de l'autocomplete et le champs est de nouveau activé
                    et.setText("");
                    et.setEnabled(true);
                    // On arrete LocalisationService
                    stopService(i);

                // Si l'alerte est false, c'est à dire qu'avant le clique on est pas à l'écoute d'une destination
                } else {
                    // Si l'utilisateur n'a sélectionné aucune destination valide
                    if(destination == null){
                        // On renvoie un message d'erreur
                        tv.setText("Cette gare n'existe pas. Veuillez sélectionner une gare valide parmi les propositions.");
                    // Sinon
                    }else {
                        // Le boolean alerte passe à true pour signifier qu'on est à l'écoute
                        alerte = true;
                        // On envoie la gare de destination à LocalisationService
                        i.putExtra("Gare", destination);
                        // On démarre LocalisationService
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            app.startForegroundService(i);
                        }else{
                            startService(i);
                        }

                        // Le champs de l'autocomplete n'est plus actif
                        et.setEnabled(false);
                        // On affiche un message pour indiquer qu'on est à l'écoute de la gare selectionnée
                        tv.setText("L'alarme enclenchée avec pour gare de destination " + destination.getNom());
                        // On change le texte du bouton
                        b.setText("Annuler");

                    }
                }



            }
        });
    }

/*
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    //configureButton();
                    return;
        }
    }
*/

    // Méthode pour demander l'autorisation de localisation et désactiver l'économie de batterie sur cette application
    private void autorisations(){
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        Intent intent = new Intent();
        String packageName = this.getPackageName();
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }




    /* Methodes pour animer le texte, non utilisées */

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
