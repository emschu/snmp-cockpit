/*
 * SNMP Cockpit Android App
 *
 * Copyright (C) 2018-2019
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.emschu.snmp.cockpit.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.activity.TagManagementActivity;
import org.emschu.snmp.cockpit.adapter.CustomQueryAdapter;
import org.emschu.snmp.cockpit.persistence.CockpitDbHelper;
import org.emschu.snmp.cockpit.persistence.model.CustomQuery;
import org.emschu.snmp.cockpit.persistence.model.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * this class represents the query management fragment
 */
public class OwnQueryFragment extends Fragment {

    public static final String TAG = OwnQueryFragment.class.getName();
    private static final List<Tag> HARDWARE_TAGS = new ArrayList<>();
    public static final String CURRENT_OID_FORM_KEY = "current_oid_form_key";
    public static final String CURRENT_NAME_FORM_KEY = "current_name_form_key";
    public static final String QUESTION_MODE_FORM_KEY = "question_mode_form_key";
    public static final String TAG_LIST_TEXT_VIEW_FORM_KEY = "tag_list_text_view_form_key";
    public static final String CURRENT_TAG_SELECTION_FORM_KEY = "current_tag_selection_form_key";
    public static final String LAST_QUERY_ID_FORM_KEY = "last_query_id_form_key";
    public static final String IS_DIALOG_SHOWN_FORM_KEY = "is_dialog_shown_form_key";
    private final List<Tag> currentTagSelection = new ArrayList<>();
    private CockpitDbHelper cockpitDbHelper;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter<?> adapter;
    private View dialogView;
    private EditText oidTextField;
    private EditText queryNameField;
    private CheckBox questionModeField;
    private TextView tagListTextView;
    private CustomQuery lastQuery;
    private AlertDialog alertDialog;
    private ImageButton deleteTagsButton;
    private AutoCompleteTextView autoCompleteTextView;
    private TextView noQueriesText;

    public OwnQueryFragment() {
        // mandatory empty constructor of fragment
    }

    public static OwnQueryFragment newInstance() {
        OwnQueryFragment fragment = new OwnQueryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cockpitDbHelper = new CockpitDbHelper(getContext());
        if (HARDWARE_TAGS.isEmpty()) {
            List<Tag> allTags = cockpitDbHelper.getAllTags();
            Log.d(TAG, "current tags: " + allTags);
            HARDWARE_TAGS.addAll(allTags);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        View view = inflater.inflate(R.layout.fragment_device_manage_custom_queries, container, false);

        //Init recycler view with the corresponding layout and the adapter
        RecyclerView recyclerView = view.findViewById(R.id.custom_query_recyclerview);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CustomQueryAdapter(cockpitDbHelper, getContext());
        recyclerView.setAdapter(adapter);

        noQueriesText = view.findViewById(R.id.textview_noQueriesText);
        checkNoData();

        return view;
    }

    /**
     * method to check if view has no data
     *
     */
    public void checkNoData() {
        if (noQueriesText == null) {
            return;
        }
        if (cockpitDbHelper != null && cockpitDbHelper.getQueryRowCount() == 0) {
            Log.d(TAG, "visible");
            // is empty - show message
            noQueriesText.setText(R.string.no_own_queries_message);
            noQueriesText.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "invisible");
            noQueriesText.setVisibility(View.GONE);
        }
    }

