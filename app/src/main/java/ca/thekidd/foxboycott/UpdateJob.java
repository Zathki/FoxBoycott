package ca.thekidd.foxboycott;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;

public class UpdateJob extends Worker {

    private static final String TAG = "FoxBoycott";
    private List<String> foxAdvertisers;
    private PlaceDetectionClient mPlaceDetectionClient;
    private SharedPreferences prefs;

    public UpdateJob() {
        super();

        //don't cache
        FirebaseRemoteConfigSettings configSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(BuildConfig.DEBUG)
                        .build();
        FirebaseRemoteConfig.getInstance().setConfigSettings(configSettings);

        String advertisersString = FirebaseRemoteConfig.getInstance().getString("advertisers");
        foxAdvertisers = Arrays.asList(advertisersString.split(",", -1));
    }

    public static void start() {

        PeriodicWorkRequest.Builder updateJobBuilder =
                new PeriodicWorkRequest.Builder(UpdateJob.class, 10,
                        TimeUnit.MINUTES);
        PeriodicWorkRequest updateWork = updateJobBuilder.build();
        WorkManager workManager = WorkManager.getInstance();
        assert (workManager != null);
        workManager.enqueueUniquePeriodicWork("update", ExistingPeriodicWorkPolicy.REPLACE, updateWork);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(mPlaceDetectionClient == null)
            mPlaceDetectionClient = Places.getPlaceDetectionClient(getApplicationContext());
        if(prefs == null)
            prefs = getApplicationContext().getSharedPreferences("ignore", Context.MODE_PRIVATE);

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.FAILURE;
        }

        Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i(TAG, String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                    checkIfFoxAdvertiser(placeLikelihood.getPlace());
                }
                likelyPlaces.release();
            }
        });

        return Result.SUCCESS;
    }

    private void checkIfFoxAdvertiser(Place place) {
        String placeName = place.getName().toString();
        String placeID = place.getId();
        for(String advertiser : foxAdvertisers) {
            if (!advertiser.isEmpty() && placeName.contains(advertiser) && !prefs.contains(placeID)) {
                showSimpleNotification(getApplicationContext(), "FoxBoycott - " + advertiser, "A nearby location appears to be a Fox News sponsor.", place.getId(), (int)(Math.random()*10000));
            }
        }
    }

    private static void showSimpleNotification(Context c, String title, String text, String placeId, int id) {
        Intent ignoreIntent = new Intent(c, NotificationIgnoreReceiver.class);
        ignoreIntent.putExtra("placeID", placeId);
        ignoreIntent.putExtra("notificationID", id);
        PendingIntent ignorePendingIntent = PendingIntent.getBroadcast(c, 0, ignoreIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent = new Intent(c, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, "notify_001")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_cancel_black_24dp,"Ignore", ignorePendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        assert(notificationManager != null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("notify_001",
                    "FoxBoycott",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }
}