package com.example.mochomework2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

import android.content.Context;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mochomework2.utitlities.MagicCard;
import com.example.mochomework2.utitlities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private static final int LOADER_ID = 69;
    private static final int FIRST_PAGE = 1;

    private static final String CONTENT_KEY = "content";
    private static final String PAGE_KEY = "page";

    private static int page = FIRST_PAGE;

    private Button loadBtn;
    private TextView resultTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadBtn = findViewById(R.id.btn_load);
        resultTv = findViewById(R.id.tv_result);

        loadBtn.setOnClickListener(v -> {
            resultTv.setText("");
            getData();
        });

        if(savedInstanceState != null) {
            resultTv.setText(savedInstanceState.getString(CONTENT_KEY));
            page = savedInstanceState.getInt(PAGE_KEY);
        }
    }

    private void getData() {
        Bundle queryBundle = new Bundle();

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> apiLoader = loaderManager.getLoader(LOADER_ID);

        if(apiLoader == null){
            loaderManager.initLoader(LOADER_ID, queryBundle, this);
        } else{
            loaderManager.restartLoader(LOADER_ID, queryBundle, this);
        }

        page++;
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {

        return new AsyncTaskLoader<String>(this) {

            String mJsonData;

            @Override
            public void onStartLoading() {

                if (mJsonData != null) {
                    deliverResult(mJsonData);
                } else {
                    loadBtn.setEnabled(false);
                    forceLoad();
                }
            }

            @Nullable
            @Override
            public String loadInBackground() {

                URL apiUrl = NetworkUtils.buildUrl(page);

                try {
                    return NetworkUtils.getResponseFromHttpUrl(apiUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public void deliverResult(@Nullable String data) {
                mJsonData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {

        loadBtn.setEnabled(true);

        if (null == data) {
            showErrorMessage();
        } else {
            loadData(data);
        }

    }

    private void showErrorMessage() {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.load_error_msg), Toast.LENGTH_SHORT);
        toast.show();
    }

    private void loadData(String data) {

        try {

            JSONArray jsonArray = new JSONObject(data).getJSONArray("cards");

            if(jsonArray.length() == 0) {
                page = FIRST_PAGE;
                getData();
            }

            List<MagicCard> cardList = new LinkedList<MagicCard>();

            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonElement = jsonArray.getJSONObject(i);

                MagicCard card = new MagicCard(
                        jsonElement.getString("name"),
                        jsonElement.getString("type"),
                        jsonElement.getString("rarity")
                );

                JSONArray jsonColors = jsonElement.getJSONArray("colors");

                for(int j = 0; j < jsonColors.length(); j++) {
                    card.addColor(jsonColors.getString(j));
                }

                cardList.add(card);
            }

            Collections.sort(cardList);

            for(MagicCard card : cardList) {

                StringBuilder stringBuilder = new StringBuilder();

                for(String color : card.getColors()) {
                    stringBuilder.append(color);
                }


                resultTv.append(String.format("%s: %s, %s, %s \n",
                        card.getName(),
                        card.getType(),
                        card.getRarity(),
                        stringBuilder.toString()));
            }

        } catch (JSONException e) {
            showErrorMessage();
            e.printStackTrace();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(CONTENT_KEY, resultTv.getText().toString());
        outState.putInt(PAGE_KEY, page);
    }
}
