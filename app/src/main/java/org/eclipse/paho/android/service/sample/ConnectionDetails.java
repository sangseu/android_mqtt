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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import org.eclipse.paho.android.service.sample.R;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

/**
 * The connection details activity operates the fragments that make up the
 * connection details screen.
 * <p>
 * The fragments which this FragmentActivity uses are
 * <ul>
 * <li>{@link HistoryFragment}
 * <li>{@link PublishFragment}
 * <li>{@link SubscribeFragment}
 * </ul>
 *
 */
public class ConnectionDetails extends FragmentActivity implements
    ActionBar.TabListener {
  //===============================
  private BroadcastReceiver mReceiver;

  /**
   * {@link SectionsPagerAdapter} that is used to get pages to display
   */
  SectionsPagerAdapter sectionsPagerAdapter;
  /**
   * {@link ViewPager} object allows pages to be flipped left and right
   */
  ViewPager viewPager;

  /** The currently selected tab **/
  private int selected = 0;

  /**
   * The handle to the {@link Connection} which holds the data for the client
   * selected
   **/
  private String clientHandle = null;

  /** This instance of <code>ConnectionDetails</code> **/
  private final ConnectionDetails connectionDetails = this;

  /**
   * The instance of {@link Connection} that the <code>clientHandle</code>
   * represents
   **/
  private Connection connection = null;

  /**
   * The {@link ChangeListener} this object is using for the connection
   * updates
   **/
  private ChangeListener changeListener = null;

  private Context context = this;

  public int index = 0;

  static String topic_sub = "";
  //static String topic_sub_lastwill = "";
  static String topic_pub = "";
  static String client_id_del = "";


  /**
   * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    //========================
    IntentFilter intentFilter = new IntentFilter(
            "android.intent.action.MAIN");

    mReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        //extract our message from intent
        String msg = intent.getStringExtra("some_msg");
        char[] c_msg = msg.toCharArray();
        //log our message value
        Log.i("InchooTutorial", msg);

        TextView tView = (TextView) findViewById(R.id.tv);
        tView.setText("STT: " + msg);
        if(msg.equals("off")) {
          tView.setText("NOT READY!");
        }
        else
        if(c_msg.length == 3) {
          if (c_msg[0] == '1') {
            set_switch(1, true);
          } else set_switch(1, false);
          if (c_msg[1] == '1') {
            set_switch(2, true);
          } else set_switch(2, false);
          if (c_msg[2] == '1') {
            set_switch(3, true);
          } else set_switch(3, false);
        }
      }
    };
    //registering our receiver
    this.registerReceiver(mReceiver, intentFilter);
    //==========================

    clientHandle = getIntent().getStringExtra("handle");

    //ActivityConstants.index_device = Persistence.ID.indexOf(clientHandle);
    for(int i=0; i < Persistence.ID.size(); i++) {
      if (Persistence.ID.get(i).equals(cur_device()) ) {
        index = i;
        break;
      }
    }

    topic_sub = "myswitch/" + cur_ID() + "/stt";
    topic_pub = "myswitch/" + cur_ID() + "/ctrl";


    setContentView(R.layout.activity_connection_details);
    // Create the adapter that will return a fragment for each of the pages
    sectionsPagerAdapter = new SectionsPagerAdapter(
        getSupportFragmentManager());

    // Set up the action bar for tab navigation
    final ActionBar actionBar = getActionBar();
    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    // add the sectionsPagerAdapter
    viewPager = (ViewPager) findViewById(R.id.pager);
    viewPager.setAdapter(sectionsPagerAdapter);

    viewPager
        .setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {

          @Override
          public void onPageSelected(int position) {
            // select the tab that represents the current page
            actionBar.setSelectedNavigationItem(position);
          }
        });

    // Create the tabs for the screen
    for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
      ActionBar.Tab tab = actionBar.newTab();
      tab.setText(sectionsPagerAdapter.getPageTitle(i));
      tab.setTabListener(this);
      actionBar.addTab(tab);
    }

    connection = Connections.getInstance(this).getConnection(clientHandle);
    changeListener = new ChangeListener();
    connection.registerChangeListener(changeListener);

  }

  @Override
  public void onResume(){
    super.onResume();
    if(isDisconnected() || isNone()) ClientConnections.doCon = true;

    //=================================
    IntentFilter intentFilter = new IntentFilter(
            "android.intent.action.MAIN");

    mReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        //extract our message from intent
        String msg_for_me = intent.getStringExtra("some_msg");
        //log our message value
        Log.i("InchooTutorial", msg_for_me);

      }
    };
    //registering our receiver
    this.registerReceiver(mReceiver, intentFilter);
  }


  @Override
  public void onPause(){
    super.onPause();
    disCon();

    //========================================unregister our receiver
    this.unregisterReceiver(this.mReceiver);
  }

  @Override
  protected void onDestroy() {
    connection.removeChangeListener(null);
    super.onDestroy();
  }

  //============================
  public String cur_device() {
    return Connections.getInstance(this).getConnection(clientHandle).get_cur_device();
  }
  public String cur_ID() {
    return  Persistence.name.get(index);
  }

  public void set_switch(int sw, boolean stt) {
    switch (sw) {
      case 1: ((Switch) findViewById(R.id.switch1)).setChecked(stt); break;
      case 2: ((Switch) findViewById(R.id.switch2)).setChecked(stt); break;
      case 3: ((Switch) findViewById(R.id.switch3)).setChecked(stt); break;
    }
  }

  //============================
  public void onSwitchClick(View v) {
    // mySwitch = (Switch) findViewById(R.id.mySwitch);
    //set the switch to ON
    //mySwitch.setChecked(true);
    Listener listento = new Listener(this, clientHandle);

    cur_ID();
    TextView tView = (TextView) findViewById(R.id.tv);
    switch(v.getId()) {
      case R.id.switch1:
        if(((Switch) v).isChecked()) {
          tView.setText("1 is ON");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.on1;
            listento.publish();
          }
        }
        else {
          tView.setText("1 is OFF");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.off1;
            listento.publish();
          }
        }
        break;

      case R.id.switch2:
        if(((Switch) v).isChecked()) {
          tView.setText("2 is ON");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.on2;
            listento.publish();
          }
        }
        else {
          tView.setText("2 is OFF");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.off2;
            listento.publish();
          }
        }
        break;

      case R.id.switch3:
        if(((Switch) v).isChecked()) {
          tView.setText("3 is ON");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.on3;
            listento.publish();
          }
        }
        else {
          tView.setText("3 is OFF");
          if (isConnected()) {
            ActivityConstants.sw_state = ActivityConstants.off3;
            listento.publish();
          }
        }
        break;

    }
    boolean stt = ((Switch)v).isChecked();
    if(stt) {
    }
    else {
    }
  }

  public void onButtonClick(View v) {
    Pub("myswitch/manager/del", "{\"id\":\"" + cur_ID() + "\"}");
  }


  /**
   * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    int menuID;
    Integer button = null;
    boolean connected = Connections.getInstance(this)
        .getConnection(clientHandle).isConnected();

    // Select the correct action bar menu to display based on the
    // connectionStatus and which tab is selected
    if (connected) {
      switch (selected) {
        case 0 : // history view
          menuID = R.menu.activity_connection_details;
          break;
        case 1 : // subscribe view
          menuID = R.menu.activity_subscribe;
          button = R.id.subscribe;
          break;
        case 2 : // publish view
          menuID = R.menu.activity_publish;
          button = R.id.publish;
          Sub(topic_sub);
          if(ClientConnections.doAdd) {
          //try PUB to add device
            String add = "{\"id\":\"" + cur_ID() + "\",\"name\":\"" + cur_device() + "\"}";
            Pub("myswitch/manager/req", add);
            ClientConnections.doAdd = false;
          }
          /*
          if(ClientConnections.doDel) {
            ClientConnections.doDel = false;
            Pub("myswitch/manager/del", "{\"id\":\"" + ClientConnections.client_ID_del + "\"}");
          }
          */

          break;
        default :
          menuID = R.menu.activity_connection_details;
          break;
      }

    }
    else {
      switch (selected) {
        case 0 : // history view
          menuID = R.menu.activity_connection_details_disconnected;
          break;
        case 1 : // subscribe view
          menuID = R.menu.activity_subscribe_disconnected;
          button = R.id.subscribe;
          break;
        case 2 : // publish view
          menuID = R.menu.activity_publish_disconnected;
          button = R.id.publish;
          Con();//==============================> do Connect
          break;
        default :
          menuID = R.menu.activity_connection_details_disconnected;
          break;
      }
    }

    // inflate the menu selected
    getMenuInflater().inflate(menuID, menu);
    Listener listener = new Listener(this, clientHandle);
    // add listeners
    if (button != null) {
      // add listeners
      menu.findItem(button).setOnMenuItemClickListener(listener);
      if (!Connections.getInstance(this).getConnection(clientHandle)
          .isConnected()) {
        menu.findItem(button).setEnabled(false);
      }
    }
    // add the listener to the disconnect or connect menu option
    if (connected) {
      menu.findItem(R.id.disconnect).setOnMenuItemClickListener(listener);
    }
    else {
      menu.findItem(R.id.connectMenuOption).setOnMenuItemClickListener(
              listener);
    }

    return true;
  }

  /**
   * @see android.app.ActionBar.TabListener#onTabUnselected(android.app.ActionBar.Tab,
   *      android.app.FragmentTransaction)
   */
  @Override
  public void onTabUnselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // Don't need to do anything when a tab is unselected
  }

  /**
   * @see android.app.ActionBar.TabListener#onTabSelected(android.app.ActionBar.Tab,
   *      android.app.FragmentTransaction)
   */
  @Override
  public void onTabSelected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // When the given tab is selected, switch to the corresponding page in
    // the ViewPager.
    viewPager.setCurrentItem(tab.getPosition());
    selected = tab.getPosition();
    // invalidate the options menu so it can be updated
    invalidateOptionsMenu();
    // history fragment is at position zero so get this then refresh its
    // view
    ((HistoryFragment) sectionsPagerAdapter.getItem(0)).refresh();
  }

  /**
   * @see android.app.ActionBar.TabListener#onTabReselected(android.app.ActionBar.Tab,
   *      android.app.FragmentTransaction)
   */
  @Override
  public void onTabReselected(ActionBar.Tab tab,
      FragmentTransaction fragmentTransaction) {
    // Don't need to do anything when the tab is reselected
  }

  /**
   * Provides the Activity with the pages to display for each tab
   *
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    // Stores the instances of the pages
    private ArrayList<Fragment> fragments = null;

    /**
     * Only Constructor, requires a the activity's fragment managers
     *
     * //@param fragmentManager
     */
    public SectionsPagerAdapter(FragmentManager fragmentManager) {
      super(fragmentManager);
      fragments = new ArrayList<Fragment>();
      // create the history view, passes the client handle as an argument
      // through a bundle
      Fragment fragment = new HistoryFragment();
      Bundle args = new Bundle();
      args.putString("handle", getIntent().getStringExtra("handle"));
      fragment.setArguments(args);
      // add all the fragments for the display to the fragments list
      fragments.add(fragment);
      fragments.add(new SubscribeFragment());
      fragments.add(new PublishFragment());

    }

    /**
     * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
     */
    @Override
    public Fragment getItem(int position) {
      return fragments.get(position);
    }

    /**
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
      return fragments.size();
    }

    /**
     *
     * @see FragmentPagerAdapter#getPageTitle(int)
     */
    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0 :
          return getString(R.string.history).toUpperCase();
        case 1 :
          return getString(R.string.subscribe).toUpperCase();
        case 2 :
          return getString(R.string.publish).toUpperCase();
      }
      // return null if there is no title matching the position
      return null;
    }

  }

  /**
   * <code>ChangeListener</code> updates the UI when the {@link Connection}
   * object it is associated with updates
   *
   */
  private class ChangeListener implements PropertyChangeListener {

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      // connection object has change refresh the UI

      connectionDetails.runOnUiThread(new Runnable() {

        @Override
        public void run() {
          connectionDetails.invalidateOptionsMenu();
          ((HistoryFragment) connectionDetails.sectionsPagerAdapter
              .getItem(0)).refresh();

        }
      });

    }
  }

  protected void Con() {
    //if(Connections.getInstance(this).getConnection(clientHandle).isNone()) ClientConnections.doSub = false;
    // If disconnected===============================================================================================> do Connect
    //if(Connections.getInstance(this).getConnection(clientHandle).isDisconnected() || ClientConnections.doCon) {
    if (ClientConnections.doCon) {
      Connection c = Connections.getInstance(this).getConnection(clientHandle);
      ClientConnections.doSub = true;// allow Sub========================
      ClientConnections.doCon = false;
      try {
        c.getClient().connect(c.getConnectionOptions(), null, new ActionListener(context, ActionListener.Action.CONNECT, clientHandle, null));
      } catch (MqttSecurityException e) {
        ClientConnections.doSub = false;
        Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
        c.addAction("Client failed to connect");
      } catch (MqttException e) {
        ClientConnections.doSub = false;
        Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
        c.addAction("Client failed to connect");
      }
    }
  }

  protected void disCon() {
    Connection c = Connections.getInstance(context).getConnection(clientHandle);

    //if the client is not connected, process the disconnect
    if (!c.isConnected()) {
      return;
    }

    try {
      c.getClient().disconnect(null, new ActionListener(context, ActionListener.Action.DISCONNECT, clientHandle, null));
      c.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to disconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to disconnect");
    }
  }

  protected void Pub(String tp, String mess) {
    String topic = tp;
    String message = mess;
    int qos = 0;
    boolean retained = false;
    String[] args = new String[2];
    args[0] = message;
    args[1] = topic+";qos:"+qos+";retained:"+retained;

    if(Connections.getInstance(this).getConnection(clientHandle).isConnected() ) {
      try {
        Connections.getInstance(context).getConnection(clientHandle).getClient()
                .publish(topic, message.getBytes(), qos, retained, null, new ActionListener(context, ActionListener.Action.PUBLISH, clientHandle, args));
      } catch (MqttSecurityException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
      } catch (MqttException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
      }
    }
  }

  protected void Sub(String topic_sub) {
    // ============================= do Sub
    //if(Connections.getInstance(this).getConnection(clientHandle).isConnected() ) {
    if( ClientConnections.doSub ) {
      String topic;
      topic = topic_sub;
      ClientConnections.doSub = false;
      try {
        String[] topics = new String[1];
        topics[0] = topic;
        int qos = 0;
        Connections.getInstance(context).getConnection(clientHandle).getClient()
                .subscribe(topic, qos, null, new ActionListener(context, ActionListener.Action.SUBSCRIBE, clientHandle, topics));
      } catch (MqttSecurityException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
      } catch (MqttException e) {
        Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
      }
    }
    //}
  }

  protected boolean isNone() {
    return Connections.getInstance(this).getConnection(clientHandle).isNone();
  }
  protected boolean isConnected() {
    return Connections.getInstance(this).getConnection(clientHandle).isConnected();
  }
  protected boolean isDisconnected() {
    return Connections.getInstance(this).getConnection(clientHandle).isDisconnected();
  }
  protected boolean isConnecting() {
    return Connections.getInstance(this).getConnection(clientHandle).isConnecting();
  }
  protected boolean isDisconnecting() {
    return Connections.getInstance(this).getConnection(clientHandle).isDisconnecting();
  }
}
