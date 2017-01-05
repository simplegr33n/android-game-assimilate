package ca.ggolda.android_game_assimilate;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;


/**
 * Created by gcgol on 12/22/2016.
 */

public class LobbyActivity extends AppCompatActivity {

    private FirebaseAuth.AuthStateListener authListener;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mGamesDatabaseReference;
    private DatabaseReference mUsersDatabaseReference;

    private DatabaseReference mActiveDatabaseReference;
    private ChildEventListener mActiveEventListener;

    private TextView mActiveCountTextview;

    private AdapterActive mAdapterActive;
    private ListView mListViewActive;

    public static String userId;
    private String username;
    private ArrayList<InstanceGame> games;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);


        games = new ArrayList<>();


        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mGamesDatabaseReference = mFirebaseDatabase.getReference().child("games");
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");

        final EditText editUsername = (EditText) findViewById(R.id.username_edittext);


        mActiveCountTextview = (TextView) findViewById(R.id.active_count);

        //get current user and send to login screen if user is null
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user.getUid();

        Log.e("USER", "" + userId);


        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // user auth state is changed - user is null
                    // launch login activity
                    startActivity(new Intent(LobbyActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };


        // Get username
        mUsersDatabaseReference.child(userId).child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                username = dataSnapshot.getValue(String.class);

                TextView usernameBar = (TextView) findViewById(R.id.username_left);
                usernameBar.setText(username);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        TextView setUsername = (TextView) findViewById(R.id.set_username);
        setUsername.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                username = editUsername.getText().toString();
                Log.e("GAMEEE", "" + username);
                if (username != null) {
                    mUsersDatabaseReference.child(userId).child("username").setValue(username);

                    TextView usernameBar = (TextView) findViewById(R.id.username_left);
                    usernameBar.setText(username);

                }

            }


        });

        // Ensure user has username
        mUsersDatabaseReference.child(userId).child("username").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Log.e("USER", "GAME2uname " + dataSnapshot.getValue());

                if (dataSnapshot.getValue() == null) {
                    LinearLayout usernameLayout = (LinearLayout) findViewById(R.id.username_layout);
                    usernameLayout.setVisibility(View.VISIBLE);

                }

                if (dataSnapshot.getValue() != null) {
                    LinearLayout usernameLayout = (LinearLayout) findViewById(R.id.username_layout);
                    usernameLayout.setVisibility(View.GONE);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });


        //create a random game offer
        TextView matchRandom = (TextView) findViewById(R.id.random_button);
        matchRandom.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                mGamesDatabaseReference.child("offers").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {


                        // TODO: just get game key rather than splitting {akdffkadfjsdkfdf=true} strings
                        String temp = ("" + dataSnapshot.getValue()).split("=")[0];

                        Log.e("USER", "temp" + temp);

                        //if offer/accept offer
                        // TODO: possibly allow offers to hold multiple offer games
                        if (dataSnapshot.getValue() != null) {

                            //TODO: remove stringbuilder
                            StringBuilder sb = new StringBuilder(temp);
                            sb.deleteCharAt(0);
                            final String offer = sb.toString();
                            Log.e("USEROH", "offer" + offer);

                            mGamesDatabaseReference.child(offer).child("white").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String player_white = dataSnapshot.getValue(String.class);

                                    Log.e("USER", "player_white " + player_white);

                                    if ((player_white != null) && player_white.equals(userId)) {
                                        Toast.makeText(LobbyActivity.this, "Please Wait...", Toast.LENGTH_SHORT).show();

                                    } else {
                                        // TODO: something to ensure these all happen
                                        mGamesDatabaseReference.child(offer).child("black").setValue(userId);
                                        mUsersDatabaseReference.child(userId).child("games").child(offer).setValue(true);
                                        mGamesDatabaseReference.child(offer).child("username_black").setValue(username);
                                        mGamesDatabaseReference.child("offers").removeValue();

                                        // go to game
                                        Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                                        intent.putExtra("MATCH_ID", offer);
                                        startActivity(intent);
                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    System.out.println("The read failed: " + databaseError.getCode());
                                }
                            });

                        } else {
                            String eventId = mGamesDatabaseReference.push().getKey();

                            //TODO: ensure these all happen
                            mGamesDatabaseReference.child(eventId).child("white").setValue(userId);
                            mGamesDatabaseReference.child(eventId).child("turn_color").setValue("white");
                            mUsersDatabaseReference.child(userId).child("games").child(eventId).setValue(true);
                            mGamesDatabaseReference.child("offers").child(eventId).setValue(true);
                            mGamesDatabaseReference.child(eventId).child("match_id").setValue(eventId);
                            mGamesDatabaseReference.child(eventId).child("username_white").setValue(username);

                            // go to game
                            Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
                            intent.putExtra("MATCH_ID", eventId);
                            startActivity(intent);
                        }


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        System.out.println("The read failed: " + databaseError.getCode());
                    }
                });


            }
        });


        Log.e("USERBEFORE", "" + userId);

        //get current user and send to login screen if user is null
        // IS this not redundant? TODO: Find out . lol
        userId = user.getUid();

        Log.e("USERAFTER", "" + userId);


        games = new ArrayList<>();
        mAdapterActive = new AdapterActive(LobbyActivity.this, R.layout.card_game, games);
        mListViewActive = (ListView) findViewById(R.id.active_listview);
        mListViewActive.setAdapter(mAdapterActive);


    }


    @Override
    protected void onResume() {
        super.onResume();

        setUpAdapterActive();

    }


    @Override
    protected void onPause() {
        super.onPause();

        mActiveDatabaseReference.removeEventListener(mActiveEventListener);

        mAdapterActive.clear();

        games.clear();

    }


    private void setUpAdapterActive() {

        mAdapterActive.clear();

        games.clear();


        mActiveDatabaseReference = mUsersDatabaseReference
                .child(userId).child("games");

        mActiveEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {

                Log.e("GAME", "added: " + dataSnapshot.getKey());

                mGamesDatabaseReference.child(dataSnapshot.getKey()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {


                        // only show games if there is a white and a black player
                        if (((dataSnapshot.getValue(InstanceGame.class).getBlack()) != null) && ((dataSnapshot.getValue(InstanceGame.class).getWhite()) != null)) {


                            int temp = 1;

                            // if new entry a double of another match id, replace the older entry, otherwise add new
                            for (int i = 0; i < games.size(); i++) {
                                if (games.get(i).getMatch_id().equals(dataSnapshot.getValue(InstanceGame.class).getMatch_id())) {
                                    games.set(i, dataSnapshot.getValue(InstanceGame.class));
                                    temp = 0;
                                }
                            }

                            if (temp != 0) {
                                mAdapterActive.add(dataSnapshot.getValue(InstanceGame.class));
                            } else {
                                mAdapterActive.notifyDataSetChanged();
                            }



                            if (games.size() > 0) {
                                mActiveCountTextview.setVisibility(View.VISIBLE);
                                mActiveCountTextview.setText("Active Games " + "(" + games.size() + ")");

                            } else if (games.size() == 0) {

                                mActiveCountTextview.setVisibility(View.GONE);

                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {

//              // TODO: probably remove, I don't think this does anything and I don't think I want it // TODO: 01/03/2017
                Log.e("GAME", "Changed: " + dataSnapshot.getKey());
//                Intent intent = new Intent(LobbyActivity.this, LobbyActivity.class);
//                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
//                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                startActivity(intent);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

                Log.e("GAME", "Removed: " + dataSnapshot.getKey());

                mAdapterActive.clear();
                setUpAdapterActive();

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        mActiveDatabaseReference.addChildEventListener(mActiveEventListener);
    }


}
