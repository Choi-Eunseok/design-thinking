package com.example.ponyo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MemeActivity extends AppCompatActivity {

    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    ArrayAdapter<String> arrayAdapter;

    static ArrayList<String> arrayIndex =  new ArrayList<String>();
    static ArrayList<String> arrayData = new ArrayList<String>();

    private DatabaseReference databaseReference = database.getReference();

    Button btn;
    EditText edit1, edit2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme);

        btn = findViewById(R.id.addBtn);
        edit1 = findViewById(R.id.edit1);
        edit2 = findViewById(R.id.edit2);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addMeme(edit1.getText().toString(),edit2.getText().toString());
            }
        });

        Button UpdateBtn = findViewById(R.id.updateBtn);
        UpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFirebaseDatabase();
            }
        });

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.db_list_view);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                Log.d("Long Click", "position = " + position);
                String viewData = arrayData.get(position);
                AlertDialog.Builder dialog = new AlertDialog.Builder(MemeActivity.this);
                dialog.setTitle("데이터 삭제")
                        .setMessage("해당 데이터를 삭제 하시겠습니까?" + "\n" + viewData)
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                databaseReference.child("meme").child(arrayIndex.get(position)).setValue(null);

                                getFirebaseDatabase();
                                Toast.makeText(MemeActivity.this, "데이터를 삭제했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(MemeActivity.this, "삭제를 취소했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .create()
                        .show();
                return false;
            }
        });

        getFirebaseDatabase();
    }


    public void addMeme(String ko, String en) {
        Meme meme = new Meme(ko,en);
        getFirebaseDatabase();
        databaseReference.child("meme").child(String.valueOf(Integer.parseInt(arrayIndex.get(arrayIndex.size() - 1))+1)).setValue(meme);
        edit1.setText("");
        edit2.setText("");
    }

    public void getFirebaseDatabase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("getFirebaseDatabase", "key: " + dataSnapshot.getChildrenCount());
                arrayData.clear();
                arrayIndex.clear();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    Meme get = postSnapshot.getValue(Meme.class);
                    String[] info = {get.ko, get.en};
                    String Result = "KO : " + info[0] + " | EN : " + info[1];
                    arrayData.add(Result);
                    arrayIndex.add(key);
                    Log.d("getFirebaseDatabase", "key: " + key);
                    Log.d("getFirebaseDatabase", "info: " + info[0] + info[1]);
                }
                arrayAdapter.clear();
                arrayAdapter.addAll(arrayData);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("getFirebaseDatabase","loadPost:onCancelled", databaseError.toException());
            }
        };
        Query query = FirebaseDatabase.getInstance().getReference().child("meme");
        query.addListenerForSingleValueEvent(postListener);
    }

    @Override
    protected void onUserLeaveHint() {          // 홈 버튼 감지
        super.onUserLeaveHint();
        System.exit(0);
    }

}