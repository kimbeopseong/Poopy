package com.example.poopy.account;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.poopy.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import xyz.hasnat.sweettoast.SweetToast;

public class SignActivity extends AppCompatActivity {
    private static final String TAG = "SignActivity";
    private Button btnSignup;
    private EditText etUserEmail, etUserPassword,etUserPasswordConfirm, etUserName;
    private TextView tvBirthday;
    private TextView tvHavedAccount;
    private RadioButton rbtnMale, rbtnFemale;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);
        mAuth=FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignup = (Button)findViewById(R.id.signup_button);
        etUserEmail = (EditText)findViewById(R.id.signup_email);
        etUserPassword = (EditText)findViewById(R.id.signup_password);
        etUserPasswordConfirm=(EditText)findViewById(R.id.signup_confirm);
        etUserName=(EditText)findViewById(R.id.signup_name);
        tvBirthday=(TextView)findViewById(R.id.signup_birthday);
        rbtnMale=(RadioButton)findViewById(R.id.signup_male);
        rbtnFemale=(RadioButton)findViewById(R.id.signup_female);

        tvHavedAccount = (TextView)findViewById(R.id.already_have_account);
        progressDialog = new ProgressDialog(SignActivity.this);


        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etUserEmail.getText().toString();
                String password = etUserPassword.getText().toString();
                String confirmPassword=etUserPasswordConfirm.getText().toString();
                String name=etUserName.getText().toString();
                String birthday=tvBirthday.getText().toString();
                String sex = null;
                if(rbtnMale.isChecked())
                    sex="남자";
                else if(rbtnFemale.isChecked())
                    sex="여자";
                CreateNewAccount(email, password, confirmPassword, name, birthday, sex);
            }
        });

        tvHavedAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToLoginActivity();
            }
        });

        tvBirthday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog_DatePicker();
            }
        });

    }

    private void CreateNewAccount(String email, String password, String confirmPassword, final String name, final String birthday, final String sex) {
        if (TextUtils.isEmpty(email)) {
            SweetToast.error(SignActivity.this, "Your email is required.");
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            SweetToast.error(SignActivity.this, "Your email is not valid.");
        } else if (TextUtils.isEmpty(password)) {
            SweetToast.error(SignActivity.this, "Please fill this password field");
        } else if (password.length() < 6) {
            SweetToast.error(SignActivity.this, "Create a password at least 6 characters long.");
        } else if (TextUtils.isEmpty(confirmPassword)) {
            SweetToast.warning(SignActivity.this, "Please retype in password field");
        } else if (!password.equals(confirmPassword)) {
            SweetToast.error(SignActivity.this, "Your password don't match with your confirm password");
        } else if (TextUtils.isEmpty(name)) {
            SweetToast.warning(SignActivity.this, "Please write your name");
        } else if (TextUtils.isEmpty(birthday)){
            SweetToast.warning(SignActivity.this, "Please write your birthday");
        }
        else {

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Map<String, Object> Userinfo = new HashMap<>();
                                String currentUserID = mAuth.getCurrentUser().getUid();

                                Userinfo.put("name", name);
                                Userinfo.put("birthday", birthday);
                                Userinfo.put("sex", sex);
                                Userinfo.put("verified", "false");

                                db.collection("Users").document(currentUserID).set(Userinfo, SetOptions.merge())
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    // SENDING VERIFICATION EMAIL TO THE REGISTERED USER'S EMAIL
                                                    currentUser = mAuth.getCurrentUser();
                                                    if (currentUser != null) {
                                                        currentUser.sendEmailVerification()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                        if (task.isSuccessful()) {
                                                                            registerSuccessPopUp();
                                                                            // LAUNCH activity after certain time period
                                                                            new Timer().schedule(new TimerTask() {
                                                                                public void run() {
                                                                                    SignActivity.this.runOnUiThread(new Runnable() {
                                                                                        public void run() {
                                                                                            mAuth.signOut();
                                                                                            Intent mainIntent = new Intent(SignActivity.this, LoginActivity.class);
                                                                                            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                            startActivity(mainIntent);
                                                                                            finish();

                                                                                            SweetToast.info(SignActivity.this, "Please check your email & verify.");
                                                                                        }
                                                                                    });
                                                                                }
                                                                            }, 8000);
                                                                        } else {
                                                                            mAuth.signOut();
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                }
                                            }
                                        });

                            } else {
                                String message = task.getException().getMessage();
                                SweetToast.error(SignActivity.this, "Error occurred : " + message);
                            }
                            progressDialog.dismiss();
                        }
                    });
            //config progressbar
            progressDialog.setTitle("Creating new account");
            progressDialog.setMessage("Please wait a moment....");
            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(false);

        }
    }
    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(SignActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }
    private void registerSuccessPopUp() {
        // Custom Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(SignActivity.this);
        View view = LayoutInflater.from(SignActivity.this).inflate(R.layout.register_success_popup, null);

        //ImageButton imageButton = view.findViewById(R.id.successIcon);
        //imageButton.setImageResource(R.drawable.logout);
        builder.setCancelable(false);

        builder.setView(view);
        builder.show();
    }
    //생년월일 입력용 다이얼로그
    private void Dialog_DatePicker() {
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String _dateStr = year+"년 "+(monthOfYear + 1) + "월 " + dayOfMonth + "일";
                tvBirthday.setText(_dateStr);
            }
        };
        DatePickerDialog alert = new DatePickerDialog(this, R.style.MySpinnerDatePickerStyle, mDateSetListener, 1990, 0, 1);
        alert.show();
    }
}