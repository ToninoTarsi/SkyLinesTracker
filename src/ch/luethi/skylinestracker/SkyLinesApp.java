/*
 * SkyLines Tracker is a location tracking client for the SkyLines platform <www.skylines-project.org>.
 * Copyright (C) 2013  Andreas Lüthi
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.luethi.skylinestracker;

import android.app.Application;
import android.util.Log;

import java.util.Stack;

public class SkyLinesApp extends Application {

    public boolean guiActive;
    public double lastLat, lastLon;
    public PositionService positionService = null;
    public static FixQueue<byte[]> fixStack;

    public SkyLinesApp() {
        Log.d("SkyLines", "SkyLinesApp()");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fixStack = new FixQueue<byte[]>(getApplicationContext()).load();
        Log.d("SkyLines", "SkyLinesApp, onCreate()");
    }

}