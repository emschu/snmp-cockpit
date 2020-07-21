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

package org.emschu.snmp.cockpit.query.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.BufferedInputStream;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.emschu.snmp.cockpit.CockpitPreferenceManager;
import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.query.AbstractQueryRequest;
import org.emschu.snmp.cockpit.query.ListQuery;
import org.emschu.snmp.cockpit.query.SnmpQuery;
import org.emschu.snmp.cockpit.query.TableQuery;
import org.emschu.snmp.cockpit.snmp.SnmpManager;
import org.emschu.snmp.cockpit.tasks.QueryTask;

/**
 * a custom view component wrapping an android webview + inserting custom generated html
 * of a list of {@link AbstractCockpitQuerySection}
 */
public class CockpitQueryView extends ConstraintLayout {

    public static final String TAG = CockpitQueryView.class.getName();
    private static ThreadPoolExecutor THREAD_POOL_EXECUTOR = SnmpManager.getInstance().getThreadPoolExecutor();
    private WebView webView;
    private ConcurrentHashMap<Integer, AbstractCockpitQuerySection> cockpitQuerySectionList = new ConcurrentHashMap<>();
    private String content = null;
    private boolean isFullyRendered = false;
    private OnRenderingFinishedListener onRenderingFinishedListener = null;

    /**
     * constructor
     *
     * @param context
     */
    public CockpitQueryView(Context context) {
        super(context);
        initView();
    }

