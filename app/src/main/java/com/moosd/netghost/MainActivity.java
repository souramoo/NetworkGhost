package com.moosd.netghost;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    EditText macEntry = null, hostEntry = null;
    ToggleButton macToggle = null, hostToggle = null;
    Button updateButton = null, revertButton = null;
    SharedPreferences settings = null;
    Switch spoofSwitch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences("settings", 0);

        hostEntry = ((EditText) findViewById(R.id.editText2));
        hostToggle = ((ToggleButton) findViewById(R.id.toggleButton2));

        macEntry = ((EditText) findViewById(R.id.editText));
        macToggle = ((ToggleButton) findViewById(R.id.toggleButton));

        updateButton = ((Button) findViewById(R.id.button3));
        revertButton = ((Button) findViewById(R.id.button));

        spoofSwitch = ((Switch) findViewById(R.id.switch1));

        spoofSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // update settings
                settings.edit().putBoolean("spoofenabled", isChecked).commit();
                // update ux
                updateUX();
            }
        });

        macEntry.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        macEntry.setSingleLine();

        macEntry.addTextChangedListener(new TextWatcher() {
            String mPreviousMac = null;

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String enteredMac = macEntry.getText().toString().toUpperCase();
                String cleanMac = clearNonMacCharacters(enteredMac);
                String formattedMac = formatMacAddress(cleanMac);

                int selectionStart = macEntry.getSelectionStart();
                formattedMac = handleColonDeletion(enteredMac, formattedMac, selectionStart);
                int lengthDiff = formattedMac.length() - enteredMac.length();

                setMacEdit(cleanMac, formattedMac, selectionStart, lengthDiff);
            }

            private String clearNonMacCharacters(String mac) {
                return mac.toString().replaceAll("[^A-Fa-f0-9]", "");
            }

            private String formatMacAddress(String cleanMac) {
                int grouppedCharacters = 0;
                String formattedMac = "";

                for (int i = 0; i < cleanMac.length(); ++i) {
                    formattedMac += cleanMac.charAt(i);
                    ++grouppedCharacters;

                    if (grouppedCharacters == 2) {
                        formattedMac += ":";
                        grouppedCharacters = 0;
                    }
                }

                if (cleanMac.length() == 12)
                    formattedMac = formattedMac.substring(0, formattedMac.length() - 1);

                return formattedMac;
            }

            private String handleColonDeletion(String enteredMac, String formattedMac, int selectionStart) {
                if (mPreviousMac != null && mPreviousMac.length() > 1) {
                    int previousColonCount = colonCount(mPreviousMac);
                    int currentColonCount = colonCount(enteredMac);

                    if (currentColonCount < previousColonCount) {
                        formattedMac = formattedMac.substring(0, selectionStart - 1) + formattedMac.substring(selectionStart);
                        String cleanMac = clearNonMacCharacters(formattedMac);
                        formattedMac = formatMacAddress(cleanMac);
                    }
                }
                return formattedMac;
            }

            private int colonCount(String formattedMac) {
                return formattedMac.replaceAll("[^:]", "").length();
            }

            private void setMacEdit(String cleanMac, String formattedMac, int selectionStart, int lengthDiff) {
                macEntry.removeTextChangedListener(this);
                if (cleanMac.length() <= 12) {
                    macEntry.setText(formattedMac);
                    macEntry.setSelection(selectionStart + lengthDiff);
                    mPreviousMac = formattedMac;
                } else {
                    macEntry.setText(mPreviousMac);
                    macEntry.setSelection(mPreviousMac.length());
                }
                macEntry.addTextChangedListener(this);
            }
        });

        macToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked) {
                    settings.edit().putString("macset", Util.randomMAC()).commit();
                }
                // update settings
                settings.edit().putBoolean("macrandomise", isChecked).commit();
                //update ux
                updateUX();
            }
        });
        hostToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if(isChecked) {
                    settings.edit().putString("hostset", Util.randomHostname()).commit();
                }
                // update settings
                settings.edit().putBoolean("hostrandomise", isChecked).commit();
                // update ux
                updateUX();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean macrandomise = settings.getBoolean("macrandomise", false), hostrandomise = settings.getBoolean("hostrandomise", false);
                if(macrandomise) {
                    settings.edit().putString("macset", Util.randomMAC()).commit();
                } else {
                    settings.edit().putString("macset", macEntry.getText().toString()).commit();
                }
                if(hostrandomise) {
                    settings.edit().putString("hostset", Util.randomHostname()).commit();
                } else {
                    settings.edit().putString("hostset", hostEntry.getText().toString()).commit();
                }
                updateUX();
                Util.setHost(hostEntry.getText().toString());
                Util.setMAC(macEntry.getText().toString(), MainActivity.this);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUX();
    }

    private void updateUX() {
        boolean macrandomise = settings.getBoolean("macrandomise", false), hostrandomise = settings.getBoolean("hostrandomise", false), spoofenabled = settings.getBoolean("spoofenabled", true);

        macEntry.setText(settings.getString("macset", Util.getMAC()));
        hostEntry.setText(settings.getString("hostset", Util.getHost()));
        spoofSwitch.setChecked(spoofenabled);

        macToggle.setEnabled(spoofenabled);
        hostToggle.setEnabled(spoofenabled);

        if(spoofenabled) {
            macToggle.setChecked(macrandomise);
            macEntry.setEnabled(!macrandomise);

            hostToggle.setChecked(hostrandomise);
            hostEntry.setEnabled(!hostrandomise);

            updateButton.setEnabled(true);
            revertButton.setEnabled(true);
        } else {
            macToggle.setChecked(false);
            macEntry.setEnabled(false);

            hostToggle.setChecked(false);
            hostEntry.setEnabled(false);

            updateButton.setEnabled(false);
            revertButton.setEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
