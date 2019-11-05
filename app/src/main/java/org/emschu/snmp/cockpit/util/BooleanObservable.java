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

package org.emschu.snmp.cockpit.util;

import java.util.Observable;

/**
 * this class is an implementation of a java {@link Observable}.
 * it basically keeps tracking state of a single boolean variable "state".
 * Call {@link #setValue(boolean)} to update the value and call
 * {@link #notifyObservers()} to trigger obervers
 */
public class BooleanObservable extends Observable {
    private boolean state = false;

    /**
     * constructor
     *
     * @param initialState
     */
    public BooleanObservable(boolean initialState) {
        this.state = initialState;
    }

    /**
     * set value + notify observers implicitely
     *
     * @param newValue
     */
    public void setValueAndTriggerObservers(boolean newValue) {
        setValue(newValue);
        this.notifyObservers();
    }

    public void setValue(boolean newValue) {
        if(state != newValue) {
            state = newValue;
            setChanged();
        }
    }

    public boolean getValue() {
        return state;
    }
}