    public CockpitQueryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public CockpitQueryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {
        Log.i(TAG, "init cockpit query view");
        View view = LayoutInflater.from(getContext()).inflate(R.layout.generic_query_view, this);
        webView = view.findViewById(R.id.snmp_interface_table_view);
        webView.setNetworkAvailable(false);
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings settings = webView.getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // otherwise the manifest declaration should be used by android
            settings.setSafeBrowsingEnabled(false);
        }
        settings.setJavaScriptEnabled(true);
        showContent();
    }

    /**
     * method to get basic html in res/raw/html_snmp_table_view.html
     *
     * @return
     */
    private String getBasicHtml() {
        String result = null;
        try (Scanner s = new Scanner(new BufferedInputStream(getResources().openRawResource(R.raw.html_snmp_table_view))).useDelimiter("\\A")) {
            result = s.hasNext() ? s.next() : "";
        } catch (Exception e) {
            Log.w(TAG, "error fetching basic html: " + e.getMessage());
        }
        return result;
    }

    public void addQuerySection(AbstractCockpitQuerySection querySection) {
        cockpitQuerySectionList.put(cockpitQuerySectionList.size(), querySection);
    }

    /**
     * get generated content. empty ones at the end
     * @return
     */
    private String getGeneratedSectionContent() {
        StringBuilder sbContent = new StringBuilder();
        StringBuilder sbEmpty = new StringBuilder();
        Log.d(TAG, "displaying " + cockpitQuerySectionList.values().size() + " sections");

        for (AbstractCockpitQuerySection querySection : cockpitQuerySectionList.values()) {
            if (querySection.isEmpty()) {
                sbEmpty.append(querySection.generateHtml());
            } else {
                sbContent.append(querySection.generateHtml());
            }
        }
        sbContent.append(sbEmpty.toString());
        if (isFullyRendered()) {
            // close spinner with internal js
            sbContent.append("<script type='text/javascript'>").append("hideSpinner();").append("</script>");
        }
        return sbContent.toString();
    }

    public void render() {
        Log.d(TAG, "rendering cockpit query view");
        showContent();
    }

    public void render(boolean isFinished) {
        Log.d(TAG, "final rendering cockpit query view");
        setFullyRendered(true);
        showContent();
    }

    private void showContent() {
        webView.loadDataWithBaseURL("file:///android_asset/",
                getContent(), "text/html", "UTF-8", null);
    }

    /**
     * get html content
     *
     * @return
     */
    private String getContent() {
        String basicHtml = getBasicHtml();
        if (basicHtml != null) {
            return basicHtml.replace("CONTENT_HERE_PLACEHOLDER", getGeneratedSectionContent());
        }
        Log.e(TAG, "using fallback without basic html");
        return getGeneratedSectionContent();
    }

    public void clear() {
        cockpitQuerySectionList.clear();
    }

    /**
     * method for convenience
     *
     * @param title
     * @param queryRequest
     */
    public void addListQuery(String title, AbstractQueryRequest queryRequest) {
        addListQuery(title, queryRequest, false);
    }

    /**
     * helper method to add list queries to this component
     *
     * @param title
     * @param queryRequest
     */
    public void addListQuery(String title, AbstractQueryRequest queryRequest, boolean listUnknown) {
        QueryTask<? extends SnmpQuery> snmpInfoTask = new QueryTask<>();

        if (THREAD_POOL_EXECUTOR.isTerminating() ||
                THREAD_POOL_EXECUTOR.isShutdown()) {
            THREAD_POOL_EXECUTOR = SnmpManager.getInstance().getThreadPoolExecutor();
        }

        snmpInfoTask.executeOnExecutor(THREAD_POOL_EXECUTOR,
                queryRequest);
        ListQuery listQuery = (ListQuery) getAnswer(snmpInfoTask);
        if (listQuery != null) {
            ListQuerySection querySection = new ListQuerySection(title, listQuery);
            if (listUnknown) {
                querySection.setSkipUnknown(false);
            } else {
                querySection.setSkipUnknown(true);
            }
            addQuerySection(querySection);
            Log.d(TAG, "list query section added");
        }
        Log.d(TAG, "list query was null. nothing added.");
    }

    /**
     * add dynamic table query
     *
     * @param title
     * @param queryRequest
     * @param listUnknown
     */
    public void addTableQuery(String title, AbstractQueryRequest queryRequest, boolean listUnknown) {
        QueryTask<? extends SnmpQuery> snmpInfoTask = new QueryTask<>();

        if (THREAD_POOL_EXECUTOR.isTerminating() ||
                THREAD_POOL_EXECUTOR.isShutdown()) {
            THREAD_POOL_EXECUTOR = SnmpManager.getInstance().getThreadPoolExecutor();
        }

        snmpInfoTask.executeOnExecutor(THREAD_POOL_EXECUTOR, queryRequest);
        TableQuery listQuery = (TableQuery) getAnswer(snmpInfoTask);
        if (listQuery != null) {
            GroupedListQuerySection querySection = new GroupedListQuerySection(title, listQuery);
            if (listUnknown) {
                querySection.setSkipUnknown(false);
            } else {
                querySection.setSkipUnknown(true);
            }
            addQuerySection(querySection);
            Log.d(TAG, "table query section added");
        }
        Log.d(TAG, "table query was null. nothing added.");
    }

    public boolean isFullyRendered() {
        return isFullyRendered;
    }

    public OnRenderingFinishedListener getOnRenderingFinishedListener() {
        return onRenderingFinishedListener;
    }

    public void setOnRenderingFinishedListener(OnRenderingFinishedListener onRenderingFinishedListener) {
        this.onRenderingFinishedListener = onRenderingFinishedListener;
    }

    /**
     * simple helper method
     *
     * @param qt
     * @return
     */
    private SnmpQuery getAnswer(QueryTask<?> qt) {
        try {
            if (qt.getDeviceConfiguration() == null) {
                throw new IllegalStateException("null device config");
            }
            int offset = qt.getDeviceConfiguration().getAdditionalTimeoutOffset();
            SnmpQuery snmpQuery = qt.get((long) CockpitPreferenceManager.TIMEOUT_WAIT_ASYNC_MILLISECONDS + offset, TimeUnit.MILLISECONDS);
            if (snmpQuery != null) {
                SnmpManager.getInstance().resetTimeout(qt.getDeviceConfiguration());
                return snmpQuery;
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "detail task interrupted!");
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Log.e(TAG, "detail task failed: " + e.getMessage());
        } catch (TimeoutException e) {
            Log.w(TAG, "timeout reached in CockpitQueryView");
            if (qt.getDeviceConfiguration() != null) {
                SnmpManager.getInstance().registerTimeout(qt.getDeviceConfiguration());
            }
        }
        return null;
    }

    /**
     * internal method to finish and call a listener - if defined
     *
     * @param fullyRendered
     */
    private void setFullyRendered(boolean fullyRendered) {
        if (fullyRendered && onRenderingFinishedListener != null) {
            onRenderingFinishedListener.finished();
        }
        this.isFullyRendered = fullyRendered;
    }

    /**
     * simple interface for a finishing listener
     * TODO replace with functional style
     */
    public interface OnRenderingFinishedListener {
        public void finished();
    }
}
