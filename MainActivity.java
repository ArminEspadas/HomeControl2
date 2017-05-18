package com.example.pch61m.homecontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pch61m.homecontrol.home.db.Inventory;
import com.example.pch61m.homecontrol.home.db.Users;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, FragmentExterior.OnFragmentInteractionListener {





    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID SERIAL_PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    private BtDevRvAdapter adapter;
    private List<BluetoothDevice> devices;
    private BluetoothSocket connectedSocket;
    ProgressDialog progress_search=null;


    private EditText txtState;
    private EditText txtMessages;
    private EditText txtToSend;

    // ************************************************
    // BtBackgroundTask
    // ************************************************

    private class BtBackgroundTask extends AsyncTask<BufferedReader, String, Void> {
        @Override
        protected Void doInBackground(BufferedReader... params) {
            try {
                while (!isCancelled()) {
                    publishProgress(params[0].readLine());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(getApplicationContext(), "[Recibido] " + values[0], Toast.LENGTH_LONG).show();

           // appendMessageText("[Recibido] " + values[0]);
        }
    }


    // ************************************************
    // ViewHolder for RecyclerView
    // ************************************************

    private class BtDevRvHolder extends RecyclerView.ViewHolder {
        private final TextView lblName;
        private final TextView lblAddress;

        private BluetoothDevice device;

        BtDevRvHolder(View itemView) {
            super(itemView);

            lblName = (TextView) itemView.findViewById(R.id.device_name);
            lblAddress = (TextView) itemView.findViewById(R.id.device_address);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(MainActivity.this, lblName);
                    popup.getMenuInflater().inflate(R.menu.device_popup, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.connect_menu_item:
                                    // Use a temporary socket until correct connection is done
                                    BluetoothSocket tmpSocket = null;

                                    // Connect with BluetoothDevice
                                    if (connectedSocket == null) {
                                        try {
                                            tmpSocket = device.createRfcommSocketToServiceRecord(MainActivity.SERIAL_PORT_UUID);

                                            // Get device's own Bluetooth adapter
                                            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                                            // Cancel discovery because it otherwise slows down the connection.
                                            btAdapter.cancelDiscovery();

                                            // Connect to the remote device through the socket. This call blocks until it succeeds or throws an exception
                                            tmpSocket.connect();

                                            // Acknowledge connected socket
                                            connectedSocket = tmpSocket;

                                            // Create socket reader thread
                                            BufferedReader br = new BufferedReader(new InputStreamReader(connectedSocket.getInputStream()));
                                            new BtBackgroundTask().execute(br);
                                            Toast.makeText(getApplicationContext(), "Conectado ", Toast.LENGTH_SHORT).show();

                                           // appendStateText("[Estado] Conectado.");
                                        } catch (IOException e) {
                                            try {
                                                if (tmpSocket != null) {
                                                    tmpSocket.close();
                                                }
                                            } catch (IOException closeExceptione) {
                                            }
                                            Toast.makeText(getApplicationContext(), "No se pudo establecer conexión! ", Toast.LENGTH_SHORT).show();

                                           // appendStateText("[Error] No se pudo establecer conexión!");
                                            e.printStackTrace();
                                        }
                                    }
                                    break;

                                default:
                                    break;
                            }

                            return true;
                        }
                    });

                    popup.show();
                }
            });
        }

        void bind(BluetoothDevice device) {
            this.device = device;
            lblName.setText(device.getName());
            lblAddress.setText(device.getAddress());
        }
    }


    // ************************************************
    // Adapter for RecyclerView
    // ************************************************

    private class BtDevRvAdapter extends RecyclerView.Adapter<BtDevRvHolder> {
        private List<BluetoothDevice> devices;

        BtDevRvAdapter(List<BluetoothDevice> devices) {
            this.devices = devices;
        }

        @Override
        public BtDevRvHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            return new BtDevRvHolder(inflater.inflate(R.layout.device_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(BtDevRvHolder holder, int position) {
            holder.bind(devices.get(position));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        void update() {
            notifyDataSetChanged();
        }
    }


    // ************************************************
    // MainActivity implementation
    // ************************************************

    // Create a BroadcastReceiver for ACTION_STATE_CHANGED
    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                // Bluetooth adapter state has changed
                switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(), "Bluetooth Apagado ", Toast.LENGTH_SHORT).show();
                        //  appendStateText("[Estado] Apagado.");
                        break;

                    case BluetoothAdapter.STATE_ON:
                     //   progress_search.dismiss();

                        Toast.makeText(getApplicationContext(), "Bluetooth Encendido ", Toast.LENGTH_SHORT).show();

                        // appendStateText("[Estado] Encendido.");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                      //  appendStateText("[Acción] Apagando...");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:

                      //  appendStateText("[Acción] Encendiendo...");
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_DISCOVERY_STARTED
    private final BroadcastReceiver btDiscoveryStartedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {

                progress_search = new ProgressDialog(MainActivity.this);
                progress_search.setMessage("Iniciando Busqueda...");
                progress_search.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress_search.show();

               // appendStateText("[Acción] Iniciando búsqueda de dispositivos...");
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_DISCOVERY_FINISHED
    private final BroadcastReceiver btDiscoveryFinishedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {


                if(devices.isEmpty())
                {

                    Toast.makeText(getApplicationContext(), "No se encontraron dispositivos ", Toast.LENGTH_SHORT).show();
                }


              //  appendStateText("[Acción] Finalizando búsqueda de dispositivos...");
                progress_search.dismiss();

            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver btFoundReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                devices.add(device);

                adapter.update();
                Toast.makeText(getApplicationContext(), "Dispositivo: " + device.getName() + " encontrado", Toast.LENGTH_SHORT).show();

              //  appendStateText("[Info] Dispositivo encontrado: " + device.getName() + ".");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get device's own Bluetooth adapter

        switch (item.getItemId()) {
            case R.id.menu_item_paired:
                devices.clear();
                adapter.update();

                if (id == R.id.action_settings) {
                    return true;
                }

                // Get paired devices
              //  appendStateText("[Acción] Buscando dispositivos sincronizados...");
                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

                progress_search = new ProgressDialog(MainActivity.this);
                progress_search.setMessage("Dispositivos Emparejados...");
                progress_search.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress_search.show();
                // Check if there are paired devices
                if (pairedDevices.size() > 0) {
                    if (pairedDevices.size() == 1) {
                    //    appendStateText("[Info] Se encontró 1 dispositivo.");
                    } else {
                        Toast.makeText(getApplicationContext(), "Se encontraron " + pairedDevices.size() + " dispositivos.", Toast.LENGTH_SHORT).show();

                        // appendStateText("[Info] Se encontraron " + pairedDevices.size() + " dispositivos.");
                    }

                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        devices.add(device);
                      //  appendStateText("[Info] Dispositivo sincronizado: " + device.getName() + ".");
                    }

                    adapter.update();
                    progress_search.dismiss();

                } else {
                    Toast.makeText(getApplicationContext(), " No se encontraron dispositivos sincronizados.", Toast.LENGTH_SHORT).show();

                    // appendStateText("[Info] No se encontraron dispositivos sincronizados.");
                }
                return true;

            case R.id.menu_item_discover:
                // Check if device supports Bluetooth
                if (btAdapter == null) {
                   // appendStateText("[Error] Dispositivo Bluetooth no encontrado!");
                }
                // Check if device adapter is not enabled
                else if (!btAdapter.isEnabled()) {
                    // Issue a request to enable Bluetooth through the system settings (without stopping application)
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
                // Start discovery process
                else {
                    devices.clear();
                    adapter.update();
                    btAdapter.startDiscovery();
                }
                return true;

            case R.id.menu_item_disconnect:
                if (connectedSocket != null) {
                    try {
                        connectedSocket.close();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), " Ocurrió un problema al intentar cerrar la conexión!", Toast.LENGTH_SHORT).show();
                      //  appendStateText("[Error] Ocurrió un problema al intentar cerrar la conexión!");
                        e.printStackTrace();
                    } finally {
                        connectedSocket = null;
                        Toast.makeText(getApplicationContext(), " Desconectado", Toast.LENGTH_SHORT).show();

                        // appendStateText("[Estado] Desconectado.");
                    }

                } else {
                    Toast.makeText(getApplicationContext(), " No hay  conexión con otro dispositivo ", Toast.LENGTH_SHORT).show();
                   // appendStateText("[Info] La conexión no parece estar activa.");
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    // CODIGO ON CREATE

    public static final String EXTRA_ID = "com.example.pch61m.homecontrol.extra_id";
    private Inventory inventory;
    private int id;
    private Users users;
    private Switch blue_switch;
    private boolean flag;
    private int  request_code=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent i = getIntent();

        id= i.getIntExtra(EXTRA_ID,0);


        if(savedInstanceState!= null)
        {
          //  id= savedInstanceState.getString(KEY_ID, "");

        }


        inventory= new Inventory(getApplicationContext());
        users= inventory.getUserFromID(id);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

     //  FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
     //  fab.setOnClickListener(new View.OnClickListener() {
     //      @Override
     //      public void onClick(View view) {
     //          Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
     //                  .setAction("Action", null).show();
     //      }
     //  });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View hView =  navigationView.getHeaderView(0);

        TextView email_user = (TextView)hView.findViewById(R.id.textView_email);
        TextView name_user = (TextView)hView.findViewById(R.id.textView_name);

        email_user.setText(users.getEmail());
        name_user.setText(users.getName() +" "+ users.getLastname());
        navigationView.setNavigationItemSelectedListener(this);

        //INICIO DE BLUETOOTH


        RecyclerView rvDevices = (RecyclerView) findViewById(R.id.devices_list);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));

        devices = new ArrayList<>();
        adapter = new BtDevRvAdapter(devices);
        rvDevices.setAdapter(adapter);


        // Setup message-to-send edit-text
        txtToSend = (EditText) findViewById(R.id.message_to_send_text);

        Button btnSend = (Button) findViewById(R.id.send_button);
         blue_switch = (Switch) findViewById(R.id.blue_switch);


        if (!btAdapter.isEnabled()) {
            blue_switch.setChecked(false);

        }
        else {
            blue_switch.setChecked(true);

        }



        blue_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

             if (blue_switch.isChecked())
             {
                 //   Toast.makeText(getApplicationContext(), "  true ", Toast.LENGTH_SHORT).show();
                 if (!btAdapter.isEnabled()) {
                     // Issue a request to enable Bluetooth through the system settings (without stopping application)
                     Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                     startActivityForResult(intent, REQUEST_ENABLE_BT);
                 }

             }else{
                 // Toast.makeText(getApplicationContext(), "  false ", Toast.LENGTH_SHORT).show();
                 if (btAdapter.isEnabled()) {
                     //btAdapter.disable();
                      Intent cancel = new Intent(getApplicationContext(), Cancel_Confirmation.class);
                     startActivityForResult(cancel, request_code);

                 }

             }

            }
        });






        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ... Transmisión de datos
                try {
                    if ((connectedSocket != null) && (connectedSocket.isConnected())) {
                        String toSend = txtToSend.getText().toString().trim();

                        if (toSend.length() > 0) {
                            // TBI - This object "should" be a member variable
                            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connectedSocket.getOutputStream()));
                            bw.write(toSend);
                            bw.write("\r\n");
                            bw.flush();
                            Toast.makeText(getApplicationContext(), "  Enviado "+ toSend, Toast.LENGTH_SHORT).show();
                           // appendMessageText("[Enviado] " + toSend);
                        }

                        txtToSend.setText("");
                    } else {
                        Toast.makeText(getApplicationContext(), "  No se está conectado con algún dispositivo" , Toast.LENGTH_SHORT).show();

                        //   appendStateText("[Error] La conexión no parece estar activa!");
                    }
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), " Ocurrió un problema durante el envío de datos!" , Toast.LENGTH_SHORT).show();

                    // appendStateText("[Error] Ocurrió un problema durante el envío de datos!");
                    e.printStackTrace();
                }
            }
        });

        // Register for broadcasts when bluetooth device state changes
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btStateReceiver, filter);

        // Register for broadcasts when bluetooth discovery state changes
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(btDiscoveryStartedReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(btDiscoveryFinishedReceiver, filter);

        // Register for broadcasts when a device is discovered.
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(btFoundReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);



        switch (resultCode) {
            case RESULT_OK:
                if (requestCode == REQUEST_ENABLE_BT) {
                    // Get device's own Bluetooth adapter
                    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                    // Start discovery process
                    devices.clear();
                    adapter.update();
                    btAdapter.startDiscovery();
                }
                break;

            case RESULT_CANCELED:
            default:
                Toast.makeText(getApplicationContext(), "El dispositivo Bluetooth no pudo ser habilitado! " , Toast.LENGTH_SHORT).show();

                //  appendStateText("[Error] El dispositivo Bluetooth no pudo ser habilitado!");
                break;
        }

        if ( requestCode==request_code && resultCode== RESULT_OK)
        {
              btAdapter.disable();
           // Toast.makeText(getApplicationContext(),"SIMON Apagado", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister the ACTION_STATE_CHANGE receiver
        unregisterReceiver(btStateReceiver);

        // Unregister the ACTION_DISCOVERY_STARTED receiver.
        unregisterReceiver(btDiscoveryStartedReceiver);

        // Unregister the ACTION_DISCOVERY_FINISHED receiver.
        unregisterReceiver(btDiscoveryFinishedReceiver);

        // Unregister the ACTION_FOUND receiver
        unregisterReceiver(btFoundReceiver);
    }

    private void appendStateText(String text) {
        txtState.setText(text + "\n" + txtState.getText());
    }

    private void appendMessageText(String text) {
        txtMessages.setText(text + "\n" + txtMessages.getText());
    }








    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

//NOOOOOOOOOOOOOOOOOOOOOOOOOTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        // SI QUIERES CREAR OTRA COMO EXTERIOR AGREGAS UN NUEVO FRAGMENT EN BLANCO  Y LO UNICO QUE TIENES QU CAMBIAR SERIA DENTRO DEL IF PONER FRAGMENT= NEW FRAGMENTNUEVO();  Y LISTO
//ddddeeff
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Fragment fragment= null;
        Boolean Fragment_selected=false;


        if (id == R.id.nav_exterior) {
            // Handle the camera action
           fragment = new FragmentExterior();
           // getSupportFragmentManager().beginTransaction().replace(R.id.content_main, fragment).commit();


             Toast.makeText(getApplicationContext(), "HOLA", Toast.LENGTH_SHORT).show();

        } else if (id == R.id.nav_interior) {


        } else if (id == R.id.nav_room1) {

        } else if (id == R.id.nav_room2) {

        } else if (id == R.id.nav_sala) {

        }
        else if (id == R.id.nav_users) {

        }
        else if (id == R.id.nav_profiles) {

        }
     if (fragment!=null){
      //   Toast.makeText(getApplicationContext(), "HOLA", Toast.LENGTH_SHORT).show();

         FragmentTransaction ft= getSupportFragmentManager().beginTransaction();
         ft.replace(R.id.content_main, fragment);
         ft.commit();

     }



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
