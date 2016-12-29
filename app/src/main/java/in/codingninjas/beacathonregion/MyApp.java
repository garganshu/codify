package in.codingninjas.beacathonregion;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import in.codingninjas.beacathonregion.network.ApiClient;
import in.codingninjas.beacathonregion.network.NetworkDataManager;
import in.codingninjas.beacathonregion.network.responses.ApiResponse;
import in.codingninjas.beacathonregion.utils.UserUtil;
import retrofit2.Call;

/**
 * Created by rohanarora on 22/12/16.
 */

public class MyApp extends Application implements BeaconConsumer {

    private static MyApp instance = null;
    private BeaconManager beaconManager;
    private static final Identifier nameSpaceId = Identifier.parse("0x5dc33487f02e477d4058");

   // public CopyOnWriteArrayList<String> parkingspotOccupiedList;
    public CopyOnWriteArrayList<Region> parkingList;
    public boolean inside = false;
    public boolean outside = true;
    public HashMap<String,Region> ssnParkingMap;
    public OnListRefreshListener onListRefreshListener;
    public MainActivity context;
    public boolean status = false;

    public interface OnListRefreshListener {
        void onListRefresh();
    }

    public static MyApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //check signed in
            setUpBeacon();

    }

    public void setUpBeacon(){
        ssnParkingMap = new HashMap<>();
        parkingList = new CopyOnWriteArrayList<>();
      //  parkingspotOccupiedList = new CopyOnWriteArrayList<>();

        ssnParkingMap.put("0x0117c59825E9",new Region("P1",nameSpaceId, Identifier.parse("0x0117c59825E9"),null));
        ssnParkingMap.put("0x0117c55be3a8",new Region("P2",nameSpaceId,Identifier.parse("0x0117c55be3a8"),null));
        ssnParkingMap.put("0x0117c552c493",new Region("P3",nameSpaceId,Identifier.parse("0x0117c552c493"),null));
        ssnParkingMap.put("0x0117c55fc452",new Region("P4",nameSpaceId,Identifier.parse("0x0117c55fc452"),null));
        ssnParkingMap.put("0x0117c555c65f",new Region("P5",nameSpaceId,Identifier.parse("0x0117c555c65f"),null));
        ssnParkingMap.put("0x0117c55d6660",new Region("P6",nameSpaceId,Identifier.parse("0x0117c55d6660"),null));
        ssnParkingMap.put("0x0117c55ec086",new Region("P7",nameSpaceId,Identifier.parse("0x0117c55ec086"),null));

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        new BackgroundPowerSaver(this);
        beaconManager.bind(this);

    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {

            }

            @Override
            public void didExitRegion(Region region) {

            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                String regionName = region.getUniqueId();
                String beaconSSN = region.getId2().toHexString();
                switch (i){
                    case INSIDE:
                        Log.i("TAG","Enter " + regionName);
                       // parkingspotOccupiedList.add(regionName);//add parking slot id to occupied
                        parkingList.add(region); //add parking slot
                        inside = true;
                        outside = false;
                        status =true;
                        MyApp.notifyListChange();
                       // Toast.makeText(getApplicationContext(),"Found beacon",Toast.LENGTH_SHORT).show();
                       // MyApp.showNotification("Found beacon");
                        //enterRegion(beaconSSN);
                        break;
                    case OUTSIDE:
                        Log.i("TAG","Outside " + regionName);
                      /*  if(parkingspotOccupiedList.contains(regionName)){
                            parkingspotOccupiedList.remove(regionName);
                            status =false;
                        }*/
                        if(parkingList.contains(region)) {
                            parkingList.remove(region);
                            MyApp.notifyListChange();
                        }

                        inside = false;
                        outside = true;
                        status = false;
                        MyApp.notifyListChange();
                        //exitRegion(beaconSSN);
                      //  MyApp.showNotification("Exit beacon");
                        // Toast.makeText(getApplicationContext(),"Exit beacon",Toast.LENGTH_SHORT).show();
                        break;
                }

             /*   ArrayList<String> list_beaconSSN = new ArrayList<String>();
                for(Region r: parkingList){
                    list_beaconSSN.add(r.getId2().toHexString());
                }
                updateParkingoccupiedList(list_beaconSSN);*/


            }
        });


        try {
            for(String key:ssnParkingMap.keySet()) {
                Region region = ssnParkingMap.get(key);
                beaconManager.startMonitoringBeaconsInRegion(region);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
//not used
    private static void showNotification(String message){
        NotificationCompat.Builder mBuilder =   new NotificationCompat.Builder(instance)
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle("Status!") // title for notification
                .setContentText(message) // message for notification
                .setAutoCancel(true); // clear notification after click
        NotificationManager mNotificationManager =
                (NotificationManager) instance.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());

    }
//not used
    private void enterRegion(final String beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Enter Update Success for beacon: " + beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Enter Update Fail for beacon: " + beaconSSN);
            }
        };
        Call<ApiResponse> call= ApiClient.authorizedApiService().addUserInRegion(beaconSSN);
        manager.execute(call,listener);
    }
//notused
    private void exitRegion(final String beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Exit Update Success for beacon: " + beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Exit Update Fail for beacon: " + beaconSSN);
            }
        };
        Call<ApiResponse> call = ApiClient.authorizedApiService().removeUserFromRegion(beaconSSN);
        manager.execute(call,listener);
    }
//updateparrking slots in parking list
    private void updateParkingoccupiedList(final ArrayList<String> list_beaconSSN){
        NetworkDataManager<ApiResponse> manager = new NetworkDataManager<>();
        NetworkDataManager.NetworkResponseListener listener = manager.new NetworkResponseListener() {
            @Override
            public void onSuccessResponse(ApiResponse response) {
                Log.i("TAG","Enter Update Success for beacon: " + list_beaconSSN);
            }

            @Override
            public void onFailure(int code, String message) {
                Log.i("TAG","Enter Update Fail for beacon: " + list_beaconSSN);
            }
        };
        String list_beaconCSV = list_beaconSSN.toString().replace("[", "").replace("]", "").replace(", ", ",");
        Call<ApiResponse> call= ApiClient.authorizedApiService().updateUserInRegions(list_beaconCSV);
        manager.execute(call,listener);
    }
//notify change
    private static void notifyListChange(){
        if (instance.context != null && instance.onListRefreshListener != null) {
            instance.context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MyApp.instance.onListRefreshListener.onListRefresh();
                }
            });
        }
    }


}
