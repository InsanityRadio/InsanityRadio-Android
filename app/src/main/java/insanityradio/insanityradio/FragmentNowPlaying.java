package insanityradio.insanityradio;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import co.mobiwise.library.RadioListener;
import co.mobiwise.library.RadioManager;

public class FragmentNowPlaying extends Fragment implements RadioListener {
    private static FragmentNowPlaying instance;

    private RadioManager radioManager;
    private HashMap<String, String> currentShow;
    private TextView currentShowTextView;
    private TextView nowPlayingTextView;
    private ImageView albumArtImageView;

    public static FragmentNowPlaying getInstance() {
        if (instance == null) {
            instance = new FragmentNowPlaying();
        }

        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);

        radioManager = RadioManager.with(getActivity());
        radioManager.connect();
        radioManager.registerListener(this);

        View view = inflater.inflate(R.layout.fragment_nowplaying, container, false);

        currentShowTextView = (TextView) view.findViewById(R.id.current_show);
        nowPlayingTextView = (TextView) view.findViewById(R.id.now_playing);
        albumArtImageView = (ImageView) view.findViewById(R.id.album_art);

        updateCurrentShow();

        return view;
    }

    public void updatePlayer() {
        updateCurrentShow();

        HashMap<String, String> nowPlaying = DataModel.getNowPlaying(getActivity());

        String nowPlayingArtist = nowPlaying.get("artist");
        String nowPlayingSong = nowPlaying.get("song");

        nowPlayingTextView.setText(nowPlayingArtist + "\n" + nowPlayingSong);

        String url = "http://ws.audioscrobbler.com/2.0/?method=track.getinfo&api_key=38ca8452a5704df8ba7e7de9855844e7&artist=" + nowPlayingArtist + "&track=" + nowPlayingSong + "&format=json";
        url = url.replaceAll(" ", "%20");

        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                updateImageWithResponse(jsonObject);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                displayCurrentShowImage();
            }
        });
        objectRequest.setRetryPolicy(DataModel.getRetryPolicy());

        VolleySingleton.getInstance(getActivity()).getRequestQueue().add(objectRequest);
    }

    private void updateCurrentShow() {
        currentShow = DataModel.getCurrentShow(getActivity());

        String currentShowTextViewText = currentShow.get("name");
        String currentShowPresenters = currentShow.get("presenters");

        if (!currentShowPresenters.equals("")) {
            currentShowTextViewText += "\nwith " + currentShowPresenters;
        }

        currentShowTextView.setText(currentShowTextViewText);
    }

    private void updateImageWithResponse(JSONObject jsonObject) {
        try {
            JSONObject track = (JSONObject) jsonObject.get("track");
            JSONObject album = (JSONObject) track.get("album");

            JSONArray images = (JSONArray) album.get("image");

            for (int i = 0; i < images.length(); i++) {
                JSONObject image = (JSONObject) images.get(i);

                if (image.get("size").equals("extralarge")) {
                    updateImageWithURL((String) image.get("#text"));

                    return;
                }
            }
        } catch (JSONException | ClassCastException e) {
            e.printStackTrace();
        }

        displayCurrentShowImage();
    }

    private void updateImageWithURL(String imageURL) {
        ImageRequest imageRequest = new ImageRequest(imageURL, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                displayFinalImage(bitmap);
            }
        }, 0, 0, null, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                displayCurrentShowImage();
            }
        });
        imageRequest.setRetryPolicy(DataModel.getRetryPolicy());

        VolleySingleton.getInstance(getActivity()).getRequestQueue().add(imageRequest);
    }

    private void displayCurrentShowImage() {
        ImageRequest imageRequest = new ImageRequest(currentShow.get("imageURL"), new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap bitmap) {
                displayFinalImage(bitmap);
            }
        }, 0, 0, null, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                displayFinalImage(BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.insanity_icon));
            }
        });
        imageRequest.setRetryPolicy(DataModel.getRetryPolicy());

        VolleySingleton.getInstance(getActivity()).getRequestQueue().add(imageRequest);
    }

    private void displayFinalImage(Bitmap bitmap) {
        albumArtImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onRadioConnected() {
        radioManager.startRadio("http://stream.insanityradio.com:8000/insanity320.mp3");
    }

    @Override
    public void onRadioStarted() {

    }

    @Override
    public void onRadioStopped() {

    }

    @Override
    public void onMetaDataReceived(String s, String s1) {
        DataModel.updateData(getActivity());
    }
}
