package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.vfdev.gettingthingsdonemusicapp.Animations.DefaultAnimations;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.ProviderMetaInfo;
import com.vfdev.mimusicservicelib.core.ProviderQuery;

import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

public class SettingsFragment extends Fragment implements
        AdapterView.OnItemSelectedListener,
        EditText.OnEditorActionListener
{

    // Ui
    @InjectView(R.id.fs_tags)
    EditText mTagsText;
    @InjectView(R.id.fs_track_duration)
    Spinner mTrackDurationSpinner;
    @InjectView(R.id.fs_providers)
    GridView mProvidersGridView;
    @InjectView(R.id.fs_update)
    TextView mUpdate;
    @InjectView(R.id.fs_reset)
    TextView mReset;

    ProvidersAdapter mProvidersAdapter;

    // Button animations
    DefaultAnimations mAnimations = new DefaultAnimations();


    boolean safeResetChecker=true;

    // flag to restore ui at onReady() or onPause() methods
    protected boolean needRestoreUi=false;

    // Music Service
    protected MusicServiceHelper mMSHelper;

    // Toast dialog
    private Toast mToast;


    // ----------- Fragment methods

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Timber.v("onCreate");
    }

    // onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.v("onCreateView");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        ButterKnife.inject(this, view);

        // setup Ui:
        setupUi();

        needRestoreUi = true;
        return view;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Timber.v("onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("onStart");
        mToast = Toast.makeText(getActivity().getApplicationContext(), "", Toast.LENGTH_LONG);
//        EventBus.getDefault().register(this);
        mMSHelper = MusicServiceHelper.getInstance();
    }

    // onResume
    @Override
    public void onResume(){
        Timber.v("onResume");
        super.onResume();

        // restore UI state:
        loadSettings();
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        needRestoreUi = true;
        super.onPause();
    }

    @Override
    public void onStop() {
        Timber.v("onStop");
        // Prefer to remove event handler when Fragment is stopped
//        EventBus.getDefault().unregister(this);
        // remove all pointers to singletons
        mMSHelper = null;
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Timber.v("onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Timber.v("onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Timber.v("onDetach");
        super.onDetach();
    }

    // ------ Callbacks

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Timber.i("Spinner onItemSelected : position=" + position);
        if (!mUpdate.isEnabled()) mUpdate.setEnabled(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to do
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event.getAction() == KeyEvent.ACTION_DOWN && (
                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER ||
                        event.getKeyCode() == KeyEvent.KEYCODE_BACK)) {
            if (!event.isShiftPressed()) {
                // the user is done typing.
                Timber.i("Edit text is finished");
                if (!mUpdate.isEnabled()) mUpdate.setEnabled(true);
                return true; // consume.
            }
        }
        return false; // pass on to other listeners.
    }


    @OnClick(R.id.fs_update)
    public void onUpdate(View view) {
        mUpdate.startAnimation(mAnimations.getButtonAnimation());
        Timber.v("onUpdate settings");
        String tags = mTagsText.getText().toString();
        int durationPref = mTrackDurationSpinner.getSelectedItemPosition();
        int providers = mProvidersAdapter.getSelectedItems();
        setupSettings(tags, durationPref, providers);

        Context context = getActivity().getApplicationContext();
        Toast.makeText(context, context.getString(R.string.settings_updated), Toast.LENGTH_SHORT).show();
        mUpdate.setEnabled(false);
    }

    @OnClick(R.id.fs_reset)
    public void onReset(View view) {

        mReset.startAnimation(mAnimations.getButtonAnimation());
        if (safeResetChecker) {
            if (mToast != null) {
                mToast.setText(R.string.settings_double_click);
                mToast.show();
            }
            view.setBackgroundColor(Color.argb(172, 250, 0, 0));
            safeResetChecker = false;
            return;
        }

        Timber.v("onReset settings to default");
        String tags = getString(R.string.settings_default_tags);
        int durationPref = 0;
        int providers = -1;
        setupSettings(tags, durationPref, providers);
        loadSettings();

        safeResetChecker = true;
        view.setBackgroundColor(getResources().getColor(R.color.transparentColor1));
    }


    // ----------- Static public methods

    static public ProviderQuery getQuery(Context context) {
        return SettingsFragment.getQuery(
                SettingsFragment.getTags(context),
                SettingsFragment.getTrackDurationPref(context)
        );
    }

    static public ProviderQuery getQuery(String tags, int durationPref) {
        ProviderQuery query = new ProviderQuery();
        query.text = tags;
        switch (durationPref) {
            case 1: // Less than 5 minutes
                query.durationMax = 5*60*1000;
                break;
            case 2: // Less than 30 minutes
                query.durationMax = 30*60*1000;
                break;
            case 3: // Only long sessions
                query.durationMin = 30*60*1000; // more than 30 minutes
                break;
            case 0: // Any duration
            default:
                break;
        }
        return query;
    }

    /**
     * Method to return selected providers as bit flags from shared preferences
     * 1st provider is 1
     * 2nd provider is 2
     * 3rd provider is 4
     * 4th provider is 8
     * etc
     * @param context
     * @return integer corresponding to selecteProvider1 | selecteProvider2 | ... | selecteProviderN
     */
    static private int getProviders(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        return prefs.getInt("providers", -1);
    }

    static public String[] getProviderNames(Context context) {
        List<ProviderMetaInfo> allProvidersInfo = MusicServiceHelper.availableProviders();
        int selectedProviders = getProviders(context);
        String [] output = new String[allProvidersInfo.size()];
        if (selectedProviders == -1) {
            int count = 0;
            for (ProviderMetaInfo p : allProvidersInfo) {
                output[count] = p.name;
                count++;
            }
            return output;
        }
        // decode bitflag result of selected providers
        int i = 1, j=0;
        int count = 0;
        for (;count<output.length;count++,i*=2) {
            if ((selectedProviders & i) == i) { // bitwise compare with count-th provider
                output[j] = allProvidersInfo.get(count).name;
                j++;
            }
        }
        return output;
    }

    static public String getTags(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        return prefs.getString("tags", context.getString(R.string.settings_default_tags));
    }

    static public int getTrackDurationPref(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        return prefs.getInt("track_duration_pref", 0); // Default preference is 0 (Less than 5 minutes)
    }

    static public void setTags(Context context, String tags) {
        Timber.v("setTags : " + tags);
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("tags", tags);
        // Commit the edits!
        editor.apply();
    }

    static public void setTrackDurationPref(Context context, int index) {
        Timber.v("setTrackDurationPref : " + index);
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("track_duration_pref", index);
        editor.apply();
    }

    static private void setProviders(Context context, int providers) {
        Timber.v("setProviders : " + providers);
        SharedPreferences prefs = context.getSharedPreferences("GTDM", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("providers", providers);
        editor.apply();
    }

    static public void setProviderNames(Context context, String [] providers) {
        Timber.v("setProviderNames : " + providers);
        List<Pair<String, Integer>> allProvidersInfo = MusicServiceHelper.allProvidersInfo();
        int result = 0;
        // encode the bitflag result of selected providers
        int i = 1, j=0;
        int count = 0;
        for (;count<allProvidersInfo.size();count++,i*=2) {
            String name = allProvidersInfo.get(count).first;
            for (;j<providers.length;j++) {
                if (name.equalsIgnoreCase(providers[j])) {
                    result |= i;
                    break;
                }
            }
        }
        setProviders(context, result);
    }

    // -------- Private/Protected methods

    private void setupSettings(String tags, int durationPref, int providers) {

        Context context = getActivity().getApplicationContext();
        if (tags.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.settings_update_error), Toast.LENGTH_SHORT).show();
            return;
        }

        SettingsFragment.setTags(context, tags);
        SettingsFragment.setTrackDurationPref(context, durationPref);
        SettingsFragment.setProviders(context, providers);

        // start retrieving tracks for new tags
        mMSHelper.clearPlaylist();
        mMSHelper.setupTracks(SettingsFragment.getQuery(tags, durationPref));

    }


    protected void showMessage(String msg) {
        mToast.setText(msg);
        mToast.show();
    }


    protected void setupUi() {
        Context context = getActivity().getApplicationContext();
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                context,
                R.array.settings_durations,
                android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mTrackDurationSpinner.setAdapter(adapter);
        mTrackDurationSpinner.setOnItemSelectedListener(this);

        mTagsText.setOnEditorActionListener(this);

        // setup providers view
        mProvidersAdapter = new ProvidersAdapter(MusicServiceHelper.availableProviders());
        mProvidersGridView.setAdapter(mProvidersAdapter);

    }

    protected void loadSettings() {
        Context context = getActivity().getApplicationContext();
        mTagsText.setText(SettingsFragment.getTags(context));
        mTrackDurationSpinner.setSelection(SettingsFragment.getTrackDurationPref(context));
        mProvidersAdapter.setSelectedItems(SettingsFragment.getProviders(context));
        mProvidersAdapter.notifyDataSetChanged();
        mUpdate.setEnabled(false);
    }


    // ------------ ProvidersAdapter
    private class ProvidersAdapter extends BaseAdapter
            implements View.OnClickListener
    {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        private List<ProviderMetaInfo> available;
        private boolean[] selectedItems;

        //Constructor to initialize values
        public ProvidersAdapter(List<ProviderMetaInfo> available) {
            this.available = available;
            selectedItems = new boolean[this.available.size()]; // Initialized to false by default
        }

        // input items are encoded as
        public void setSelectedItems(int items) {
            Timber.v("Set selected provider : " + items);
            int size = selectedItems.length;
            selectedItems = new boolean[size]; // Initialized to false by default
            if (items == -1) {
                // all are selected
                Arrays.fill(selectedItems, true);
                return;
            }
            int i = 1;
            int count = 0;
            for (;count<size;count++,i*=2) {
                if ((items & i) == i) { // bitwise compare with count-th provider
                    selectedItems[count] = true;
                }
            }
        }

        public int getSelectedItems() {
            // encode the bitflag:
            int result = 0;
            int i = 1;
            int count = 0;
            for (;count<selectedItems.length;count++,i*=2) {
                if (selectedItems[count]) {
                    result |= i;
                }
            }
            return result;
        }


        @Override
        public int getCount() {
            // Number of times getView method call depends upon gridValues.length
            return selectedItems.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        // Number of times getView method call depends upon gridValues.length
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder h;

            if (null == convertView) {
                convertView = inflater.inflate(R.layout.settings_provider_item, null);
                h = new ViewHolder();
                h.icon = (ImageView) convertView.findViewById(R.id.spi_icon);
                h.checkBox = (CheckBox) convertView.findViewById(R.id.spi_checkbox);

                convertView.setTag(h);
                h.checkBox.setOnClickListener(this);
                h.icon.setTag(h.checkBox);
                h.icon.setImageResource(available.get(position).drawable);
                h.icon.setOnClickListener(this);
            } else {
                h = (ViewHolder) convertView.getTag();
            }

            h.checkBox.setChecked(selectedItems[position]);

            return convertView;
        }

        // -----------
        private class ViewHolder {
            CheckBox checkBox;
            ImageView icon;
        }

        // ----------- OnItemClickListener
        @Override
        public void onClick(View view) {
            int position = mProvidersGridView.getPositionForView(view);
            CheckBox cb = null;
            if (view instanceof CheckBox) {
                cb = (CheckBox) view;
            } else if (view instanceof ImageView) {
                cb = (CheckBox) view.getTag();
                cb.setChecked(!cb.isChecked());
            } else {
                return;
            }

            selectedItems[position] = cb.isChecked();
            String providerName = available.get(position).name;
            Timber.v("Provider \'" + providerName + "\' is selected : " + selectedItems[position]);
            if (selectedItems[position]) {
                mMSHelper.addTrackInfoProvider(providerName);
            } else {
                mMSHelper.removeTrackInfoProvider(providerName);
            }
            Timber.v("MusicService Providers : " + mMSHelper.getTrackInfoProviderNames());
        }

    };


}
