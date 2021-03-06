package com.fragments;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.app_folder.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.models.DataCache;
import com.models.User;
import com.util.ByteArrayUtils;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import com.models.Beacon;
import com.models.DataCache;

import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment {

    protected LocationManager locationManager;
    protected CurrentBeaconFragment currentBeaconFragment;

    ImageView radarPulse;
    ImageView loading;
    TextView distanceText;
    SeekBar distance;
    Timer timer;
    AnimatedVectorDrawable loadingAnimation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        //Initialize view
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        //Initialize map fragment
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);

        radarPulse = view.findViewById(R.id.radarSweeper);
        distance = view.findViewById(R.id.distance);
        distanceText = view.findViewById(R.id.distance_text);
        radarPulse.setVisibility(View.INVISIBLE);
        distance.setVisibility(View.INVISIBLE);
        distanceText.setAlpha(0);
        Button button = (Button) getActivity().findViewById(R.id.create_beacon_button);
        button.setVisibility(View.INVISIBLE);

        loading = view.findViewById(R.id.loading);
        loading.setBackgroundResource(R.drawable.loading);
        loadingAnimation = (AnimatedVectorDrawable) loading.getBackground();
        loadingAnimation.start();

        DataCache dataCache = DataCache.getInstance();
        User user = dataCache.getUser();
        LatLng currLocation = new LatLng(dataCache.locationData.getLatitude(), dataCache.locationData.getLongitude());

        timer = new Timer();
        // Set the schedule function
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {
                                          // Magic here
                                          loadingAnimation.start();
                                      }
                                  },
                0, 3500);

        //Async map
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity().getApplicationContext(), R.raw.style_json));
                googleMap.getUiSettings().setScrollGesturesEnabled(false);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setZoomGesturesEnabled(false);


                if (dataCache.getCurrUserBeacon() != null) {
                    googleMap.addMarker(beaconToMarkerOptions(dataCache.getCurrUserBeacon()).icon(BitmapDescriptorFactory.fromBitmap(
                            createCustomMarker(getActivity(), R.drawable.generatedperson,"Manish"))));
                }
                distance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                        Beacon.maxRange = (progress + 1) * 100;
                        DataCache.getInstance().locationData.zoom = (progress/10) + 10;
                    }
                });



                googleMap.getUiSettings().setScrollGesturesEnabled(false);
                //When map is loaded
                DataCache cache = DataCache.getInstance();
                cache.locationData.setGoogleMap(googleMap);

                googleMap.setOnMarkerClickListener(
                        new GoogleMap.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(@NonNull Marker marker) {
                                Beacon target = getBeaconFromMarker(marker);
                                if (target == null) {
                                    return true;
                                }
                                DataCache.getInstance().currentBeacon = target;
                                FragmentManager manager = getActivity().getSupportFragmentManager();
                                if (currentBeaconFragment != null) {
                                    manager.beginTransaction().remove(currentBeaconFragment).commit();
                                }
                                currentBeaconFragment = new CurrentBeaconFragment();
                                manager.beginTransaction().add(R.id.navBar_current_beacon_container, currentBeaconFragment)
                                        .commit();
                                return true;    // true means the camera doesn't move. Which is what we always want.
                            }
                        }
                );
                googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(@NonNull LatLng latLng) {
                        if (currentBeaconFragment != null) {
                            FragmentManager manager = getActivity().getSupportFragmentManager();
                            manager.beginTransaction().remove(currentBeaconFragment).commit();
                        }
                    }
                });
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        timer.cancel();
                        loading.setVisibility(View.INVISIBLE);
                        radarPulse.setVisibility(View.VISIBLE);
                        distance.setVisibility(View.VISIBLE);
                        distanceText.setAlpha(1.0f);
                        Button button = (Button) getActivity().findViewById(R.id.create_beacon_button);
                        button.setVisibility(View.VISIBLE);
                        rotate(radarPulse, getActivity().getApplicationContext());
                    }
                });
            }
        });

        //Return View
        return view;
    }





    private void rotate(ImageView view, Context context) {

        RotateAnimation rotate = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        rotate.setInterpolator(new LinearInterpolator());
        rotate.setRepeatCount(Animation.INFINITE);



        rotate.setDuration(5000);

        view.startAnimation(rotate);
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }


        private BitmapDescriptor BitmapFromVector(Context context, int vectorResId) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private BitmapDescriptor BitmapFromByteArray(byte[] data) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return BitmapDescriptorFactory.fromBitmap(bmp);
    }



    public Beacon getBeaconFromMarker(Marker marker) {
        List<Beacon> beaconList = DataCache.getInstance().getBeaconList();
        for (Beacon beacon: beaconList) {
            if (beacon.getMarkerId().equals(marker.getId())) {
                return beacon;
            }
        }
        return null;
    }

    private MarkerOptions beaconToMarkerOptions(Beacon beacon) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(beacon.latitude, beacon.longitude));
        markerOptions.title(beacon.title);
        markerOptions.snippet(beacon.getDescription());
        return markerOptions;
    }

    public static Bitmap createCustomMarker(Context context, @DrawableRes int resource, String _name) {

        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.custom_marker_layout, null);

        CircleImageView markerImage = (CircleImageView) marker.findViewById(R.id.user_dp);
        markerImage.setImageResource(resource);
        TextView txt_name = (TextView)marker.findViewById(R.id.name);
        txt_name.setText(_name);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(), marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);

        return bitmap;
    }
}