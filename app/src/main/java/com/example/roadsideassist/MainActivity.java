package com.example.roadsideassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText edtBrand = findViewById(R.id.editTextText);
        EditText edtModel = findViewById(R.id.editTextText2);
        EditText edtYear = findViewById(R.id.editTextText3);
        CheckBox cbSafety = findViewById(R.id.cbSafetyCheck);
        Button btnSubmit = findViewById(R.id.button);
        Button btnEmergency = findViewById(R.id.btnEmergency);

        btnSubmit.setOnClickListener(v -> {
            if (!cbSafety.isChecked()) {
                btnEmergency.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Please ensure you are safe before proceeding.", Toast.LENGTH_SHORT).show();
                return;
            } else {
                btnEmergency.setVisibility(View.GONE);
            }

            String brand = edtBrand.getText().toString().trim();
            String model = edtModel.getText().toString().trim();
            String year = edtYear.getText().toString().trim();

            if (brand.isEmpty() || model.isEmpty() || year.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all vehicle details", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, RoadsideChatActivity.class);
            intent.putExtra("CAR_BRAND", brand);
            intent.putExtra("CAR_MODEL", model);
            intent.putExtra("CAR_YEAR", year);
            startActivity(intent);
        });
    }
}