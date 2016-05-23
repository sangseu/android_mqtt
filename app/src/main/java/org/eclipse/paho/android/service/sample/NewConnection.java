/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.paho.android.service.sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.paho.android.service.sample.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Handles collection of user information to create a new MQTT Client
 *
 */
public class NewConnection extends Activity {

  static final String ACTION_SCAN = "com.google.zxing.client.android.SCAN";
  static String QRcode = "";
  static String deviceid = "";

  /** {@link Bundle} which holds data from activities launched from this activity **/
  private Bundle result_hold = null;


  /** 
   * @see android.app.Activity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_new_connection);

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
    adapter.addAll(readHosts());
    //AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.serverURI);
    //textView.setAdapter(adapter);

    //load auto compete options

  }

  //product qr code mode
  public void scanQR(View v) {
    try {
      //start the scanning activity from the com.google.zxing.client.android.SCAN intent
      Intent intent = new Intent(ACTION_SCAN);
      intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
      startActivityForResult(intent, 0);
    } catch (ActivityNotFoundException anfe) {
      //on catch, show the download dialog
      showDialog(NewConnection.this, "No Scanner Found", "Download a scanner code activity?", "Yes", "No").show();
    }
  }

  //alert dialog for downloadDialog
  private static AlertDialog showDialog(final Activity act, CharSequence title, CharSequence message, CharSequence buttonYes, CharSequence buttonNo) {
    AlertDialog.Builder downloadDialog = new AlertDialog.Builder(act);
    downloadDialog.setTitle(title);
    downloadDialog.setMessage(message); //======================= message
    downloadDialog.setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialogInterface, int i) {
        Uri uri = Uri.parse("market://search?q=pname:" + "com.google.zxing.client.android");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
          act.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {

        }
      }
    });
    downloadDialog.setNegativeButton(buttonNo, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialogInterface, int i) {
      }
    });
    return downloadDialog.show();
  }


  /** 
   * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_new_connection, menu);
    OnMenuItemClickListener listener = new Listener(this);
    menu.findItem(R.id.connectAction).setOnMenuItemClickListener(listener);
    menu.findItem(R.id.advanced).setOnMenuItemClickListener(listener);

    return true;
  }

  /** 
   * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home :
        NavUtils.navigateUpFromSameTask(this);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * //@see android.app.Activity#onActivityResult(int, int, android.content.Intent)
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode,
      Intent intent) {

    if (resultCode == RESULT_CANCELED) {
      return;
    }

    switch (requestCode) {
      case 0:// QRcode scan
        if (resultCode == RESULT_OK) {
          //get the extras that are returned from the intent
          String contents = intent.getStringExtra("SCAN_RESULT");
          QRcode = contents;
          String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
          Toast toast = Toast.makeText(this, "Result:" + contents + " Format:" + format, Toast.LENGTH_LONG);
          toast.show();
        }

        if ( (QRcode.indexOf(";") > 0) ) {
          if (QRcode.indexOf(":") > 0) {
            String[] part = QRcode.split(";");
            String[] part_ssid = part[0].split(":");
            String[] part_pwd = part[1].split(":");
            if (part_ssid[0].equals("SSID")) {
              deviceid = part_ssid[1];
            }
          }
        }
        ((EditText) findViewById(R.id.device_ID)).setText(deviceid);

        break;
      case 1:// Advance config Client
        result_hold = intent.getExtras();
        break;

    }
  }


  /**
   * Handles action bar actions
   *
   */
  private class Listener implements OnMenuItemClickListener {

    //used for starting activities 
    private NewConnection newConnection = null;

    public Listener(NewConnection newConnection)
    {
      this.newConnection = newConnection;
    }

