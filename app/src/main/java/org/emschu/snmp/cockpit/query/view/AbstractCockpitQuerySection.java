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

import android.text.Html;
import android.util.Log;

import androidx.annotation.Nullable;

import org.emschu.snmp.cockpit.R;
import org.emschu.snmp.cockpit.SnmpCockpitApp;
import org.emschu.snmp.cockpit.query.OIDCatalog;
import org.emschu.snmp.cockpit.query.OIDNotInCatalogException;
import org.emschu.snmp.cockpit.snmp.model.QueryResponse;

/**
 * this class represents a displayable section in the ui with a title and a type (enum {@link SectionType} )
 */
abstract class AbstractCockpitQuerySection {

    public static final String TAG = AbstractCockpitQuerySection.class.getName();
    private String title;
    private final SectionType type;
    private int unkownOIDCounter = -1;
    private boolean skipUnknown = true;

    public AbstractCockpitQuerySection(String title, SectionType type) {
        this.title = title;
        this.type = type;
    }

    public abstract String generateHtml();

    public abstract boolean isEmpty();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SectionType getType() {
        return type;
    }

    protected String generateTitle() {
        return "<h3>" + getTitle() + "</h3>";
    }

    public boolean isSkipUnknown() {
        return skipUnknown;
    }

    public void setSkipUnknown(boolean skipUnknown) {
        this.skipUnknown = skipUnknown;
    }

    /**
     * helper method to display a line of a component of a single query response
     *
     * @param sb
     * @param qr
     */
    protected void addQueryResponseRow(StringBuilder sb, QueryResponse qr) {
        String asnName;
        try {
            asnName = OIDCatalog.getInstance(null, null).getAsnByOidStripLast(qr.getOid());
        } catch (OIDNotInCatalogException e) {
            Log.d(TAG, "unknown oid '" + qr.getOid() + "' use 'unknown' as fallback and skipunknown: " + skipUnknown);
            if (skipUnknown) {
                return;
            }
            asnName = "unknown-" + unkownOIDCounter++;
        }
        sb.append("<li>");
        sb.append("<i>").append(Html.escapeHtml(asnName)).append("</i>").append(": ");
        sb.append("<span class='oid_value'>").append(Html.escapeHtml(qr.getValue())).append("</span>");
        sb.append("<span class='oid_info'>(").append(Html.escapeHtml(qr.getOid())).append(")</span>");

        sb.append("</li>");
    }

    protected void addNoDataText(StringBuilder sb) {
        String noInfoText = SnmpCockpitApp.getContext().getString(R.string.no_information_available);
        sb.append("<i>").append(noInfoText).append("</i>");
    }

    /**
     *
     * @param sb
     * @param title
     * @param content
     * @param cssClasses nullable
     */
    protected void addAccordionItem(StringBuilder sb, String title, String content,@Nullable String cssClasses) {
        if (cssClasses == null) {
            sb.append("<div class='accordion'>");
        } else {
            sb.append("<div class='accordion ").append(cssClasses).append("'>");
        }
        sb.append(title);
        sb.append("</div>");
        sb.append("<div class='content'><ul>");
        sb.append(content);
        sb.append("</ul></div>");
    }

    public enum SectionType {
        LIST, TABLE, GROUPED_LIST
    }
}
