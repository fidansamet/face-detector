package net.fidansamet.facedetector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private static List<PersonLine> personLineList;
    private static List<PersonLine> personsList = new ArrayList<>();
    TextView textView, searching;
    int dot_counter = 0;
    String update_searching;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.no_item_id);
        searching = findViewById(R.id.searching_view);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        recyclerViewAdapter = new RecyclerViewAdapter(getBaseContext(), personLineList);
        recyclerView.setAdapter(recyclerViewAdapter);
        update_searching =  getString(R.string.searching);


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dot_counter%4 == 0) {
                                    update_searching = getString(R.string.searching);
                                    dot_counter++;
                                } else {
                                    update_searching = update_searching + ".";
                                    dot_counter++;
                                }
                                searching.setText(update_searching);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        thread.start();
    }

    public void updateList(){

        Collections.reverse(personLineList);
        personsList.clear();
        personsList.addAll(personLineList);
        Collections.reverse(personLineList);

        if (personsList.size() == 0){

            textView.setVisibility(View.VISIBLE);

        } else {

            textView.setVisibility(View.GONE);
            recyclerViewAdapter.setPeopleList(personsList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
            recyclerView.setAdapter(recyclerViewAdapter);
        }
    }
}
