/* Copyright (C) 2014  olie.xdev <olie.xdev@googlemail.com>
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.health.openscale.gui.preferences;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

import com.health.openscale.R;
import com.health.openscale.gui.ReminderBootReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ReminderPreferences extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private CheckBoxPreference reminderEnable;
    private MultiSelectListPreference reminderWeekdays;
    private TimePreferenceDialog reminderTime;
    private EditTextPreference reminderNotifyText;

    private static ArrayList<PendingIntent> pendingAlarms = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.reminder_preferences);

        reminderEnable = (CheckBoxPreference)findPreference("reminderEnable");
        reminderWeekdays = (MultiSelectListPreference)findPreference("reminderWeekdays");
        reminderTime = (TimePreferenceDialog)findPreference("reminderTime");
        reminderNotifyText = (EditTextPreference)findPreference("reminderNotifyText");

        updateAlarmPreferences();
        initSummary(getPreferenceScreen());
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePrefSummary(findPreference(key));
        updateAlarmPreferences();
    }

    public void updateAlarmPreferences() {
        ComponentName receiver = new ComponentName(getActivity().getApplicationContext(), ReminderBootReceiver.class);
        PackageManager pm = getActivity().getApplicationContext().getPackageManager();

        if (reminderEnable.isChecked()) {
            scheduleAlarms(getActivity());

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            reminderWeekdays.setEnabled(true);
            reminderTime.setEnabled(true);
            reminderNotifyText.setEnabled(true);
        } else {
            disableAllAlarms(getActivity());

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            reminderWeekdays.setEnabled(false);
            reminderTime.setEnabled(false);
            reminderNotifyText.setEnabled(false);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }

        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            if (p.getTitle().toString().contains("assword"))
            {
                p.setSummary("******");
            } else {
                p.setSummary(editTextPref.getText());
            }
        }

        if (p instanceof MultiSelectListPreference) {
            MultiSelectListPreference editMultiListPref = (MultiSelectListPreference) p;

            CharSequence[] entries = editMultiListPref.getEntries();
            CharSequence[] entryValues = editMultiListPref.getEntryValues();
            List<String> currentEntries = new ArrayList<>();
            Set<String> currentEntryValues = editMultiListPref.getValues();

            for (int i = 0; i < entries.length; i++)
                if (currentEntryValues.contains(entryValues[i]))
                    currentEntries.add(entries[i].toString());

            p.setSummary(currentEntries.toString());
        }
    }

    public static void scheduleAlarms(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> reminderWeekdays = prefs.getStringSet("reminderWeekdays", new HashSet<String>());
        Long reminderTimeInMillis = prefs.getLong("reminderTime", System.currentTimeMillis());

        Iterator iterWeekdays = reminderWeekdays.iterator();

        disableAllAlarms(context);

        while (iterWeekdays.hasNext()) {
            String strWeekdays = iterWeekdays.next().toString();

            switch (Integer.parseInt(strWeekdays)){
                case 0:
                    pendingAlarms.add(enableAlarm(context, Calendar.MONDAY, reminderTimeInMillis));
                    break;
                case 1:
                    pendingAlarms.add(enableAlarm(context, Calendar.TUESDAY, reminderTimeInMillis));
                    break;
                case 2:
                    pendingAlarms.add(enableAlarm(context, Calendar.WEDNESDAY, reminderTimeInMillis));
                    break;
                case 3:
                    pendingAlarms.add(enableAlarm(context, Calendar.THURSDAY, reminderTimeInMillis));
                    break;
                case 4:
                    pendingAlarms.add(enableAlarm(context, Calendar.FRIDAY, reminderTimeInMillis));
                    break;
                case 5:
                    pendingAlarms.add(enableAlarm(context, Calendar.SATURDAY, reminderTimeInMillis));
                    break;
                case 6:
                    pendingAlarms.add(enableAlarm(context, Calendar.SUNDAY, reminderTimeInMillis));
                    break;
            }
        }

    }

    public static PendingIntent enableAlarm(Context context, int dayOfWeek, long timeInMillis) {
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.setTimeInMillis(timeInMillis);
        alarmCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        // Check we aren't setting it in the past which would trigger it to fire instantly
        if(alarmCal.getTimeInMillis() < System.currentTimeMillis()) {
            alarmCal.add(Calendar.DAY_OF_YEAR, 7);
        }

        //Log.d("ReminderPreferences", "Set alarm to " + calendar.getTime());

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, ReminderBootReceiver.class);
        alarmIntent.putExtra("alarmIntent", true);

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(context, dayOfWeek, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 7, alarmPendingIntent);

        return alarmPendingIntent;
    }

    public static void disableAllAlarms(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        for (int i=0; i<pendingAlarms.size(); i++) {
            alarmMgr.cancel(pendingAlarms.get(i));
        }

        pendingAlarms.clear();
    }
}

