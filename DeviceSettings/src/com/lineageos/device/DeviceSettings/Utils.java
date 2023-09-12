/*
* Copyright (C) 2013 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.lineageos.device.DeviceSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Utils {

    /**
     * Write a string value to the specified file.
     * @param filename      The filename
     * @param value         The value
     */
    public static void writeValue(String filename, String value) {
        if (filename == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(new File(filename));
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a string value to the specified sysfs file.
     * The format of written string has to be 2 strings with a space in between.
     * example:
     * "0 0"
     * @param filename      The filename
     * @param value         The value
     */
    public static void writeValueDual(String filename, String value) {
        if (filename == null) {
            return;
        }
        String Dualvalue = value + " " + value;

        try {
            FileOutputStream fos = new FileOutputStream(new File(filename));
            fos.write(Dualvalue.getBytes());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if the specified file exists.
     * @param filename      The filename
     * @return              Whether the file exists or not
     */
    public static boolean fileExists(String filename) {
        if (filename == null) {
            return false;
        }
        return new File(filename).exists();
    }

    public static boolean fileWritable(String filename) {
        return fileExists(filename) && new File(filename).canWrite();
    }

    public static String readLine(String filename) {
        if (filename == null) {
            return null;
        }
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(filename), 1024);
            line = br.readLine();
        } catch (IOException e) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return line;
    }

    public static boolean getFileValueAsBoolean(String filename, boolean defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return (fileValue.equals("0")?false:true);
        }
        return defValue;
    }

    public static String getFileValue(String filename, String defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return fileValue;
        }
        return defValue;
    }

    /**
     * @param filename      file to read
     * @param defValue      default value
     * @return              decluttered value or default value
     */
    public static String getFileValueDual(String filename, String defValue) {
        String fileValue = readLine(filename);
        if(fileValue!=null){
            return declutterDualValue(fileValue);
        }
        return defValue;
    }

    /**
     * we need this little helper method, because api offers us values for left and right.
     * We want to handle both values equal, so only read left value.
     * Format in sysfs file is:
     * 1 1
     * BUT... for some reasons, when writing in the file a -1, the value in the file is 255,
     * -2 is 254, so we have here to do some maths...
     * @param RawOutput      The RawOutput
     * @return              decluttered value
    */
    public static String declutterDualValue(String RawOutput) {
        String[] seperateDual = RawOutput.split(" ", 2);
        int declutteredValue = Integer.parseUnsignedInt(seperateDual[0]);
        if (declutteredValue > 20) {
            // The chosen variablename is like the thing it does ;-) ...
            int declutteredandConvertedValue = declutteredValue - 256;
            declutteredValue = declutteredandConvertedValue;
        }
        return String.valueOf(declutteredValue);
    }
}
