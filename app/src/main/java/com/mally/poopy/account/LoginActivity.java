package com.mally.poopy.account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.mally.poopy.MainActivity;
import com.mally.poopy.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import xyz.hasnat.sweettoast.SweetToast;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    EditText et_id, et_password;
    Button btn_login;
    TextView tv_signup;
    private FirebaseAuth mLogin;

    private ProgressDialog progressDialog;
    private FirebaseFirestore db;

    //로그인 정보 저장 코드 2020.06.02 KBS
    private String saved_id;
    private String saved_pwd;
    private boolean saved_LoginData;
    private CheckBox checkBox;
    private SharedPreferences appData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //로그인 정보 저장 코드 2020.06.02 KBS
        //설정값 불러오기
        appData = getSharedPreferences("appData", MODE_PRIVATE);
        load();

        mLogin=FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        et_id=(EditText)findViewById(R.id.etId);
        et_password=(EditText)findViewById(R.id.etPassword);
        checkBox = (CheckBox)findViewById(R.id.checkBox);
        btn_login=(Button)findViewById(R.id.btnLogin);
        tv_signup=(TextView) findViewById(R.id.linkSingUp);
        progressDialog = new ProgressDialog(this);
        if(saved_LoginData){
            et_id.setText(saved_id);
            et_password.setText(saved_pwd);
            checkBox.setChecked(saved_LoginData);
        }
        tv_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToRegister();
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String stEmail=et_id.getText().toString();
                String stPassword = et_password.getText().toString();
                loginUserAccount(stEmail, stPassword);

            }
        });
    }
    // 추가코드
    private void loginUserAccount(String email, String password){
        if(TextUtils.isEmpty(email)){
            SweetToast.error(this, "Email is required");
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            SweetToast.error(this, "Your email is not valid.");
        } else if(TextUtils.isEmpty(password)){
            SweetToast.error(this, "Password is required");
        } else if (password.length() < 6){
            SweetToast.error(this, "May be your password had minimum 6 numbers of character.");
        } else {

            progressDialog.setMessage("Please wait...");
            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(false);

            loginUser(et_id.getText().toString(),et_password.getText().toString());
        }

    }
    private void loginUser(String email, String password){
        mLogin.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(task.isSuccessful()){
                            String currentUserId = mLogin.getCurrentUser().getUid();
                            checkVerifiedEmail();
                            save();

                        }
                        else{
                            SweetToast.error(LoginActivity.this, "Your email and password may be incorrect. Please check & try again.");
                        }
                        progressDialog.dismiss();
                    }
                });
    }
    private void SendUserToRegister() {
        Intent registerIntent = new Intent(LoginActivity.this, SignActivity.class);
        startActivity(registerIntent);
    }
    //로그인 정보 저장 코드 2020.06.02 KBS
    private void load(){
        //기본값, 저장된 정보 없을경우
        saved_LoginData = appData.getBoolean("SAVE_LOGIN_DATA", false);
        saved_id = appData.getString("ID", "");
        saved_pwd = appData.getString("PWD", "");
    }

    private void save(){
        SharedPreferences.Editor editor = appData.edit();
        editor.putBoolean("SAVE_LOGIN_DATA", checkBox.isChecked());
        editor.putString("ID", et_id.getText().toString().trim());
        editor.putString("PWD", et_password.getText().toString().trim());
        editor.apply();
    }

    /** checking email verified or NOT */
    private void checkVerifiedEmail() {
        final FirebaseUser currentUser;
        currentUser = mLogin.getCurrentUser();
        Map<String, Object> docData = new HashMap<>();
        docData.put("verified",true);

        boolean isVerified = false;
        if (currentUser != null) {
            isVerified = currentUser.isEmailVerified();
        }
        if (isVerified){
            String UID = mLogin.getCurrentUser().getUid();
            db.collection("Users").document(UID).set(docData, SetOptions.merge());

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            SweetToast.info(LoginActivity.this, "Email is not verified. Please verify first");
            mLogin.signOut();
        }
    }
}