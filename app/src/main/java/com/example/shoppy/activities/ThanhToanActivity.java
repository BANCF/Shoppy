package com.example.shoppy.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shoppy.R;
import com.example.shoppy.model.CreateOrder;
import com.example.shoppy.model.NotiSendData;
import com.example.shoppy.retrofit.ApiBanHang;
import com.example.shoppy.retrofit.ApiPushNotification;
import com.example.shoppy.retrofit.RetrofitClient;
import com.example.shoppy.retrofit.RetrofitClientNoti;
import com.example.shoppy.ultils.EmailSingleton;
import com.example.shoppy.ultils.Ultils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import io.paperdb.Paper;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import vn.zalopay.sdk.Environment;
import vn.zalopay.sdk.ZaloPayError;
import vn.zalopay.sdk.ZaloPaySDK;
import vn.zalopay.sdk.listeners.PayOrderListener;

public class ThanhToanActivity extends AppCompatActivity {
    Toolbar toolbarThanhToan;
    TextView tvGiaTien, tvSdt, tvEmail;
    EditText edtDiaChi;
    AppCompatButton btnDatHang, btnZalopay;
    //    FirebaseAuth mAuth;
//    FirebaseUser mUser;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    ApiBanHang apiBanHang;
    long tongTien;
    int totalItem;
    String mobile, email;
    int iddonhang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thanh_toan);
        //zalo thanh toans
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        // ZaloPay SDK Init
        ZaloPaySDK.init(2553, Environment.SANDBOX);//2554
        initView();
        countItem();
        initControl();
    }

    //STEP 35: ĐỔI VỀ MẢNG MUA HÀNG
    private void countItem() {
        totalItem = 0;
        for (int i = 0; i < Ultils.mangMuaHang.size(); i++) {
            totalItem = totalItem + Ultils.mangMuaHang.get(i).getSoLuong();
        }
    }

    private void initControl() {
        setSupportActionBar(toolbarThanhToan);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbarThanhToan.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //Lấy data từ bên GiỏHàng
        DecimalFormat decimalFormat = new DecimalFormat("###,###,###");
        tongTien = getIntent().getLongExtra("tongtien", 0);
        tvGiaTien.setText(decimalFormat.format(tongTien));
        tvEmail.setText(Ultils.user_current.getEmail());
        tvSdt.setText(Ultils.user_current.getMobile());
//        email = EmailSingleton.getInstance().getEmail();
//        tvEmail.setText(email);
//        String mobile = EmailSingleton.getInstance().getMobile();
//        tvSdt.setText(mobile);
//        if(Paper.book().read("sdt")!=null){
//            tvSdt.setText(Paper.book().read("sdt"));
//        }
//        if(Paper.book().read("email")!=null){
//            tvEmail.setText(Paper.book().read("email"));
//        }
//        tvSdt.setText(mobile);

        //CẦN THÊM SỐ ĐIỆN THOẠI


        btnDatHang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strDiaChi = edtDiaChi.getText().toString().trim();
                if (TextUtils.isEmpty(strDiaChi)) {
                    Toast.makeText(getApplicationContext(), "Bạn chưa nhập địa chỉ", Toast.LENGTH_LONG).show();
                } else {
                    //Nếu nhập địa chỉ ta sẽ post data(Giỏ hàng) lên server lưu database thành chuỗi json
                    //Dùng thư viện Gson
                    String str_email = Ultils.user_current.getEmail();
                    String str_sdt = Ultils.user_current.getMobile();
                    int id = Ultils.user_current.getId();
                    Log.d("TAG", new Gson().toJson(Ultils.mangMuaHang));//STEP 35: ĐỔI VỀ MẢNG MUA HÀNG
                    //STEP 31:CẦN XỬ LÍ ĐỂ LẤY ID USER, SĐT, EMAIL
                    compositeDisposable.add(apiBanHang.createOrder(str_email, str_sdt, String.valueOf(tongTien), id, strDiaChi, totalItem, new Gson().toJson(Ultils.mangMuaHang))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    messageModel -> {
                                        pushNotiUser();
                                        Toast.makeText(getApplicationContext(), "Them don thanh cong", Toast.LENGTH_LONG).show();
                                        //STEP 35: MUA XONG THÌ CLEAR GIỎ HÀNG
                                        Ultils.mangMuaHang.clear();
                                        Intent backMain = new Intent(getApplicationContext(), MainActivity.class);
                                        startActivity(backMain);
                                        finish();

                                    },
                                    throwable -> {
                                        Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                            ));

                }
            }
        });

        btnZalopay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strDiaChi = edtDiaChi.getText().toString().trim();
                if (TextUtils.isEmpty(strDiaChi)) {
                    Toast.makeText(getApplicationContext(), "Bạn chưa nhập địa chỉ", Toast.LENGTH_LONG).show();
                } else {
                    //Nếu nhập địa chỉ ta sẽ post data(Giỏ hàng) lên server lưu database thành chuỗi json
                    //Dùng thư viện Gson
                    String str_email = Ultils.user_current.getEmail();
                    String str_sdt = Ultils.user_current.getMobile();
                    int id = Ultils.user_current.getId();
                    Log.d("TAG", new Gson().toJson(Ultils.mangMuaHang));//STEP 35: ĐỔI VỀ MẢNG MUA HÀNG
                    //STEP 31:CẦN XỬ LÍ ĐỂ LẤY ID USER, SĐT, EMAIL
                    compositeDisposable.add(apiBanHang.createOrder(str_email, str_sdt, String.valueOf(tongTien), id, strDiaChi, totalItem, new Gson().toJson(Ultils.mangMuaHang))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    messageModel -> {
                                        pushNotiUser();
                                        Toast.makeText(getApplicationContext(), "Them don thanh cong", Toast.LENGTH_LONG).show();
                                        //STEP 35: MUA XONG THÌ CLEAR GIỎ HÀNG
                                        Ultils.mangMuaHang.clear();
//                                        Intent backMain = new Intent(getApplicationContext(), MainActivity.class);
//                                        startActivity(backMain);
//                                        finish();
                                        iddonhang = Integer.parseInt(messageModel.getIddonhang());
                                        requestZalo();

                                    },
                                    throwable -> {
                                        Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                            ));

                }
            }
        });
    }
    private void requestZalo(){
        CreateOrder orderApi = new CreateOrder();

        try {
            JSONObject data = orderApi.createOrder("100000");
            String code = data.getString("return_code");
            Log.d("test", code);

            if (code.equals("1")) {
                String token = data.getString("zp_trans_token");
                Log.d("test", token);




                ZaloPaySDK.getInstance().payOrder(ThanhToanActivity.this, token, "demozpdk://app", new PayOrderListener() {
                    @Override
                    public void onPaymentSucceeded(String s, String s1, String s2) {
                        compositeDisposable.add(apiBanHang.updateZalopay(iddonhang,token)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(messageModel -> {
                                            if(messageModel.isSuccess()){
                                                Intent backMain = new Intent(getApplicationContext(), MainActivity.class);
                                                startActivity(backMain);
                                                finish();
                                            }
                                        },
                                        throwable -> {
                                            Log.d("error", throwable.getMessage());
                                        }
                                ));
                    }

                    @Override
                    public void onPaymentCanceled(String s, String s1) {

                    }

                    @Override
                    public void onPaymentError(ZaloPayError zaloPayError, String s, String s1) {

                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void pushNotiUser() {
        //get Token tren server
        compositeDisposable.add(apiBanHang.getToken(1)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(userModel -> {
                            for (int i = 0; i < userModel.getResult().size(); i++) {
                                Map<String, String> data = new HashMap<>();
                                data.put("title", "thong bao");
                                data.put("body", "Ban co don hang moi");
                                NotiSendData notiSendData = new NotiSendData(userModel.getResult().get(i).getToken(), data);
                                ApiPushNotification apiPushNotification = RetrofitClientNoti.getInstance().create(ApiPushNotification.class);
                                compositeDisposable.add(apiPushNotification.sendNotification(notiSendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(
                                                notiResponse -> {

                                                },
                                                throwable -> {
                                                    Log.d("logg", throwable.getMessage());
                                                }
                                        ));
                            }
                        }, throwable -> {
                            Log.d("logg", throwable.getMessage());
                        }
                ));
    }

    private void initView() {
        toolbarThanhToan = findViewById(R.id.toolbarThanhToan);
        tvGiaTien = findViewById(R.id.tvGiaTien);
        tvSdt = findViewById(R.id.tvSdt);
        tvEmail = findViewById(R.id.tvEmail);
        edtDiaChi = findViewById(R.id.edtDiaChi);
        btnDatHang = findViewById(R.id.btnDatHang);
        btnZalopay = findViewById(R.id.btnZalopay);

//        mAuth = FirebaseAuth.getInstance();
//        mUser = mAuth.getCurrentUser();
        //STEP 31:
        apiBanHang = RetrofitClient.getInstance(Ultils.BASE_URL).create(ApiBanHang.class);
    }

    //STEP 31
    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        ZaloPaySDK.getInstance().onResult(intent);
    }
}