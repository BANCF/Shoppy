package com.example.shoppy.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.shoppy.R;
import com.example.shoppy.adapter.ChatAdapter;
import com.example.shoppy.model.ChatMessage;
import com.example.shoppy.ultils.Ultils;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView imgChat;
    EditText edtMess;
    FirebaseFirestore db;
    ChatAdapter adapter;
    List<ChatMessage> listChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initView();
        initControl();
        listenMess();
        insertUser();
    }

    private void insertUser() {
        HashMap<String, Object> user = new HashMap<>();
        user.put("id",Ultils.user_current.getId());
        user.put("username",Ultils.user_current.getUsername());
        db.collection("users").document(String.valueOf(Ultils.user_current.getId())).set(user);

    }

    private void initControl() {
        imgChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessToFire();
            }
        });
    }

    private void sendMessToFire() {
        String str_mess = edtMess.getText().toString().trim();
        if(TextUtils.isEmpty(str_mess)){

        }else {
            HashMap<String,Object> message = new HashMap<>();
            message.put(Ultils.SENDID,String.valueOf(Ultils.user_current.getId()));
            message.put(Ultils.RECEIVEDID,Ultils.ID_RECEIVED);
            message.put(Ultils.MESS,str_mess);
            message.put(Ultils.DATETIME,new Date());
            db.collection(Ultils.PATH_CHAT).add(message);
            edtMess.setText("");

        }
    }

    //so sanh lay data ve
    private void listenMess(){
        db.collection(Ultils.PATH_CHAT)
                .whereEqualTo(Ultils.SENDID,String.valueOf(Ultils.user_current.getId()))
                .whereEqualTo(Ultils.RECEIVEDID,Ultils.ID_RECEIVED)
                .addSnapshotListener(eventListener);
        //so sanh nguoc lai: id 11-> 12 || 12-> 11
        db.collection(Ultils.PATH_CHAT)
                .whereEqualTo(Ultils.SENDID,Ultils.ID_RECEIVED)
                .whereEqualTo(Ultils.RECEIVEDID,String.valueOf(Ultils.user_current.getId()))
                .addSnapshotListener(eventListener);
    }
    //Lay data tren firestore add vao list -> nem vao Adapter -> set cho RecycleView
    private final EventListener<QuerySnapshot> eventListener = (value, error)->{
        if(error!=null){
            return;
        }
        if(value!=null){
            int count = listChat.size();
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.sendid = documentChange.getDocument().getString(Ultils.SENDID);
                    chatMessage.receivedid = documentChange.getDocument().getString(Ultils.RECEIVEDID);
                    chatMessage.mess = documentChange.getDocument().getString(Ultils.MESS);
                    chatMessage.dateObj = documentChange.getDocument().getDate(Ultils.DATETIME);
                    chatMessage.datetime = format_date(documentChange.getDocument().getDate(Ultils.DATETIME));
                    listChat.add(chatMessage);
                }
            }
            Collections.sort(listChat,(obj1, obj2)->obj1.dateObj.compareTo(obj2.dateObj));
            if(count == 0){
                adapter.notifyDataSetChanged();
            }else {
                adapter.notifyItemRangeInserted(listChat.size(),listChat.size());
                recyclerView.smoothScrollToPosition(listChat.size()-1);
            }
        }

    };

    private String format_date(Date date){
        return new SimpleDateFormat("MMMM dd, yyyy- hh:mm a", Locale.getDefault()).format(date);
    }

    private void initView() {
        listChat = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycleChat);
        imgChat = findViewById(R.id.imgChat);
        edtMess = findViewById(R.id.edtInputText);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        adapter = new ChatAdapter(getApplicationContext(),listChat,String.valueOf(Ultils.user_current.getId()));
        recyclerView.setAdapter(adapter);
    }
}