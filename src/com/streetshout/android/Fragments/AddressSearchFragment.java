package com.streetshout.android.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import com.streetshout.android.R;
import com.streetshout.android.Utils.LocationUtils;

public class AddressSearchFragment extends Fragment {

    private static final int ANIMATE_MAP_TO_ADDRESS = 17584;

    private EditText addressSearchView = null;

    private Geocoder geocoder = null;

    private OnAddressValidateListener onAddressValidateListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        geocoder = new Geocoder(getActivity());

        return inflater.inflate(R.layout.address_search_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        addressSearchView = (EditText) getView().findViewById(R.id.nav_search_address_view);

        addressSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((EditText) v).setError(null);
            }
        });

        addressSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId== EditorInfo.IME_ACTION_SEARCH) {
                    geocodeAddress(v);
                }
                return false;
            }
        });
    }

    public void setFocusOnSearchAddressView() {
        addressSearchView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(addressSearchView, InputMethodManager.SHOW_IMPLICIT);
    }

    public void removeFocusFromSearchAddressView() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(addressSearchView.getWindowToken(), 0);
    }

    private void geocodeAddress(final TextView editTextView) {
        String dialogText = String.format(getString(R.string.nav_address_geocoding_processing), '"' + editTextView.getText().toString() + '"');
        final ProgressDialog addressDialog = ProgressDialog.show(getActivity(), "", dialogText, false);

        final Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == ANIMATE_MAP_TO_ADDRESS) {
                    Address address = (Address) msg.obj;

                    if (address != null) {
                        try {
                            double addressLat = address.getLatitude();
                            double addressLng = address.getLongitude();

                            addressDialog.cancel();
                            onAddressValidateListener.onAddressValidate(addressLat, addressLng);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    addressDialog.cancel();
                    editTextView.setError(getString(R.string.address_geocoding_failed));
                    editTextView.setText("");
                }

                super.handleMessage(msg);
            }
        };

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Address address = LocationUtils.geocodeAddress(geocoder, editTextView.getText().toString(), null);
                    Message msg = handler.obtainMessage();
                    msg.what = ANIMATE_MAP_TO_ADDRESS;
                    msg.obj = address;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            onAddressValidateListener = (OnAddressValidateListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onAddressValidateListener");
        }
    }

    public interface OnAddressValidateListener {
        public void onAddressValidate(double lat, double lng);
    }
}
