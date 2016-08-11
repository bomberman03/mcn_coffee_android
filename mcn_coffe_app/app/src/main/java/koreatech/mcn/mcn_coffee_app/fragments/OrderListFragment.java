package koreatech.mcn.mcn_coffee_app.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import koreatech.mcn.mcn_coffe_app.R;
import koreatech.mcn.mcn_coffee_app.activities.MainActivity;
import koreatech.mcn.mcn_coffee_app.activities.OrderActivity;
import koreatech.mcn.mcn_coffee_app.adapter.OrderListRecyclerViewAdapter;
import koreatech.mcn.mcn_coffee_app.adapter.OrderRecyclerViewAdapter;
import koreatech.mcn.mcn_coffee_app.config.Settings;
import koreatech.mcn.mcn_coffee_app.models.Cafe;
import koreatech.mcn.mcn_coffee_app.models.MenuModel;
import koreatech.mcn.mcn_coffee_app.models.Option;
import koreatech.mcn.mcn_coffee_app.models.Order;
import koreatech.mcn.mcn_coffee_app.models.OrderList;
import koreatech.mcn.mcn_coffee_app.request.CustomArrayRequest;

/**
 * Created by blood_000 on 2016-05-24.
 */
public class OrderListFragment extends TabFragment{

    private String authentication_key;

    private ArrayList<OrderList> orderLists = new ArrayList<>();

    private RecyclerView recyclerView;
    private OrderListRecyclerViewAdapter orderListRecyclerViewAdapter;

    private Socket mSocket;

    public void generateOrder(){
        ArrayList<Order> orders = new ArrayList<>();
        for(int i=0; i<6; i++) {
            List<Option> options = new ArrayList<>();
            List<Option> shots = new ArrayList<>();
            Option shot1 = new Option("0", "1샷 추가", 0, null);
            shots.add(shot1);
            Option shot2 = new Option("1", "2샷 추가", 600, null);
            shots.add(shot2);
            Option shot3 = new Option("2", "3샷 추가", 900, null);
            shots.add(shot3);
            Option shot = new Option("3", "샷 추가", 0, shots);
            options.add(shot);
            Option cream = new Option("4", "크림 추가", 500, null);
            options.add(cream);
            MenuModel americano = new MenuModel("0", "아메리카노",
                    "쓰지만 계속 먹으면 중독되는 이 맛", 3000, options);
            Order order = new Order("0", americano, shots, 3000, 1);
            orders.add(order);
        }
        OrderList orderList0 = new OrderList("0", orders, 3000, 12345, 0);
        orderLists.add(orderList0);
        OrderList orderList1 = new OrderList("0", orders, 5000, 12346, 1);
        orderLists.add(orderList1);
    }

    public void init(View view){
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    }

    public void initRecyclerView(){
        orderListRecyclerViewAdapter = new OrderListRecyclerViewAdapter(getContext(), this, orderLists);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(orderListRecyclerViewAdapter);
    }

    public void connectSocket(){
        try {
            mSocket = IO.socket("http://" + Settings.serverIp + ":" + Settings.socket_port);
        } catch (URISyntaxException e) {
            Log.d("TAG",e.getMessage());
        }
        mSocket.connect();
        Cafe cafe = ((OrderActivity)getActivity()).getCafe();
        mSocket.on(cafe.id, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject jsonObject = (JSONObject) args[0];
                try {
                    Object data  = "";
                    if(jsonObject.has("data")) data = jsonObject.get("data");
                    String method = "";
                    if(jsonObject.has("method")) method = jsonObject.getString("method");
                    String name = "";
                    if(jsonObject.has("name")) name = jsonObject.getString("name");
                    String id = "";
                    if(jsonObject.has("id")) id = jsonObject.getString("id");
                    Log.d("DEBUG",method);
                    Log.d("DEBUG",name);
                    Log.d("DEBUG",id);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSocket.disconnect();
        Cafe cafe = ((OrderActivity)getActivity()).getCafe();
        mSocket.off(cafe.id);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        checkAuthKey();
        View view = inflater.inflate(R.layout.fragment_order_list, container, false);
        init(view);
        initRecyclerView();
        content_request();
        connectSocket();

        return view;
    }

    public void checkAuthKey(){
        SharedPreferences pref = getActivity().getSharedPreferences("pref", getActivity().MODE_PRIVATE);
        authentication_key = pref.getString("authentication_key", "");
        if(authentication_key.length() > 0) {
            // if authentication_key is not valid
        }
    }

    public void content_request(){

        RequestQueue queue = Volley.newRequestQueue(getContext());
        Cafe cafe = ((OrderActivity) getActivity()).getCafe();
        String url = "http://" + Settings.serverIp + ":" + Settings.port + "/cafes/" + cafe.id + "/orders/";

        Map<String, String> params = new HashMap<>();

        CustomArrayRequest cafeListRequest = new CustomArrayRequest(Request.Method.GET, url, params, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jsonArray) {
                orderLists.clear();
                for(int i=0; i<jsonArray.length(); i++){
                    try {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        OrderList orderList = new OrderList(jsonObject);
                        orderLists.add(orderList);
                        orderListRecyclerViewAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.d("TAG", volleyError.getMessage());
            }
        });
        // Add the request to the RequestQueue.
        queue.add(cafeListRequest);
    }
}