    /**
     * @see android.view.MenuItem.OnMenuItemClickListener#onMenuItemClick(android.view.MenuItem)
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
      {
        // this will only connect need to package up and sent back

        int id = item.getItemId();

        Intent dataBundle = new Intent();

        switch (id) {
          case R.id.connectAction :
            //extract client information
            String server = ((AutoCompleteTextView) findViewById(R.id.serverURI)).getText().toString();
            String port = ((EditText) findViewById(R.id.port)).getText().toString();
            //String clientId = ((EditText) findViewById(R.id.clientId)).getText().toString();

            String d_name = ((EditText) findViewById(R.id.device_name)).getText().toString();// [device name] using for show list view
            String dv_ID = ((EditText) findViewById(R.id.device_ID)).getText().toString();// [device ID] using for SUB/PUB
            ClientConnections.d_ID = dv_ID;

            if(d_name.equals(ActivityConstants.empty)) {
              d_name = QRcode;
            }

            //String clientId = d_name; //==========> change default to configable
            //String server = getString(R.string.server_name);
            //String port = getString(R.string.port_name);

            if (dv_ID.equals(ActivityConstants.empty))
            {
              String notificationText = newConnection.getString(R.string.missingID);
              Notify.toast(newConnection, notificationText, Toast.LENGTH_LONG);
              return false;
            }

/*
            if (server.equals(ActivityConstants.empty) || port.equals(ActivityConstants.empty) || clientId.equals(ActivityConstants.empty))
            {
              String notificationText = newConnection.getString(R.string.missingOptions);
              Notify.toast(newConnection, notificationText, Toast.LENGTH_LONG);
              return false;
            }
*/


            boolean cleanSession = ((CheckBox) findViewById(R.id.cleanSessionCheckBox)).isChecked();
            //persist server
            persistServerURI(server);

            //put data into a bundle to be passed back to ClientConnections
            dataBundle.putExtra(ActivityConstants.device_name, dv_ID);
            dataBundle.putExtra(ActivityConstants.server, server);
            dataBundle.putExtra(ActivityConstants.port, port);
            dataBundle.putExtra(ActivityConstants.clientId, d_name);// ~ device name (set by user)
            dataBundle.putExtra(ActivityConstants.action, ActivityConstants.connect);
            dataBundle.putExtra(ActivityConstants.cleanSession, cleanSession);

            if (result_hold == null) {
              // create a new bundle and put default advanced options into a bundle
              result_hold = new Bundle();

              result_hold.putString(ActivityConstants.message,
                  ActivityConstants.empty);
              result_hold.putString(ActivityConstants.topic, ActivityConstants.empty);
              result_hold.putInt(ActivityConstants.qos, ActivityConstants.defaultQos);
              result_hold.putBoolean(ActivityConstants.retained,
                  ActivityConstants.defaultRetained);

              result_hold.putString(ActivityConstants.username,
                  ActivityConstants.empty);
              result_hold.putString(ActivityConstants.password,
                  ActivityConstants.empty);

              result_hold.putInt(ActivityConstants.timeout,
                  ActivityConstants.defaultTimeOut);
              result_hold.putInt(ActivityConstants.keepalive,
                  ActivityConstants.defaultKeepAlive);
              result_hold.putBoolean(ActivityConstants.ssl,
                  ActivityConstants.defaultSsl);

            }
            //add result bundle to the data being returned to ClientConnections
            dataBundle.putExtras(result_hold);

            setResult(RESULT_OK, dataBundle);
            newConnection.finish();
            break;
          case R.id.advanced :
            //start the advanced options activity
            dataBundle.setClassName(newConnection,
                "org.eclipse.paho.android.service.sample.Advanced");
            newConnection.startActivityForResult(dataBundle,
                ActivityConstants.advancedConnect);

            break;
        }
        return false;

      }

    }

    /**
     * Add a server URI to the persisted file
     * 
     * @param serverURI the uri to store
     */
    private void persistServerURI(String serverURI) {
      File fileDir = newConnection.getFilesDir();
      File presited = new File(fileDir, "hosts.txt");
      BufferedWriter bfw = null;
      try {
        bfw = new BufferedWriter(new FileWriter(presited));
        bfw.write(serverURI);
        bfw.newLine();
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      finally {
        try {
          if (bfw != null) {
            bfw.close();
          }
        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

  }

  /**
   * Read persisted hosts
   * @return The hosts contained in the persisted file
   */
  private String[] readHosts() {
    File fileDir = getFilesDir();
    File persisted = new File(fileDir, "hosts.txt");
    if (!persisted.exists()) {
      return new String[0];
    }
    ArrayList<String> hosts = new ArrayList<String>();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(persisted));
      String line = null;
      line = br.readLine();
      while (line != null) {
        hosts.add(line);
        line = br.readLine();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (br != null) {
          br.close();
        }
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    return hosts.toArray(new String[hosts.size()]);

  }
}