    /**
     * method with to show create query dialog
     */
    public void showCustomQueryDialog(@Nullable CustomQuery customQuery, final boolean isEditMode) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.dialog_custom_query, null, false);

        initTagField(dialogView);
        initFormField(customQuery);

        String dialogTitle;
        if (isEditMode) {
            dialogTitle = getContext().getString(R.string.dialog_title_custom_query_edit);
        } else {
            dialogTitle = getContext().getString(R.string.dialog_title_custom_query_add);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitle)
                .setIcon(R.drawable.ic_receipt_black)
                .setCancelable(false)
                .setView(dialogView)
                // button is handled below
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> dialog.cancel())
                .setNegativeButton(R.string.close, (dialog, which) -> dialog.cancel())
                .setOnDismissListener(dialog -> {
                    currentTagSelection.clear();
                    adapter.notifyDataSetChanged();
                    checkNoData();
                });

        if (isEditMode) {
            // add additional button in edit mode: delete
            builder.setNeutralButton(R.string.delete_custom_query_label, (dialog, which) -> {
                cockpitDbHelper.removeQuery(customQuery.getId());
                dialog.cancel();
            });
        }

        alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v1 -> {
            boolean isSingleQuery = questionModeField.isChecked();
            Tag[] selectedTags = currentTagSelection.toArray(new Tag[]{});

            if (isInputValid(oidTextField, queryNameField, selectedTags)) {
                Log.d(TAG, "user input valid");
                String inputOID = oidTextField.getText().toString().trim();
                String inputName = queryNameField.getText().toString().trim();

                if (!isEditMode) {
                    // insert
                    long insertedQueryId = cockpitDbHelper.addNewQuery(inputOID, inputName, isSingleQuery);
                    cockpitDbHelper.linkTagsToQuery(insertedQueryId, selectedTags);
                } else {
                    // update
                    CustomQuery updatedCustomQuery = new CustomQuery(customQuery.getId(), inputOID, inputName, isSingleQuery);
                    updatedCustomQuery.setTagList(currentTagSelection);
                    cockpitDbHelper.updateQuery(updatedCustomQuery);
                }

                adapter.notifyDataSetChanged();
                layoutManager.scrollToPosition(0);
                checkNoData();
                //Resetting the input
                alertDialog.cancel();
            } else {
                Log.d(TAG, "input not valid!");
            }
        });
    }

    /**
     * internal method to init the form field of query dialog
     *
     * @param customQuery
     * @return
     */
    public void initFormField(@Nullable CustomQuery customQuery) {
        lastQuery = customQuery;
        oidTextField = dialogView.findViewById(R.id.dialog_custom_query_oid_edittext);
        queryNameField = dialogView.findViewById(R.id.dialog_custom_query_name_edittext);
        questionModeField = dialogView.findViewById(R.id.dialog_custom_query_checkbox_questionMode);
        tagListTextView = dialogView.findViewById(R.id.dialog_custom_query_current_tag_list);
        autoCompleteTextView = dialogView.findViewById(R.id.dialog_custom_query_tags_autocomplete);
        deleteTagsButton = dialogView.findViewById(R.id.dialog_custom_query_remove_tags);

        deleteTagsButton.setOnClickListener(v -> {
            currentTagSelection.clear();
            refreshTagSelection(autoCompleteTextView, currentTagSelection);
            deleteTagsButton.setVisibility(View.GONE);
        });

        if (customQuery != null) {
            // edit mode: populate fields
            oidTextField.setText(customQuery.getOid());
            queryNameField.setText(customQuery.getName());
            boolean isChecked = false;
            if (customQuery.isSingleQuery()) {
                isChecked = true;
            }
            questionModeField.setChecked(isChecked);
            currentTagSelection.addAll(customQuery.getTagList());
            refreshTagSelection(autoCompleteTextView, currentTagSelection);

            if (customQuery.getTagList().isEmpty()) {
                deleteTagsButton.setVisibility(View.GONE);
            }
        } else {
            deleteTagsButton.setVisibility(View.GONE);
        }
    }

    /**
     * oid and name should not be empty
     *
     * @param inputOIDField
     * @param inputNameField
     * @param selectedTags
     * @return
     */
    private boolean isInputValid(EditText inputOIDField, EditText inputNameField, Tag[] selectedTags) {
        Log.d(TAG, "validating user input");
        String inputOID = inputOIDField.getText().toString();
        if (inputOID.trim().isEmpty()) {
            showError(inputOIDField);
            return false;
        }
        String inputName = inputNameField.getText().toString();
        if (inputName.trim().isEmpty()) {
            showError(inputNameField);
            return false;
        }
        if (selectedTags == null) {
            throw new IllegalStateException("null tags selected, not valid!");
        }
        return true;
    }

    /**
     * helper method to init the tag fields
     *
     * @param dialogView
     */
    public void initTagField(View dialogView) {
        AutoCompleteTextView autoCompleteField = dialogView.findViewById(R.id.dialog_custom_query_tags_autocomplete);

        autoCompleteField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && getContext() != null) {
                dismissKeyboard(autoCompleteField, v);
            }
        });
        autoCompleteField.setOnClickListener(v -> {
            if (getContext() != null) {
                dismissKeyboard(autoCompleteField, v);
            }
        });
        autoCompleteField.setShowSoftInputOnFocus(false);

        final TextView tagListView = dialogView.findViewById(R.id.dialog_custom_query_current_tag_list);
        final ImageButton removeTagButton = dialogView.findViewById(R.id.dialog_custom_query_remove_tags);

        autoCompleteField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                for (int i = 0; i < HARDWARE_TAGS.size(); i++) {
                    Tag hardwareTag = HARDWARE_TAGS.get(i);
                    if (s.toString().equals(hardwareTag.getName())) {
                        // new tag added!
                        currentTagSelection.add(hardwareTag);
                        removeTagButton.setVisibility(View.VISIBLE);
                        String tagLabel = tagListToString(currentTagSelection);

                        tagListView.setText(tagLabel);
                        autoCompleteField.setText(null);

                        refreshTagSelection(autoCompleteField, currentTagSelection);
                    }
                }
            }
        });
        // update tag list
        HARDWARE_TAGS.clear();
        HARDWARE_TAGS.addAll(cockpitDbHelper.getAllTags());
        List<String> tagListLabels = getTagLabels(HARDWARE_TAGS);
        autoCompleteField.setAdapter(new ArrayAdapter<>(getContext(),
                R.layout.support_simple_spinner_dropdown_item,
                tagListLabels));
    }

    /**
     * list of strings with labels
     *
     * @param tagList
     * @return
     */
    @NonNull
    public List<String> getTagLabels(List<Tag> tagList) {
        List<String> tagListLabels = new ArrayList<>();
        for (Tag singleTag : tagList) {
            tagListLabels.add(singleTag.getName());
        }
        return tagListLabels;
    }

    /**
     * helper method to convert a list to a formatted string
     *
     * @param listOfTags
     * @return
     */
    private String tagListToString(List<Tag> listOfTags) {
        StringBuilder sb = new StringBuilder();
        String currentLine = "";
        for (Tag singleTag : listOfTags) {
            if (!listOfTags.isEmpty() && !currentLine.isEmpty()) {
                currentLine += ", ";
            }
            if ((currentLine.length() + singleTag.getName().length()) > 20) {
                // reset current line
                sb.append(currentLine).append("\n");
                currentLine = singleTag.getName();
            } else {
                currentLine += singleTag.getName();
            }
        }
        if (!currentLine.isEmpty()) {
            sb.append(currentLine);
        }
        return sb.toString();
    }

    /**
     * method to remove already selected tags
     *
     * @param autoComplete
     * @param selectedTagList
     */
    public void refreshTagSelection(AutoCompleteTextView autoComplete, List<Tag> selectedTagList) {
        List<String> tagListLabels = getTagLabels(HARDWARE_TAGS);
        tagListTextView.setText(tagListToString(selectedTagList));
        if (tagListLabels.isEmpty() && deleteTagsButton != null) {
            deleteTagsButton.setVisibility(View.GONE);
        } else {
            if (deleteTagsButton != null) {
                deleteTagsButton.setVisibility(View.VISIBLE);
            }
        }
        List<String> restItems = new ArrayList<>(tagListLabels);
        for (Tag selectedTag : selectedTagList) {
            restItems.remove(selectedTag.getName());
        }
        autoComplete.setAdapter(new ArrayAdapter<String>(getContext(),
                R.layout.support_simple_spinner_dropdown_item,
                restItems.toArray(new String[]{})));
    }

    /**
     * method to disable keyboard for tag input field
     *
     * @param autoCompleteTextView
     * @param v
     */
    public void dismissKeyboard(AutoCompleteTextView autoCompleteTextView, View v) {
        if (getContext() == null) {
            Log.w(TAG, "null context!");
            return;
        }
        InputMethodManager in = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (in != null) {
            in.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
        autoCompleteTextView.showDropDown();
    }

    /*
     * showError method, which gives the edittext field a warning hint, if the parameter is wrong.
     */
    private void showError(EditText text) {
        //Animation if pattern isn´t matching
        Animation shake = AnimationUtils.loadAnimation(getActivity(), R.anim.shake);
        text.startAnimation(shake);
        //Text output
        text.setError(getString(R.string.invalid_user_input));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.main_custom_query_options_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_query) {
            showCustomQueryDialog(null, false);
            return true;
        } else if (item.getItemId() == R.id.action_manage_tags) {
            Intent tagManagement = new Intent(getActivity(), TagManagementActivity.class);
            startActivity(tagManagement);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (oidTextField != null) {
            outState.putString(CURRENT_OID_FORM_KEY,
                    oidTextField.getText().toString());
        }
        if (queryNameField != null) {
            outState.putString(CURRENT_NAME_FORM_KEY,
                    queryNameField.getText().toString());
        }
        if (questionModeField != null) {
            outState.putString(QUESTION_MODE_FORM_KEY,
                    questionModeField.isChecked() ? "1" : "0");
        }
        if (tagListTextView != null) {
            outState.putString(TAG_LIST_TEXT_VIEW_FORM_KEY,
                    tagListTextView.getText().toString());
        }
        if (lastQuery != null) {
            outState.putLong(LAST_QUERY_ID_FORM_KEY, lastQuery.getId());
        }
        if (!currentTagSelection.isEmpty()) {
            outState.putStringArray(CURRENT_TAG_SELECTION_FORM_KEY,
                    getTagLabels(currentTagSelection).toArray(new String[]{}));
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.cancel();
            outState.putBoolean(IS_DIALOG_SHOWN_FORM_KEY, true);
        } else {
            outState.putBoolean(IS_DIALOG_SHOWN_FORM_KEY, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String[] stringArrayList = savedInstanceState.getStringArray(CURRENT_TAG_SELECTION_FORM_KEY);
            if (stringArrayList == null) {
                stringArrayList = new String[]{};
            }
            Log.d(TAG, "received previous selected tags: " + Arrays.asList(stringArrayList).toString());
            List<Tag> allTags = cockpitDbHelper.getAllTags();
            List<Tag> selectedTagList = new ArrayList<>();
            for (Tag singleTag : allTags) {
                for (String selectedTag : stringArrayList) {
                    if (singleTag.getName().equals(selectedTag)) {
                        selectedTagList.add(singleTag);
                    }
                }
            }

            Log.d(TAG, "restored tags: " + selectedTagList.toString());

            boolean wasDialogShown = savedInstanceState.getBoolean(IS_DIALOG_SHOWN_FORM_KEY);
            if (wasDialogShown) {
                long id = savedInstanceState.getLong(LAST_QUERY_ID_FORM_KEY, 0);
                String oid = savedInstanceState.getString(CURRENT_OID_FORM_KEY, null);
                String name = savedInstanceState.getString(CURRENT_NAME_FORM_KEY, null);
                boolean isSingleQuery = savedInstanceState.getString(QUESTION_MODE_FORM_KEY, "").equals("1");

                CustomQuery customQuery = new CustomQuery(id, oid, name, isSingleQuery);
                customQuery.setTagList(selectedTagList);

                showCustomQueryDialog(customQuery, id != 0);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cockpitDbHelper != null) {
            cockpitDbHelper.close();
        }
    }
}
